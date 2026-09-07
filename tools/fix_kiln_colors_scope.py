from pathlib import Path
import re

UI = Path(r"C:\Users\praim\AndroidStudioProjects\MySmartKiln\app\src\main\java\com\example\mysmartkiln\ui")
IMPORT = "import com.example.mysmartkiln.ui.theme.LocalKilnColors\n"


def ensure_import(text: str) -> str:
    if "import com.example.mysmartkiln.ui.theme.LocalKilnColors" in text:
        return text
    if "kilnColors." not in text and "LocalKilnColors" not in text:
        return text
    if "import com.example.mysmartkiln.ui.theme.AppColors" in text:
        return text.replace(
            "import com.example.mysmartkiln.ui.theme.AppColors\n",
            "import com.example.mysmartkiln.ui.theme.AppColors\n" + IMPORT,
            1,
        )
    return re.sub(r"(package [^\n]+\n\n?)", r"\1" + IMPORT, text, count=1)


def inject_all(text: str) -> str:
    """For every @Composable fun that uses kilnColors., ensure local val exists."""
    if "kilnColors." not in text:
        return text

    parts = re.split(r"(?=@Composable\b)", text)
    out = []
    for part in parts:
        if not part.startswith("@Composable"):
            out.append(part)
            continue
        if "kilnColors." not in part:
            out.append(part)
            continue
        # Already has the val near the start of this composable chunk?
        # Find function body start
        m = re.search(r"\bfun\s+(\w+)\s*\(", part)
        if not m:
            out.append(part)
            continue
        # Find matching opening brace of function - first { after params
        # Handle default params with lambdas carefully: find ") {" or ") : X {" pattern
        brace_m = re.search(
            r"\bfun\s+\w+\s*\((?:[^()]|\([^()]*\))*\)\s*(?::\s*[^{]+)?\{",
            part,
            re.DOTALL,
        )
        if not brace_m:
            # fallback: first {
            idx = part.find("{", m.end())
            if idx < 0:
                out.append(part)
                continue
            insert_at = idx + 1
        else:
            insert_at = brace_m.end()

        head = part[:insert_at]
        tail = part[insert_at:]
        # If val already right after opening, skip
        if re.match(r"\s*val kilnColors = LocalKilnColors\.current", tail):
            out.append(part)
            continue
        # If this composable's body (until next top-level-ish) already has val later, 
        # still inject at top for scope - but avoid duplicate
        if "val kilnColors = LocalKilnColors.current" in part:
            # Might be in nested - check if before any kilnColors use in this function
            # Simplest: if val exists anywhere in part, don't re-inject at top IF it's before uses
            # Actually nested @Composable will be separate parts due to split. So if val in part, OK.
            out.append(part)
            continue

        part = head + "\n    val kilnColors = LocalKilnColors.current\n" + tail
        out.append(part)
    return "".join(out)


changed = 0
for p in sorted(UI.rglob("*.kt")):
    if "theme" in p.parts:
        continue
    orig = p.read_text(encoding="utf-8")
    text = ensure_import(orig)
    text = inject_all(text)
    # Dedup imports
    lines = text.splitlines(keepends=True)
    seen = set()
    new_lines = []
    for line in lines:
        if line.startswith("import "):
            if line in seen:
                continue
            seen.add(line)
        new_lines.append(line)
    text = "".join(new_lines)
    if text != orig:
        p.write_text(text, encoding="utf-8")
        changed += 1
        print("fixed", p.relative_to(UI))

print("changed", changed)

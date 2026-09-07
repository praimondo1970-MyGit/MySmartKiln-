#!/usr/bin/env python3
"""Migrate UI files from hardcoded AppColors to LocalKilnColors (Phase A)."""
from __future__ import annotations

import re
from pathlib import Path

UI = Path(r"C:\Users\praim\AndroidStudioProjects\MySmartKiln\app\src\main\java\com\example\mysmartkiln\ui")
SKIP = {"theme"}

# Order matters.
REPLACEMENTS: list[tuple[str, str]] = [
    (r"Color\(0xFF1E3A8A\)", "kilnColors.title"),
    (r"Color\(0xFF003D99\)", "kilnColors.navBar"),
    (r"\.background\(\s*color\s*=\s*AppColors\.Primary100\s*\)", ".background(color = kilnColors.screenBackground)"),
    (r"\.background\(\s*AppColors\.Primary100\s*\)", ".background(kilnColors.screenBackground)"),
    (r"backgroundColor\s*=\s*AppColors\.Primary100", "backgroundColor = kilnColors.screenBackground"),
    (r"containerColor\s*=\s*AppColors\.Primary100", "containerColor = kilnColors.screenBackground"),
    (r"\.background\(\s*color\s*=\s*Color\.White\s*\)", ".background(color = kilnColors.cardBackground)"),
    (r"\.background\(\s*Color\.White\s*\)", ".background(kilnColors.cardBackground)"),
    (r"containerColor\s*=\s*Color\.White", "containerColor = kilnColors.cardBackground"),
    (r"containerColor\s*=\s*AppColors\.White", "containerColor = kilnColors.cardBackground"),
    (r"colors\s*=\s*CardDefaults\.cardColors\(\s*containerColor\s*=\s*Color\.White\s*\)",
     "colors = CardDefaults.cardColors(containerColor = kilnColors.cardBackground)"),
    (r"border\(([^,\n]+),\s*AppColors\.Primary100\s*,", r"border(\1, kilnColors.cardBorder,"),
    (r"\.border\(\s*1\.dp\s*,\s*AppColors\.Primary100\s*,", ".border(1.dp, kilnColors.cardBorder,"),
    (r"containerColor\s*=\s*AppColors\.Primary700", "containerColor = kilnColors.accent"),
    (r"containerColor\s*=\s*AppColors\.Tertiary900", "containerColor = kilnColors.fab"),
    (r"\.background\(\s*AppColors\.Primary700\s*\)", ".background(kilnColors.accent)"),
    (r"\.background\(\s*color\s*=\s*AppColors\.Primary700\s*\)", ".background(color = kilnColors.accent)"),
    (r"tint\s*=\s*AppColors\.Primary700", "tint = kilnColors.body"),
    (r"tint\s*=\s*AppColors\.Primary600", "tint = kilnColors.body"),
    (r"tint\s*=\s*AppColors\.Primary800", "tint = kilnColors.title"),
    (r"color\s*=\s*AppColors\.Primary700", "color = kilnColors.body"),
    (r"color\s*=\s*AppColors\.Primary800", "color = kilnColors.title"),
    (r"color\s*=\s*AppColors\.Primary600", "color = kilnColors.body"),
    (r"color\s*=\s*AppColors\.Primary400", "color = kilnColors.muted"),
    (r"color\s*=\s*AppColors\.Primary300", "color = kilnColors.muted"),
    (r"color\s*=\s*AppColors\.Primary500", "color = kilnColors.accent"),
    (r"\.background\(\s*AppColors\.Primary200\s*\)", ".background(kilnColors.cardBorder)"),
    (r"color\s*=\s*AppColors\.Primary200", "color = kilnColors.cardBorder"),
    # Remaining white (icons/text on accent) after bg replacements
    (r"tint\s*=\s*Color\.White", "tint = kilnColors.onAccent"),
    (r"color\s*=\s*Color\.White", "color = kilnColors.onAccent"),
    (r"color\s*=\s*AppColors\.White", "color = kilnColors.onAccent"),
]


def ensure_import(text: str) -> str:
    if "LocalKilnColors" in text and "import com.example.mysmartkiln.ui.theme.LocalKilnColors" not in text:
        # Insert after package or after last theme import
        if "import com.example.mysmartkiln.ui.theme.AppColors" in text:
            text = text.replace(
                "import com.example.mysmartkiln.ui.theme.AppColors",
                "import com.example.mysmartkiln.ui.theme.AppColors\n"
                "import com.example.mysmartkiln.ui.theme.LocalKilnColors",
                1,
            )
        elif "import com.example.mysmartkiln.ui.theme." in text:
            text = re.sub(
                r"(import com\.example\.mysmartkiln\.ui\.theme\.[^\n]+\n)",
                r"\1import com.example.mysmartkiln.ui.theme.LocalKilnColors\n",
                text,
                count=1,
            )
        else:
            text = re.sub(
                r"(package [^\n]+\n)",
                r"\1\nimport com.example.mysmartkiln.ui.theme.LocalKilnColors\n",
                text,
                count=1,
            )
    return text


def inject_kiln_colors(text: str) -> str:
    """Inject val kilnColors in each @Composable fun that references kilnColors."""
    if "kilnColors." not in text:
        return text

    # Split keeping delimiters on @Composable
    parts = re.split(r"(?=@Composable)", text)
    out = []
    for part in parts:
        if not part.startswith("@Composable"):
            out.append(part)
            continue
        if "kilnColors." not in part or "LocalKilnColors.current" in part:
            out.append(part)
            continue
        # Find the opening brace of the function (first { after fun ...)
        m = re.search(r"\bfun\s+\w+[^{]*\{", part, re.DOTALL)
        if not m:
            out.append(part)
            continue
        insert_at = m.end()
        injection = "\n    val kilnColors = LocalKilnColors.current\n"
        # Avoid double inject
        rest = part[insert_at:insert_at + 80]
        if "val kilnColors = LocalKilnColors.current" in rest:
            out.append(part)
            continue
        part = part[:insert_at] + injection + part[insert_at:]
        out.append(part)
    return "".join(out)


def process_file(path: Path) -> bool:
    if "theme" in path.parts:
        return False
    original = path.read_text(encoding="utf-8")
    text = original
    for pat, repl in REPLACEMENTS:
        text = re.sub(pat, repl, text)
    if text == original:
        return False
    text = ensure_import(text)
    text = inject_kiln_colors(text)
    # Deduplicate imports
    lines = text.splitlines(keepends=True)
    seen = set()
    new_lines = []
    for line in lines:
        if line.startswith("import ") and line in seen:
            continue
        if line.startswith("import "):
            seen.add(line)
        new_lines.append(line)
    text = "".join(new_lines)
    path.write_text(text, encoding="utf-8")
    return True


def main() -> None:
    changed = []
    for path in sorted(UI.rglob("*.kt")):
        if any(s in path.parts for s in SKIP):
            continue
        if process_file(path):
            changed.append(str(path.relative_to(UI)))
    print(f"Updated {len(changed)} files:")
    for c in changed:
        print(" -", c)


if __name__ == "__main__":
    main()

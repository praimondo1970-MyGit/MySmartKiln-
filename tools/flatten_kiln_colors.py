from pathlib import Path
import re

UI = Path(r"C:\Users\praim\AndroidStudioProjects\MySmartKiln\app\src\main\java\com\example\mysmartkiln\ui")

for p in UI.rglob("*.kt"):
    if "theme" in p.parts:
        continue
    text = p.read_text(encoding="utf-8")
    orig = text
    # Remove injected locals (including wrongly placed ones)
    text = re.sub(r"^\s*val kilnColors = LocalKilnColors\.current\s*\n", "", text, flags=re.MULTILINE)
    # Also remove if stuck mid-signature somehow
    text = text.replace("val kilnColors = LocalKilnColors.current\n", "")
    text = text.replace("val kilnColors = LocalKilnColors.current", "")
    # Direct CompositionLocal reads
    text = text.replace("kilnColors.", "LocalKilnColors.current.")
    # Fix accidental double
    text = text.replace("LocalKilnColors.current.LocalKilnColors.current.", "LocalKilnColors.current.")
    # Ensure import
    if "LocalKilnColors.current." in text and "import com.example.mysmartkiln.ui.theme.LocalKilnColors" not in text:
        if "import com.example.mysmartkiln.ui.theme.AppColors" in text:
            text = text.replace(
                "import com.example.mysmartkiln.ui.theme.AppColors\n",
                "import com.example.mysmartkiln.ui.theme.AppColors\n"
                "import com.example.mysmartkiln.ui.theme.LocalKilnColors\n",
                1,
            )
        else:
            text = re.sub(
                r"(package [^\n]+\n\n?)",
                r"\1import com.example.mysmartkiln.ui.theme.LocalKilnColors\n",
                text,
                count=1,
            )
    # Dedup imports
    lines = text.splitlines(keepends=True)
    seen = set()
    out = []
    for line in lines:
        if line.startswith("import "):
            if line in seen:
                continue
            seen.add(line)
        out.append(line)
    text = "".join(out)
    if text != orig:
        p.write_text(text, encoding="utf-8")
        print("rewrote", p.relative_to(UI))

print("done")

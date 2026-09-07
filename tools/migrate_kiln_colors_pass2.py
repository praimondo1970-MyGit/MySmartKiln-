from pathlib import Path
import re

UI = Path(r"C:\Users\praim\AndroidStudioProjects\MySmartKiln\app\src\main\java\com\example\mysmartkiln\ui")

for p in UI.rglob("*.kt"):
    if "theme" in p.parts:
        continue
    t = p.read_text(encoding="utf-8")
    orig = t
    t = t.replace("AppColors.Primary100", "kilnColors.screenBackground")
    if t != orig:
        if "import com.example.mysmartkiln.ui.theme.LocalKilnColors" not in t:
            if "import com.example.mysmartkiln.ui.theme.AppColors" in t:
                t = t.replace(
                    "import com.example.mysmartkiln.ui.theme.AppColors",
                    "import com.example.mysmartkiln.ui.theme.AppColors\n"
                    "import com.example.mysmartkiln.ui.theme.LocalKilnColors",
                    1,
                )
            else:
                t = re.sub(
                    r"(package [^\n]+\n)",
                    r"\1\nimport com.example.mysmartkiln.ui.theme.LocalKilnColors\n",
                    t,
                    count=1,
                )
        if "kilnColors." in t and "val kilnColors = LocalKilnColors.current" not in t:
            parts = re.split(r"(?=@Composable)", t)
            out = []
            for part in parts:
                if (
                    part.startswith("@Composable")
                    and "kilnColors." in part
                    and "LocalKilnColors.current" not in part
                ):
                    m = re.search(r"\bfun\s+\w+[^{]*\{", part, re.DOTALL)
                    if m:
                        part = (
                            part[: m.end()]
                            + "\n    val kilnColors = LocalKilnColors.current\n"
                            + part[m.end() :]
                        )
                out.append(part)
            t = "".join(out)
        p.write_text(t, encoding="utf-8")
        print("patched", p.relative_to(UI))

# Report leftovers
print("\nLeftover Primary100 / hardcoded blues:")
for p in UI.rglob("*.kt"):
    if "theme" in p.parts:
        continue
    t = p.read_text(encoding="utf-8")
    hits = []
    for pat in [
        "AppColors.Primary100",
        "Color(0xFF1E3A8A)",
        "Color(0xFF003D99)",
        "AppColors.Primary700",
        ".background(Color.White)",
    ]:
        if pat in t:
            hits.append(pat)
    if hits:
        print(p.relative_to(UI), "->", hits)

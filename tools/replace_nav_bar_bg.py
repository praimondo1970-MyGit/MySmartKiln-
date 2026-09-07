"""Replace duplicated curved nav backgrounds with BottomNavBarBackground."""
from pathlib import Path
import re

ROOT = Path("app/src/main/java/com/example/mysmartkiln/ui/screens")
IMPORT = "import com.example.mysmartkiln.ui.components.BottomNavBarBackground"

# Match the full Box that paints the curved nav fill
PATTERN = re.compile(
    r"[ \t]*// Navigation Bar Background[^\n]*\n"
    r"[ \t]*Box\(\s*"
    r"modifier\s*=\s*Modifier\s*"
    r"\.align\([^\)]+\)\s*"
    r"\.offset\(x\s*=\s*0\.dp,\s*y\s*=\s*60\.dp\)\s*"
    r"\.fillMaxWidth\(\)\s*"
    r"\.requiredHeight\(height\s*=\s*141\.dp\)\s*"
    r"\.background\(\s*"
    r"color\s*=\s*(?:LocalKilnColors\.current\.navBar|kilnColors\.navBar)\s*,\s*"
    r"shape\s*=\s*object\s*:\s*androidx\.compose\.ui\.graphics\.Shape\s*\{"
    r".*?"
    r"return\s+androidx\.compose\.ui\.graphics\.Outline\.Generic\(path\)\s*"
    r"\}\s*"
    r"\}\s*"
    r"\)\s*"
    r"\)",
    re.DOTALL,
)

REPLACEMENT = """            // Navigation Bar Background
            BottomNavBarBackground(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
            )"""

def ensure_import(text: str) -> str:
    if IMPORT in text:
        return text
    # after ResponsiveBottomNavigation import if present
    needle = "import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation"
    if needle in text:
        return text.replace(needle, needle + "\n" + IMPORT, 1)
    # after any components import
    m = re.search(r"import com\.example\.mysmartkiln\.ui\.components\.\w+\n", text)
    if m:
        i = m.end()
        return text[:i] + IMPORT + "\n" + text[i:]
    return text


def main():
    for p in sorted(ROOT.glob("*.kt")):
        t = p.read_text(encoding="utf-8")
        if "requiredHeight(height = 141.dp)" not in t:
            continue
        if "navBar" not in t and "Navigation Bar Background" not in t:
            continue
        new, n = PATTERN.subn(REPLACEMENT, t)
        if n == 0:
            print(f"MISS {p.name}")
            # debug: show snippet around 141
            idx = t.find("141.dp")
            print(repr(t[idx - 120 : idx + 80]))
            continue
        new = ensure_import(new)
        p.write_text(new, encoding="utf-8")
        print(f"OK {p.name} x{n}")


if __name__ == "__main__":
    main()

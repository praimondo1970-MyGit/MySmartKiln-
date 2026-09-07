from pathlib import Path
import re

root = Path("app/src/main/java/com/example/mysmartkiln/ui")
files = sorted(root.rglob("*.kt"))
print(f"Total UI kt files: {len(files)}\n")

patterns = {
    "LocalKilnColors": r"LocalKilnColors",
    "hardcoded_Color": r"Color\(0x[0-9A-Fa-f]+\)",
    "Primary_const": r"\bPrimary[0-9]{2,3}\b",
    "Tertiary_const": r"\bTertiary[0-9]{2,3}\b",
    "Secondary_const": r"\bSecondary[0-9]{2,3}\b",
    "White_Black": r"\bColor\.(White|Black|Gray|LightGray|DarkGray)\b",
    "colorScheme": r"MaterialTheme\.colorScheme\.(primary|secondary|tertiary|background|surface|onPrimary|onBackground)",
}

rows = []
for p in files:
    t = p.read_text(encoding="utf-8")
    counts = {k: len(re.findall(v, t)) for k, v in patterns.items()}
    rel = str(p.relative_to(root)).replace("\\", "/")
    rows.append((rel, counts, len(t.splitlines())))

print("=== SIN LocalKilnColors ===")
for r, c, n in rows:
    if c["LocalKilnColors"] != 0:
        continue
    flags = {k: c[k] for k in c if k != "LocalKilnColors" and c[k] > 0}
    print(f"  {r} ({n} lines) {flags}")

print("\n=== CON LocalKilnColors PERO aún hardcodes ===")
for r, c, n in rows:
    if c["LocalKilnColors"] == 0:
        continue
    leftovers = {
        k: c[k]
        for k in [
            "hardcoded_Color",
            "Primary_const",
            "Tertiary_const",
            "Secondary_const",
            "White_Black",
        ]
        if c[k] > 0
    }
    if leftovers:
        print(f"  {r}: kiln={c['LocalKilnColors']} leftovers={leftovers}")

print("\n=== Resumen por carpeta ===")
from collections import defaultdict

by = defaultdict(lambda: {"files": 0, "with": 0, "without": 0})
for r, c, n in rows:
    folder = r.split("/")[0] if "/" in r else "."
    by[folder]["files"] += 1
    if c["LocalKilnColors"]:
        by[folder]["with"] += 1
    else:
        by[folder]["without"] += 1
for folder, s in sorted(by.items()):
    print(f"  {folder}: {s['with']}/{s['files']} con LocalKilnColors ({s['without']} sin)")

"""Theme Programs, Activity, Settings, SearchBar without broken default-lambda inject."""
from pathlib import Path

ROOT = Path("app/src/main/java/com/example/mysmartkiln/ui")
IMP = "import com.example.mysmartkiln.ui.theme.LocalKilnColors"


def ensure_import(t: str) -> str:
    if IMP in t:
        return t
    if "import com.example.mysmartkiln.ui.theme.AppColors" in t:
        return t.replace(
            "import com.example.mysmartkiln.ui.theme.AppColors",
            "import com.example.mysmartkiln.ui.theme.AppColors\n" + IMP,
            1,
        )
    # after package
    lines = t.splitlines(True)
    out = []
    done = False
    for line in lines:
        out.append(line)
        if not done and line.startswith("package "):
            out.append("\n" + IMP + "\n")
            done = True
    return "".join(out)


def apply_reps(t: str, reps: list[tuple[str, str]]) -> str:
    for a, b in reps:
        t = t.replace(a, b)
    return t


COMMON = [
    ("Color(0xFF1E3A8A)", "LocalKilnColors.current.title"),
    ("AppColors.Primary800", "LocalKilnColors.current.title"),
    ("AppColors.Primary700", "LocalKilnColors.current.accent"),
    ("AppColors.Primary600", "LocalKilnColors.current.body"),
    ("AppColors.Primary400", "LocalKilnColors.current.muted"),
    ("AppColors.Primary100", "LocalKilnColors.current.cardBorder"),
    ("backgroundColor = AppColors.Primary100", "backgroundColor = LocalKilnColors.current.screenBackground"),
    ("backgroundColor = LocalKilnColors.current.cardBorder", "backgroundColor = LocalKilnColors.current.screenBackground"),  # if Primary100 became cardBorder wrongly in gradient
]


def migrate_search_bar():
    p = ROOT / "components/SearchBar.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = apply_reps(
        t,
        [
            (".background(\n                color = Color.White,\n                shape = RoundedCornerShape(12.dp)\n            )",
             ".background(\n                color = LocalKilnColors.current.cardBackground,\n                shape = RoundedCornerShape(12.dp)\n            )"),
            ("color = AppColors.Primary100,", "color = LocalKilnColors.current.cardBorder,"),
            ("color = Color(0xFF1E3A8A),", "color = LocalKilnColors.current.title,"),
            ("cursorBrush = SolidColor(Color(0xFF1E3A8A))", "cursorBrush = SolidColor(LocalKilnColors.current.accent)"),
            ("color = Color(0xFF6B7280), // Gray", "color = LocalKilnColors.current.muted,"),
            ("tint = Color(0xFF6B7280) // Gray", "tint = LocalKilnColors.current.muted"),
        ],
    )
    p.write_text(t, encoding="utf-8")
    print("OK SearchBar")


def migrate_programs():
    p = ROOT / "screens/ProgramsScreen.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = apply_reps(t, COMMON)
    t = apply_reps(
        t,
        [
            ("backgroundColor = LocalKilnColors.current.cardBorder,", "backgroundColor = LocalKilnColors.current.screenBackground,"),
            ("containerColor = LocalKilnColors.current.accent,", "containerColor = LocalKilnColors.current.accent,"),  # noop keep
            ("contentColor = Color.White", "contentColor = LocalKilnColors.current.onAccent"),
            # empty state titles that were 1E3A8A already replaced
        ],
    )
    # Fix ScrollGradient if still AppColors
    t = t.replace(
        "backgroundColor = AppColors.Primary100,",
        "backgroundColor = LocalKilnColors.current.screenBackground,",
    )
    p.write_text(t, encoding="utf-8")
    print("OK ProgramsScreen leftovers:", t.count("AppColors."), "hex titles", t.count("0xFF1E3A8A"))


def migrate_activity():
    p = ROOT / "screens/ActivityScreen.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = apply_reps(t, COMMON)
    t = apply_reps(
        t,
        [
            ("backgroundColor = LocalKilnColors.current.cardBorder,", "backgroundColor = LocalKilnColors.current.screenBackground,"),
            ("color = Color.White,", "color = LocalKilnColors.current.onAccent,"),
            ("color = AppColors.White,", "color = LocalKilnColors.current.onAccent,"),
            (".background(color = AppColors.Primary400)", ".background(color = LocalKilnColors.current.fab)"),
            (".background(color = LocalKilnColors.current.muted)", ".background(color = LocalKilnColors.current.fab)"),  # if Primary400->muted wrongly for swipe
        ],
    )
    # Primary400 for swipe was mapped to muted - restore fab for swipe delete bg if comment says ProgramSwipeActions
    t = t.replace(
        ".background(color = LocalKilnColors.current.muted) // Mismo color que ProgramSwipeActions",
        ".background(color = LocalKilnColors.current.fab) // Mismo color que ProgramSwipeActions",
    )
    p.write_text(t, encoding="utf-8")
    print("OK ActivityScreen leftovers AppColors", t.count("AppColors."), "White", t.count("Color.White"))


def migrate_settings():
    p = ROOT / "screens/SettingsScreen.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = apply_reps(t, COMMON)
    t = apply_reps(
        t,
        [
            (".background(Color.White)", ".background(LocalKilnColors.current.cardBackground)"),
            (".border(1.dp, AppColors.Primary100, RoundedCornerShape(12.dp))",
             ".border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))"),
            (".border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))",
             ".border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))"),
            (".background(LocalKilnColors.current.cardBorder)",
             ".background(LocalKilnColors.current.accent.copy(alpha = 0.15f))"),  # chip after Primary100->cardBorder may be wrong
            (".background(AppColors.Primary100)",
             ".background(LocalKilnColors.current.accent.copy(alpha = 0.15f))"),
        ],
    )
    # Fix chips that became cardBorder background from Primary100
    # Look for small icon chip pattern - already handled if AppColors.Primary100 left
    # Settings may use kilnColors already for some - leave
    # Fix: .background(LocalKilnColors.current.cardBorder) that was for icon chips (Primary100)
    # Only replace when it's chip-like - risky. Count occurrences.
    p.write_text(t, encoding="utf-8")
    print("OK SettingsScreen AppColors", t.count("AppColors."), "Color.White", t.count("Color.White"))


if __name__ == "__main__":
    migrate_search_bar()
    migrate_programs()
    migrate_activity()
    migrate_settings()

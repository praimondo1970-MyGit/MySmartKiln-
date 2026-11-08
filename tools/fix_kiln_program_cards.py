"""Fix KilnCard, ProgramCard, bottom nav, KilnsScreen for ESP theme Phase A."""
from pathlib import Path

ROOT = Path("app/src/main/java/com/example/mysmartkiln/ui")
IMPORT = "import com.example.mysmartkiln.ui.theme.LocalKilnColors"


def ensure_import(t: str) -> str:
    if IMPORT in t:
        return t
    if "import com.example.mysmartkiln.ui.theme.AppColors" in t:
        return t.replace(
            "import com.example.mysmartkiln.ui.theme.AppColors",
            "import com.example.mysmartkiln.ui.theme.AppColors\n" + IMPORT,
            1,
        )
    lines = t.splitlines(True)
    out = []
    done = False
    for line in lines:
        out.append(line)
        if not done and line.startswith("package "):
            out.append("\n" + IMPORT + "\n")
            done = True
    return "".join(out)


def inject_kiln_colors(t: str, fun_name: str) -> str:
    marker = f"fun {fun_name}("
    idx = t.find(marker)
    if idx < 0:
        return t
    # find opening brace of function body (after params)
    brace = t.find("{", idx)
    window = t[idx : brace + 80]
    if "val kilnColors = LocalKilnColors.current" in window:
        return t
    return t[: brace + 1] + "\n    val kilnColors = LocalKilnColors.current\n" + t[brace + 1 :]


def migrate_kiln_card():
    p = ROOT / "components/KilnCard.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = inject_kiln_colors(t, "KilnCard")
    reps = [
        ("color = Color.White,\n                    shape = RoundedCornerShape(16.dp)",
         "color = kilnColors.cardBackground,\n                    shape = RoundedCornerShape(16.dp)"),
        ("color = AppColors.Primary100,\n                    shape = RoundedCornerShape(16.dp)",
         "color = kilnColors.cardBorder,\n                    shape = RoundedCornerShape(16.dp)"),
        ("color = AppColors.Primary800,", "color = kilnColors.title,"),
        (".background(color = AppColors.Primary100)",
         ".background(color = kilnColors.accent.copy(alpha = 0.15f))"),
        ("tint = AppColors.Primary600,", "tint = kilnColors.accent,"),
        ("color = AppColors.Primary700,", "color = kilnColors.body,"),
    ]
    for a, b in reps:
        c = t.count(a)
        t = t.replace(a, b)
        print(f"  KilnCard: {c}x {a[:40]!r}")
    p.write_text(t, encoding="utf-8")
    print("OK KilnCard")


def migrate_program_card():
    p = ROOT / "components/ProgramCard.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = inject_kiln_colors(t, "ProgramCard")
    # Also ProgramParameter if exists
    if "fun ProgramParameter" in t:
        t = inject_kiln_colors(t, "ProgramParameter")
    reps = [
        ("color = Color.White,\n                    shape = RoundedCornerShape(12.dp)",
         "color = kilnColors.cardBackground,\n                    shape = RoundedCornerShape(12.dp)"),
        ("color = AppColors.Primary600,", "color = kilnColors.title,"),
        (".background(color = AppColors.Primary600)",
         ".background(color = kilnColors.accent)"),
        ("color = Color.White,", "color = kilnColors.onAccent,"),
        (".background(color = AppColors.Primary100)",
         ".background(color = kilnColors.accent.copy(alpha = 0.15f))"),
        ("tint = AppColors.Primary600,", "tint = kilnColors.accent,"),
        ("color = AppColors.Primary700,", "color = kilnColors.body,"),
    ]
    for a, b in reps:
        c = t.count(a)
        t = t.replace(a, b)
        print(f"  ProgramCard: {c}x {a[:40]!r}")
    p.write_text(t, encoding="utf-8")
    print("OK ProgramCard")


def migrate_nav():
    p = ROOT / "components/ResponsiveBottomNavigation.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    # Replace NavigationTab body to use theme colors
    old = '''    Column(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() } // Solo responde a clicks si está habilitado
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "$text-icon",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        
        // Text
        Text(
            text = text,
            color = Color.White,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        )
        
        // Active indicator
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(
                        color = Color.White,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )
        }
    }'''
    new = '''    val kilnColors = LocalKilnColors.current
    val activeColor = if (kilnColors.isEsp) kilnColors.accent else kilnColors.onAccent
    val inactiveColor = if (kilnColors.isEsp) kilnColors.muted else kilnColors.onAccent.copy(alpha = 0.85f)
    val contentColor = if (isActive) activeColor else inactiveColor

    Column(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() } // Solo responde a clicks si está habilitado
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "$text-icon",
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        // Text
        Text(
            text = text,
            color = contentColor,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        )
        
        // Active indicator
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(
                        color = activeColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )
        }
    }'''
    if old not in t:
        print("MISS nav NavigationTab block")
    else:
        t = t.replace(old, new)
    p.write_text(t, encoding="utf-8")
    print("OK ResponsiveBottomNavigation")


def migrate_kilns_screen():
    p = ROOT / "screens/KilnsScreen.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    reps = [
        ("backgroundColor = AppColors.Primary100,",
         "backgroundColor = kilnColors.screenBackground,"),
        ("containerColor = AppColors.Primary700,\n                            contentColor = Color.White,\n                            disabledContainerColor = AppColors.Primary700,\n                            disabledContentColor = Color.White",
         "containerColor = kilnColors.accent,\n                            contentColor = kilnColors.onAccent,\n                            disabledContainerColor = kilnColors.accent,\n                            disabledContentColor = kilnColors.onAccent"),
        ("color = Color.White,\n                                strokeWidth = 2.dp",
         "color = kilnColors.onAccent,\n                                strokeWidth = 2.dp"),
    ]
    for a, b in reps:
        c = t.count(a)
        t = t.replace(a, b)
        print(f"  KilnsScreen: {c}x")
    p.write_text(t, encoding="utf-8")
    print("OK KilnsScreen")


if __name__ == "__main__":
    migrate_kiln_card()
    migrate_program_card()
    migrate_nav()
    migrate_kilns_screen()

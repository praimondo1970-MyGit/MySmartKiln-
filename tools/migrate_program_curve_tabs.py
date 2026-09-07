"""Migrate KilnProgramScreen, KilnCurveScreen, StageCard to LocalKilnColors."""
from pathlib import Path

ROOT = Path("app/src/main/java/com/example/mysmartkiln/ui/components")
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
    return t.replace(
        "import com.example.mysmartkiln.ui.theme.",
        IMP + "\nimport com.example.mysmartkiln.ui.theme.",
        1,
    )


def inject_after_fun(t: str, fun: str) -> str:
    marker = f"fun {fun}("
    idx = t.find(marker)
    if idx < 0:
        return t
    brace = t.find("{", idx)
    # avoid injecting into default lambdas: find the function body's first {
    # by scanning for ) { after params - use last ) before first { of body
    window = t[idx:brace]
    if "val kilnColors = LocalKilnColors.current" in t[brace : brace + 120]:
        return t
    return t[: brace + 1] + "\n    val kilnColors = LocalKilnColors.current\n" + t[brace + 1 :]


def migrate_program():
    p = ROOT / "KilnProgramScreen.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = inject_after_fun(t, "KilnProgramScreen")
    reps = [
        ("colors = CardDefaults.cardColors(containerColor = Color.White),",
         "colors = CardDefaults.cardColors(containerColor = kilnColors.cardBackground),"),
        ("color = AppColors.Primary700,", "color = kilnColors.title,"),
        ("color = AppColors.Primary400,", "color = kilnColors.muted,"),
        ("color = AppColors.Primary600,", "color = kilnColors.body,"),
        ("containerColor = Color(0xFFFF6F00),\n                    contentColor = Color.White",
         "containerColor = kilnColors.fab,\n                    contentColor = kilnColors.onFab"),
        ("color = AppColors.Primary700,\n                            strokeWidth = 3.dp",
         "color = kilnColors.accent,\n                            strokeWidth = 3.dp"),
        ("containerColor = AppColors.Primary700",
         "containerColor = kilnColors.accent"),
        ("color = Color.White,\n                    fontWeight = FontWeight.Bold",
         "color = kilnColors.onAccent,\n                    fontWeight = FontWeight.Bold"),
    ]
    for a, b in reps:
        t = t.replace(a, b)
    p.write_text(t, encoding="utf-8")
    print("OK KilnProgramScreen")


def migrate_curve():
    p = ROOT / "KilnCurveScreen.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    t = inject_after_fun(t, "KilnCurveScreen")
    # LegendItem also needs kilnColors
    t = inject_after_fun(t, "LegendItem")
    reps = [
        ("colors = CardDefaults.cardColors(containerColor = Color.White),",
         "colors = CardDefaults.cardColors(containerColor = kilnColors.cardBackground),"),
        ("color = AppColors.Primary700,", "color = kilnColors.title,"),
        ("color = AppColors.Primary400,", "color = kilnColors.muted,"),
        ("color = AppColors.Primary600,", "color = kilnColors.body,"),
        ("LegendItem(color = AppColors.Primary700, label = \"Programa\")",
         "LegendItem(color = kilnColors.body, label = \"Programa\")"),
        ("LegendItem(color = Color(0xFFFF6F00), label = \"Real\")",
         "LegendItem(color = kilnColors.accent, label = \"Real\")"),
        ("color = Color(0xFFFF6F00),", "color = kilnColors.accent,"),
        ("color = AppColors.Primary700,\n            fontSize = 13.sp",
         "color = kilnColors.body,\n            fontSize = 13.sp"),
    ]
    # After first replace Primary700 might already be title - fix LegendItem Programa
    # Do LegendItem before generic Primary700 replace for the title line
    for a, b in reps:
        c = t.count(a)
        t = t.replace(a, b)
        print(f"  curve {c}x {a[:50]}")
    p.write_text(t, encoding="utf-8")
    print("OK KilnCurveScreen")


def migrate_stage():
    p = ROOT / "StageCard.kt"
    t = ensure_import(p.read_text(encoding="utf-8"))
    # inject into StageCard and ReadOnlyStageCard and ParameterColumn if any
    for fun in ("StageCard", "ReadOnlyStageCard", "StageParameter"):
        if f"fun {fun}(" in t:
            t = inject_after_fun(t, fun)

    reps = [
        ("isCurrentStage -> AppColors.Primary50",
         "isCurrentStage -> kilnColors.cardBackground"),
        ("isCompleted -> Color(0xFFE8F5E9)",
         "isCompleted -> kilnColors.cardBackground"),
        ("else -> Color.White",
         "else -> kilnColors.cardBackground"),
        ("Modifier.border(2.dp, AppColors.Primary600, RoundedCornerShape(12.dp))",
         "Modifier.border(2.dp, kilnColors.accent, RoundedCornerShape(12.dp))"),
        ("color = Color(0xFF2E7D32),",
         "color = kilnColors.success,"),
        (".background(color = AppColors.Primary100)",
         ".background(color = kilnColors.accent.copy(alpha = 0.15f))"),
        ("tint = AppColors.Primary600,",
         "tint = kilnColors.accent,"),
        ("color = AppColors.Primary700,",
         "color = kilnColors.body,"),
        ("color = AppColors.Primary600,",
         "color = kilnColors.title,"),
        (".background(color = AppColors.Primary600)",
         ".background(color = kilnColors.accent)"),
        ("color = Color.White,",
         "color = kilnColors.onAccent,"),
    ]
    for a, b in reps:
        c = t.count(a)
        t = t.replace(a, b)
        print(f"  stage {c}x {a[:45]}")
    p.write_text(t, encoding="utf-8")
    print("OK StageCard")


if __name__ == "__main__":
    migrate_program()
    migrate_curve()
    migrate_stage()

"""Migrate remaining P0/P1 hardcodes to LocalKilnColors."""
from pathlib import Path

ROOT = Path("app/src/main/java/com/example/mysmartkiln/ui")

IMPORT = "import com.example.mysmartkiln.ui.theme.LocalKilnColors"


def ensure_import(text: str) -> str:
    if "LocalKilnColors" in text and IMPORT in text:
        return text
    if IMPORT in text:
        return text
    # insert after AppColors import if present, else after package/theme imports
    if "import com.example.mysmartkiln.ui.theme.AppColors" in text:
        return text.replace(
            "import com.example.mysmartkiln.ui.theme.AppColors",
            "import com.example.mysmartkiln.ui.theme.AppColors\n" + IMPORT,
            1,
        )
    # after first theme import
    lines = text.splitlines(True)
    out = []
    inserted = False
    for i, line in enumerate(lines):
        out.append(line)
        if not inserted and line.startswith("import com.example.mysmartkiln.ui.theme."):
            out.append(IMPORT + "\n")
            inserted = True
    if not inserted:
        # after package
        out = []
        for line in lines:
            out.append(line)
            if not inserted and line.startswith("package "):
                out.append("\n" + IMPORT + "\n")
                inserted = True
    return "".join(out)


def migrate_app_inputs(path: Path):
    t = path.read_text(encoding="utf-8")
    t = ensure_import(t)
    # Add kilnColors at start of AppTextField and AppFilledTextField bodies
    for fn in ("AppTextField", "AppFilledTextField"):
        # find function and inject after opening brace of Column? better after signature {
        marker = f"fun {fn}("
        idx = t.find(marker)
        if idx < 0:
            continue
        brace = t.find("{", idx)
        # next line after {
        insert_at = brace + 1
        snippet = "\n    val kilnColors = LocalKilnColors.current\n"
        if "val kilnColors = LocalKilnColors.current" not in t[idx : idx + 800]:
            t = t[:insert_at] + snippet + t[insert_at:]

    replacements = [
        ("if (enabled) AppColors.Neutral900 else AppColors.Neutral500",
         "if (enabled) kilnColors.title else kilnColors.muted"),
        ("color = AppColors.Neutral500", "color = kilnColors.muted"),
        ("focusedBorderColor = AppColors.Primary500,", "focusedBorderColor = kilnColors.accent,"),
        ("unfocusedBorderColor = AppColors.Neutral300,", "unfocusedBorderColor = kilnColors.cardBorder,"),
        ("errorBorderColor = AppColors.Error500,", "errorBorderColor = kilnColors.danger,"),
        ("disabledBorderColor = AppColors.Neutral200,", "disabledBorderColor = kilnColors.cardBorder.copy(alpha = 0.5f),"),
        ("focusedTextColor = AppColors.Neutral900,", "focusedTextColor = kilnColors.title,"),
        ("unfocusedTextColor = AppColors.Neutral900,", "unfocusedTextColor = kilnColors.title,"),
        ("errorTextColor = AppColors.Neutral900,", "errorTextColor = kilnColors.title,"),
        ("disabledTextColor = AppColors.Neutral500,", "disabledTextColor = kilnColors.muted,"),
        ("focusedLabelColor = AppColors.Primary500,", "focusedLabelColor = kilnColors.accent,"),
        ("unfocusedLabelColor = AppColors.Neutral600,", "unfocusedLabelColor = kilnColors.muted,"),
        ("errorLabelColor = AppColors.Error500,", "errorLabelColor = kilnColors.danger,"),
        ("disabledLabelColor = AppColors.Neutral400", "disabledLabelColor = kilnColors.muted"),
        ("color = AppColors.Error500", "color = kilnColors.danger"),
        ("color = AppColors.Neutral600", "color = kilnColors.muted"),
        ("focusedContainerColor = AppColors.Neutral50,", "focusedContainerColor = kilnColors.cardBackground,"),
        ("unfocusedContainerColor = AppColors.Neutral100,", "unfocusedContainerColor = kilnColors.cardBackground,"),
        ("errorContainerColor = AppColors.Error50,", "errorContainerColor = kilnColors.danger.copy(alpha = 0.15f),"),
        ("disabledContainerColor = AppColors.Neutral200,", "disabledContainerColor = kilnColors.cardBackground.copy(alpha = 0.6f),"),
        ("focusedIndicatorColor = AppColors.Primary500,", "focusedIndicatorColor = kilnColors.accent,"),
        ("unfocusedIndicatorColor = AppColors.Neutral300,", "unfocusedIndicatorColor = kilnColors.cardBorder,"),
        ("errorIndicatorColor = AppColors.Error500,", "errorIndicatorColor = kilnColors.danger,"),
        ("disabledIndicatorColor = AppColors.Neutral200,", "disabledIndicatorColor = kilnColors.cardBorder.copy(alpha = 0.5f),"),
    ]
    for a, b in replacements:
        t = t.replace(a, b)
    path.write_text(t, encoding="utf-8")
    print("OK AppInputs")


def migrate_file(rel: str, reps: list[tuple[str, str]], add_kiln_val: str | None = None, inject_after_fn: str | None = None):
    path = ROOT / rel
    t = path.read_text(encoding="utf-8")
    t = ensure_import(t)
    if add_kiln_val and inject_after_fn:
        marker = f"fun {inject_after_fn}("
        idx = t.find(marker)
        if idx >= 0 and "val kilnColors = LocalKilnColors.current" not in t[idx:idx+600]:
            brace = t.find("{", idx)
            t = t[: brace + 1] + "\n    val kilnColors = LocalKilnColors.current\n" + t[brace + 1 :]
    for a, b in reps:
        t = t.replace(a, b)
    path.write_text(t, encoding="utf-8")
    print(f"OK {rel}")


def main():
    migrate_app_inputs(ROOT / "components/AppInputs.kt")

    # Plusbtn
    migrate_file(
        "components/Plusbtn.kt",
        [(".background(color = AppColors.color_Terciary_800)",
          ".background(color = LocalKilnColors.current.fab)")],
    )

    # OrangeHexagon - special
    p = ROOT / "components/OrangeHexagon.kt"
    t = p.read_text(encoding="utf-8")
    t = ensure_import(t)
    t = t.replace(
        """fun OrangeHexagon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    color: Color = AppColors.Primary300 // Color del hexágono
) {
    Image(
        painter = painterResource(id = R.drawable.poligono_iconos),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color) // Aplica el color al hexágono
    )
}""",
        """fun OrangeHexagon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    color: Color? = null
) {
    val resolved = color ?: LocalKilnColors.current.accent
    Image(
        painter = painterResource(id = R.drawable.poligono_iconos),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(resolved)
    )
}""",
    )
    p.write_text(t, encoding="utf-8")
    print("OK OrangeHexagon")

    # AppButtons FAB + splash
    migrate_file(
        "components/AppButtons.kt",
        [
            ("containerColor = AppColors.Primary500,\n        contentColor = AppColors.White,",
             "containerColor = LocalKilnColors.current.fab,\n        contentColor = LocalKilnColors.current.onFab,"),
            (".background(color = AppColors.color_Primary_400)",
             ".background(color = LocalKilnColors.current.accent)"),
            ("color = AppColors.color_Primary_800,",
             "color = LocalKilnColors.current.onAccent,"),
            (".background(color = AppColors.color_Terciary_800)",
             ".background(color = LocalKilnColors.current.fab)"),
            (".background(color = AppColors.color_Primary_700)",
             ".background(color = LocalKilnColors.current.muted)"),
        ],
    )

    # StageCard ReadOnly
    migrate_file(
        "components/StageCard.kt",
        [
            ("isCurrentStage -> AppColors.Primary50\n        isCompleted -> Color(0xFFE8F5E9)\n        else -> Color.White",
             "isCurrentStage -> LocalKilnColors.current.cardBackground\n        isCompleted -> LocalKilnColors.current.cardBackground\n        else -> LocalKilnColors.current.cardBackground"),
            ("Modifier.border(2.dp, AppColors.Primary600, RoundedCornerShape(12.dp))",
             "Modifier.border(2.dp, LocalKilnColors.current.accent, RoundedCornerShape(12.dp))"),
            ("color = Color(0xFF2E7D32),",
             "color = LocalKilnColors.current.success,"),
        ],
    )

    # RealtimeDataCard
    migrate_file(
        "components/RealtimeDataCard.kt",
        [
            ("color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5252),",
             "color = if (isConnected) LocalKilnColors.current.success else LocalKilnColors.current.danger,"),
            ("isLoading -> AppColors.Primary600\n                isConnected -> Color(0xFF4CAF50)\n                else -> Color(0xFFFF5252)",
             "isLoading -> LocalKilnColors.current.muted\n                isConnected -> LocalKilnColors.current.success\n                else -> LocalKilnColors.current.danger"),
            ("tint = if (isConnected) AppColors.Primary600 else AppColors.Primary300,",
             "tint = if (isConnected) LocalKilnColors.current.accent else LocalKilnColors.current.muted,"),
            ("color = if (isConnected) AppColors.Primary800 else AppColors.Primary400,",
             "color = if (isConnected) LocalKilnColors.current.title else LocalKilnColors.current.muted,"),
        ],
    )

    # KilnProgramScreen FAB
    migrate_file(
        "components/KilnProgramScreen.kt",
        [
            ("containerColor = Color(0xFFFF6F00),\n                    contentColor = Color.White",
             "containerColor = LocalKilnColors.current.fab,\n                    contentColor = LocalKilnColors.current.onFab"),
        ],
    )

    # KilnCurveScreen
    migrate_file(
        "components/KilnCurveScreen.kt",
        [
            ("LegendItem(color = Color(0xFFFF6F00), label = \"Real\")",
             "LegendItem(color = LocalKilnColors.current.accent, label = \"Real\")"),
            ("color = Color(0xFFFF6F00),",
             "color = LocalKilnColors.current.accent,"),
        ],
    )

    # CreateProgramScreen
    migrate_file(
        "screens/CreateProgramScreen.kt",
        [
            ("contentColor = Color.White,\n                            disabledContainerColor = AppColors.Neutral400, // Gris para estado inactivo\n                            disabledContentColor = AppColors.Neutral600 // Texto gris para estado inactivo",
             "contentColor = LocalKilnColors.current.onAccent,\n                            disabledContainerColor = LocalKilnColors.current.cardBorder,\n                            disabledContentColor = LocalKilnColors.current.muted"),
        ],
    )

    # Login CTA + containers
    migrate_file(
        "screens/LoginScreen.kt",
        [
            ("focusedContainerColor = AppColors.Primary800,\n                    unfocusedContainerColor = AppColors.Primary800",
             "focusedContainerColor = LocalKilnColors.current.brandSurface,\n                    unfocusedContainerColor = LocalKilnColors.current.brandSurface"),
            ("checkedColor = Color.White,\n                            uncheckedColor = Color.White,\n                            checkmarkColor = AppColors.Primary800",
             "checkedColor = LocalKilnColors.current.onAccent,\n                            uncheckedColor = LocalKilnColors.current.onAccent,\n                            checkmarkColor = LocalKilnColors.current.brandSurface"),
            ("containerColor = Color(0xFF3385FF)",
             "containerColor = LocalKilnColors.current.accent"),
            ("focusedTextColor = Color.White,", "focusedTextColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedTextColor = Color.White,", "unfocusedTextColor = LocalKilnColors.current.onAccent,"),
            ("focusedLabelColor = Color.White,", "focusedLabelColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedLabelColor = Color.White,", "unfocusedLabelColor = LocalKilnColors.current.onAccent,"),
            ("focusedIndicatorColor = Color.White,", "focusedIndicatorColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedIndicatorColor = Color.White,", "unfocusedIndicatorColor = LocalKilnColors.current.onAccent,"),
            ("cursorColor = Color.White,", "cursorColor = LocalKilnColors.current.onAccent,"),
        ],
    )

    migrate_file(
        "screens/SignInScreen.kt",
        [
            ("focusedContainerColor = AppColors.Primary800,\n                    unfocusedContainerColor = AppColors.Primary800",
             "focusedContainerColor = LocalKilnColors.current.brandSurface,\n                    unfocusedContainerColor = LocalKilnColors.current.brandSurface"),
            ("containerColor = Color(0xFF3385FF)",
             "containerColor = LocalKilnColors.current.accent"),
            ("focusedTextColor = Color.White,", "focusedTextColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedTextColor = Color.White,", "unfocusedTextColor = LocalKilnColors.current.onAccent,"),
            ("focusedLabelColor = Color.White,", "focusedLabelColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedLabelColor = Color.White,", "unfocusedLabelColor = LocalKilnColors.current.onAccent,"),
            ("focusedIndicatorColor = Color.White,", "focusedIndicatorColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedIndicatorColor = Color.White,", "unfocusedIndicatorColor = LocalKilnColors.current.onAccent,"),
            ("cursorColor = Color.White,", "cursorColor = LocalKilnColors.current.onAccent,"),
        ],
    )

    migrate_file(
        "components/ForgotPasswordDialog.kt",
        [
            ("containerColor = AppColors.Primary800,",
             "containerColor = LocalKilnColors.current.brandSurface,"),
            ("focusedContainerColor = AppColors.Primary800,\n                        unfocusedContainerColor = AppColors.Primary800",
             "focusedContainerColor = LocalKilnColors.current.brandSurface,\n                        unfocusedContainerColor = LocalKilnColors.current.brandSurface"),
            ("containerColor = Color(0xFF3385FF)",
             "containerColor = LocalKilnColors.current.accent"),
            ("focusedTextColor = Color.White,", "focusedTextColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedTextColor = Color.White,", "unfocusedTextColor = LocalKilnColors.current.onAccent,"),
            ("focusedLabelColor = Color.White,", "focusedLabelColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedLabelColor = Color.White,", "unfocusedLabelColor = LocalKilnColors.current.onAccent,"),
            ("focusedIndicatorColor = Color.White,", "focusedIndicatorColor = LocalKilnColors.current.onAccent,"),
            ("unfocusedIndicatorColor = Color.White,", "unfocusedIndicatorColor = LocalKilnColors.current.onAccent,"),
            ("cursorColor = Color.White,", "cursorColor = LocalKilnColors.current.onAccent,"),
        ],
    )

    print("Done")


if __name__ == "__main__":
    main()

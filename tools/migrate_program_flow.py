"""Migrate create/edit program flow screens + related inputs to LocalKilnColors."""
from pathlib import Path

ROOT = Path("app/src/main/java/com/example/mysmartkiln/ui")
IMP = "import com.example.mysmartkiln.ui.theme.LocalKilnColors"

FILES = [
    "screens/CreateProgramScreen.kt",
    "screens/AddStageScreen.kt",
    "screens/SelectProgramScreen.kt",
    "components/ProgramNameInput.kt",
    "components/StageInputField.kt",
    "components/StageLabels.kt",
    "components/TemperatureInput.kt",
    "components/SpeedInput.kt",
    "components/MaintenanceInput.kt",
    "components/AddStageButton.kt",
    "components/AgregarButton.kt",
    "components/AgregarEtapa.kt",
    "components/CrearPrograma.kt",
    "components/Etapas.kt",
    "components/TextFieldWithLabel.kt",
    "components/NewProgramCard.kt",
    "components/NewKilnCard.kt",
    "components/SimpleNumericKeyboard.kt",
    "components/BackIcon.kt",
]


def ensure_import(t: str) -> str:
    if IMP in t:
        return t
    if "import com.example.mysmartkiln.ui.theme.AppColors" in t:
        return t.replace(
            "import com.example.mysmartkiln.ui.theme.AppColors",
            "import com.example.mysmartkiln.ui.theme.AppColors\n" + IMP,
            1,
        )
    lines = t.splitlines(True)
    out, done = [], False
    for line in lines:
        out.append(line)
        if not done and line.startswith("package "):
            out.append("\n" + IMP + "\n")
            done = True
    return "".join(out)


# Order matters: more specific first
REPS = [
    # backgrounds
    (".background(color = AppColors.Primary100)", ".background(color = LocalKilnColors.current.screenBackground)"),
    (".background(color = AppColors.Primary100)", ".background(color = LocalKilnColors.current.screenBackground)"),
    ("backgroundColor = AppColors.Primary100,", "backgroundColor = LocalKilnColors.current.screenBackground,"),
    (".background(\n                        color = AppColors.Primary800.copy(alpha = 0.9f) // 90% opacity",
     ".background(\n                        color = LocalKilnColors.current.navBar.copy(alpha = 0.9f)"),
    ("AppColors.Primary800.copy(alpha = 0.9f)", "LocalKilnColors.current.navBar.copy(alpha = 0.9f)"),
    # titles
    ("Color(0xFF1E3A8A)", "LocalKilnColors.current.title"),
    # buttons
    ("containerColor = AppColors.Primary700, // Color Primary700 (#003D99)\n                            contentColor = Color.White,\n                            disabledContainerColor = AppColors.Neutral400, // Gris para estado inactivo\n                            disabledContentColor = AppColors.Neutral600 // Texto gris para estado inactivo",
     "containerColor = LocalKilnColors.current.accent,\n                            contentColor = LocalKilnColors.current.onAccent,\n                            disabledContainerColor = LocalKilnColors.current.cardBorder,\n                            disabledContentColor = LocalKilnColors.current.muted"),
    ("containerColor = AppColors.Primary700, // Same color as Comenzar button\n            contentColor = Color.White,\n            disabledContainerColor = AppColors.Neutral400, // Gray for disabled state\n            disabledContentColor = AppColors.Neutral600 // Gray text for disabled state",
     "containerColor = LocalKilnColors.current.accent,\n            contentColor = LocalKilnColors.current.onAccent,\n            disabledContainerColor = LocalKilnColors.current.cardBorder,\n            disabledContentColor = LocalKilnColors.current.muted"),
    # inputs white fields -> card
    (".background(color = Color.White)", ".background(color = LocalKilnColors.current.cardBackground)"),
    (".background(color = AppColors.White)", ".background(color = LocalKilnColors.current.cardBackground)"),
    ("border = BorderStroke(1.dp, AppColors.Primary100),", "border = BorderStroke(1.dp, LocalKilnColors.current.cardBorder),"),
    (".background(color = AppColors.Primary100)", ".background(color = LocalKilnColors.current.accent.copy(alpha = 0.15f))"),
    # primary text / cursor
    ("AppColors.color_Primary_700", "LocalKilnColors.current.accent"),
    ("AppColors.Primary700", "LocalKilnColors.current.accent"),
    ("AppColors.Primary600", "LocalKilnColors.current.body"),
    ("AppColors.Primary400", "LocalKilnColors.current.muted"),
    ("tint = AppColors.Primary700", "tint = LocalKilnColors.current.accent"),
    ("AppColors.color_Terciary_800", "LocalKilnColors.current.fab"),
    ("AppColors.Neutral500", "LocalKilnColors.current.muted"),
    ("AppColors.Neutral600", "LocalKilnColors.current.muted"),
    ("AppColors.Neutral400", "LocalKilnColors.current.cardBorder"),
    ("AppColors.Neutral100", "LocalKilnColors.current.cardBackground"),
    ("AppColors.color_Shades_White", "LocalKilnColors.current.cardBackground"),
    # NewProgramCard
    ("tint = Color.White, // Blanco para contrastar con el hexágono azul", "tint = LocalKilnColors.current.onAccent,"),
    ("color = Color.White,", "color = LocalKilnColors.current.onAccent,"),
]


def main():
    for rel in FILES:
        p = ROOT / rel
        if not p.exists():
            print("MISS", rel)
            continue
        t = ensure_import(p.read_text(encoding="utf-8"))
        before = t
        for a, b in REPS:
            t = t.replace(a, b)
        # CreateProgram may still have Primary700 without comment
        t = t.replace("containerColor = AppColors.Primary700,", "containerColor = LocalKilnColors.current.accent,")
        t = t.replace("contentColor = Color.White,", "contentColor = LocalKilnColors.current.onAccent,")
        if t != before:
            p.write_text(t, encoding="utf-8")
            left = t.count("AppColors.") + t.count("Color.White") + t.count("0xFF1E3A8A")
            print(f"OK {rel} leftover~{left}")
        else:
            print(f"-- {rel} unchanged")


if __name__ == "__main__":
    main()

package app.longwave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.longwave.R

/**
 * A bedroom at night with an old valve radio on the nightstand: walnut, brass, a dial glowing
 * amber, a green tuning eye. Nothing bright and nothing blue-white.
 */
@Immutable
data class Palette(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val wood: Color,
    val woodDark: Color,
    val woodLight: Color,
    val brass: Color,
    val brassDark: Color,
    val cloth: Color,
    val clothLine: Color,
    val glassLit: Color,
    val glassDim: Color,
    val glassOff: Color,
    val print: Color,
    val printProgramme: Color,
    val printRare: Color,
    val needle: Color,
    val eye: Color,
    val eyeDark: Color,
    val paper: Color,
    val paperInk: Color,
    val accent: Color,
)

val Night = Palette(
    bg = Color(0xFF0C0A0B),
    bg2 = Color(0xFF151112),
    surface = Color(0xFF1B1617),
    line = Color(0xFF2E2626),
    ink = Color(0xFFEFE3CC),
    muted = Color(0xFFAB9E88),
    faint = Color(0xFF6E6457),
    wood = Color(0xFF4A2A18),
    woodDark = Color(0xFF2A160C),
    woodLight = Color(0xFF6B3E22),
    brass = Color(0xFFC9A45C),
    brassDark = Color(0xFF7A5F2E),
    cloth = Color(0xFF2C2420),
    clothLine = Color(0xFF3C322B),
    glassLit = Color(0xFFF0C377),
    glassDim = Color(0xFFB07A36),
    glassOff = Color(0xFF2B2219),
    print = Color(0xFF2B1A0C),
    printProgramme = Color(0xFF1E4A3A),
    printRare = Color(0xFF4E2F66),
    needle = Color(0xFFD2432F),
    eye = Color(0xFF79F2A6),
    eyeDark = Color(0xFF0D2418),
    paper = Color(0xFFEFE5CC),
    paperInk = Color(0xFF2D261C),
    accent = Color(0xFFE8B765),
)

val LocalPalette = staticCompositionLocalOf { Night }

val Prata = FontFamily(Font(R.font.prata, FontWeight.Normal))
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))
private val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
)

@Immutable
data class Type(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val small: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
    val hand: TextStyle,
)

private val Types = Type(
    display = TextStyle(fontFamily = Prata, fontSize = 30.sp, lineHeight = 38.sp),
    title = TextStyle(fontFamily = Prata, fontSize = 22.sp, lineHeight = 30.sp),
    body = TextStyle(fontFamily = Manrope, fontSize = 15.sp, lineHeight = 22.sp),
    small = TextStyle(fontFamily = Manrope, fontSize = 13.sp, lineHeight = 18.sp),
    label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp),
    mono = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 18.sp),
    hand = TextStyle(fontFamily = Hand, fontSize = 26.sp, lineHeight = 28.sp),
)

val LocalType = staticCompositionLocalOf { Types }

@Composable
fun LongwaveTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides Night, LocalType provides Types) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Night.accent,
                onPrimary = Color(0xFF2A1A08),
                background = Night.bg,
                surface = Night.surface,
                surfaceVariant = Night.bg2,
                outline = Night.line,
                onBackground = Night.ink,
                onSurface = Night.ink,
                onSurfaceVariant = Night.muted,
                secondaryContainer = Night.line,
                onSecondaryContainer = Night.ink,
            ),
            content = content,
        )
    }
}

package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.R

/** Cores de situação, além das do Material 3. Vêm dos tokens do protótipo (--ok, --warn, --bad). */
data class CoresAcerto(
    val ok: Color,
    val okFundo: Color,
    val aviso: Color,
    val avisoFundo: Color,
    val ruim: Color,
    val ruimFundo: Color,
    val sol: Color,
    val campoBorda: Color,
    val campoFundo: Color,
)

val LocalCoresAcerto = staticCompositionLocalOf<CoresAcerto> { error("Tema não aplicado") }

private val Claro = lightColorScheme(
    background = Color(0xFFF0F8F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBF5F4),
    surfaceContainer = Color(0xFFEBF5F4),
    onBackground = Color(0xFF0A2225),
    onSurface = Color(0xFF0A2225),
    onSurfaceVariant = Color(0xFF536869),
    outlineVariant = Color(0xFFD5E1E0),
    outline = Color(0xFF6F8A8A),
    primary = Color(0xFF00766C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCF7F3),
    onPrimaryContainer = Color(0xFF0A2225),
    secondaryContainer = Color(0xFFDCF7F3),
    onSecondaryContainer = Color(0xFF0A2225),
    error = Color(0xFFB6322D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE2DD),
    onErrorContainer = Color(0xFF0A2225),
    // O aviso com "Desfazer" é escuro no tema claro: o botão usa o verde-água da marca.
    inverseSurface = Color(0xFF0A2225),
    inverseOnSurface = Color(0xFFF0F8F7),
    inversePrimary = Color(0xFF49D3C3),
)

private val Escuro = darkColorScheme(
    background = Color(0xFF061213),
    surface = Color(0xFF0E1C1E),
    surfaceVariant = Color(0xFF162627),
    surfaceContainer = Color(0xFF162627),
    onBackground = Color(0xFFE7F1F0),
    onSurface = Color(0xFFE7F1F0),
    onSurfaceVariant = Color(0xFF9DB3B3),
    outlineVariant = Color(0xFF263637),
    outline = Color(0xFF6B8D8D),
    primary = Color(0xFF49D3C3),
    onPrimary = Color(0xFF061213),
    primaryContainer = Color(0xFF063E3A),
    onPrimaryContainer = Color(0xFFE7F1F0),
    secondaryContainer = Color(0xFF063E3A),
    onSecondaryContainer = Color(0xFFE7F1F0),
    error = Color(0xFFF98F84),
    onError = Color(0xFF061213),
    errorContainer = Color(0xFF47211D),
    onErrorContainer = Color(0xFFE7F1F0),
    inverseSurface = Color(0xFFE7F1F0),
    inverseOnSurface = Color(0xFF061213),
    inversePrimary = Color(0xFF00766C),
)

private val CoresClaro = CoresAcerto(
    ok = Color(0xFF15733C), okFundo = Color(0xFFD9F8E0),
    aviso = Color(0xFF8F5D04), avisoFundo = Color(0xFFFFECC1),
    ruim = Color(0xFFB6322D), ruimFundo = Color(0xFFFFE2DD),
    sol = Color(0xFFFBB636), campoBorda = Color(0xFF6F8A8A), campoFundo = Color(0xFFF5FAF9),
)

private val CoresEscuro = CoresAcerto(
    ok = Color(0xFF78D694), okFundo = Color(0xFF193521),
    aviso = Color(0xFFF5C25F), avisoFundo = Color(0xFF3E2D10),
    ruim = Color(0xFFF98F84), ruimFundo = Color(0xFF47211D),
    sol = Color(0xFFFFC249), campoBorda = Color(0xFF6B8D8D), campoFundo = Color(0xFF0A1718),
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

/** O tema claro é o padrão (R18): o escuro só entra quando a pessoa escolhe. */
/** Fontes da marca, embutidas no app (funciona sem internet): títulos em Bricolage Grotesque, textos em IBM Plex Sans. */
private val Titulos = FontFamily(
    Font(R.font.bricolage_600, FontWeight.SemiBold),
    Font(R.font.bricolage_700, FontWeight.Bold),
    Font(R.font.bricolage_600, FontWeight.Normal),
)
private val Textos = FontFamily(
    Font(R.font.plex_400, FontWeight.Normal),
    Font(R.font.plex_500, FontWeight.Medium),
    Font(R.font.plex_600, FontWeight.SemiBold),
    Font(R.font.plex_600, FontWeight.Bold),
)

private val Padrao = Typography()
private val Tipografia = Typography(
    displayLarge = Padrao.displayLarge.copy(fontFamily = Titulos),
    displayMedium = Padrao.displayMedium.copy(fontFamily = Titulos),
    displaySmall = Padrao.displaySmall.copy(fontFamily = Titulos),
    headlineLarge = Padrao.headlineLarge.copy(fontFamily = Titulos),
    headlineMedium = Padrao.headlineMedium.copy(fontFamily = Titulos),
    headlineSmall = Padrao.headlineSmall.copy(fontFamily = Titulos),
    titleLarge = Padrao.titleLarge.copy(fontFamily = Titulos),
    titleMedium = Padrao.titleMedium.copy(fontFamily = Titulos),
    titleSmall = Padrao.titleSmall.copy(fontFamily = Titulos),
    bodyLarge = Padrao.bodyLarge.copy(fontFamily = Textos),
    bodyMedium = Padrao.bodyMedium.copy(fontFamily = Textos),
    bodySmall = Padrao.bodySmall.copy(fontFamily = Textos),
    labelLarge = Padrao.labelLarge.copy(fontFamily = Textos),
    labelMedium = Padrao.labelMedium.copy(fontFamily = Textos),
    labelSmall = Padrao.labelSmall.copy(fontFamily = Textos),
)

@Composable
fun TemaAcerto(escuro: Boolean = false, conteudo: @Composable () -> Unit) {
    val esquema: ColorScheme = if (escuro) Escuro else Claro
    CompositionLocalProvider(LocalCoresAcerto provides if (escuro) CoresEscuro else CoresClaro) {
        MaterialTheme(colorScheme = esquema, shapes = Formas, typography = Tipografia, content = conteudo)
    }
}

val MaterialTheme.acerto: CoresAcerto
    @Composable get() = LocalCoresAcerto.current

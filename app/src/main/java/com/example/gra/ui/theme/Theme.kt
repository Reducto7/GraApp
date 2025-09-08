package com.example.gra.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00646C), //颜色8
    secondary = Color(0xFF00474F), //颜色9（深）
    tertiary = Color(0xFF06868A) //颜色7（亮）

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

// ====== 定义可选色板集合 ======
@Stable
data class AppPalette(
    val key: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val label: String
)

object Palettes {

    val Default = AppPalette(
        key = "default",
        primary = Color(0xFF00646C), //cyan8 0xFF00646C
        secondary = Color(0xFF00474F), //cyan9
        tertiary = Color(0xFF06868A), //cyan7
        label = "默认"
    )
    val DarkBlue = AppPalette(
        key = "darkBlue",
        primary = Color(0xFF2B4F66),  // bluePurple8
        secondary = Color(0xFF1E3647),// bluePurple9
        tertiary = Color(0xFF3F6B82), // bluePurple7
        label = "深蓝"
    )
    val Green = AppPalette(
        key = "green",
        primary = Color(0xFF2C5A4D),  // blue8
        secondary = Color(0xFF1E4036),// blue9
        tertiary = Color(0xFF3C7A6B), // blue7
        label = "深绿"
    )

    val LightBlue = AppPalette(
        key = "lightBlue",
        primary = Color(0xFF4A5E8A),  // bluePurple8
        secondary = Color(0xFF344567),// bluePurple9
        tertiary = Color(0xFF6078A6), // bluePurple7
        label = "蓝紫"
    )

    val all = listOf(Default, DarkBlue, Green, LightBlue)
    fun byKey(key: String) = all.firstOrNull { it.key == key } ?: Default
}

// ====== 全局主题状态（运行时切换立刻生效） ======
object AppThemeState {
    // 默认用你的“默认色板”
    var current by mutableStateOf(Palettes.Default)
        private set

    fun setPalette(key: String) {
        current = Palettes.byKey(key)
    }
}

@Composable
fun GraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // ✅ 新增：根据当前选择的色板覆盖主色
    val p = AppThemeState.current
    val overridden = colorScheme.copy(
        primary = p.primary,
        secondary = p.secondary,
        tertiary = p.tertiary
    )

    MaterialTheme(
        colorScheme = overridden,
        typography = Typography,
        content = content
    )
}
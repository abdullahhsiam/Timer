package com.example

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject

data class ComponentStyle(
    val bgColor: String = "#0F0F12",
    val opacity: Float = 0.8f,
    val blur: Float = 15f,
    val borderColor: String = "#FFFFFF",
    val borderThickness: Float = 1f,
    val glowColor: String = "#7C4DFF",
    val glowStrength: Float = 0.3f,
    val cornerRadius: Int = 16,
    val textColor: String = "#FFFFFF",
    val accentColor: String = "#D0BCFF",
    val shadowIntensity: Float = 4f,
    val gradientEnabled: Boolean = false,
    val gradientStartColor: String = "#160E2E",
    val gradientEndColor: String = "#20113B",
    val wallpaperAware: Boolean = false,
    val customWallpaperUri: String = ""
) {
    fun getComposeBgColor(): Color = parseHexColor(bgColor).copy(alpha = opacity)
    fun getComposeBorderColor(): Color = parseHexColor(borderColor)
    fun getComposeGlowColor(): Color = parseHexColor(glowColor).copy(alpha = glowStrength)
    fun getComposeTextColor(): Color = parseHexColor(textColor)
    fun getComposeAccentColor(): Color = parseHexColor(accentColor)
    fun getComposeGradientStart(): Color = parseHexColor(gradientStartColor)
    fun getComposeGradientEnd(): Color = parseHexColor(gradientEndColor)

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("bgColor", bgColor)
        obj.put("opacity", opacity.toDouble())
        obj.put("blur", blur.toDouble())
        obj.put("borderColor", borderColor)
        obj.put("borderThickness", borderThickness.toDouble())
        obj.put("glowColor", glowColor)
        obj.put("glowStrength", glowStrength.toDouble())
        obj.put("cornerRadius", cornerRadius)
        obj.put("textColor", textColor)
        obj.put("accentColor", accentColor)
        obj.put("shadowIntensity", shadowIntensity.toDouble())
        obj.put("gradientEnabled", gradientEnabled)
        obj.put("gradientStartColor", gradientStartColor)
        obj.put("gradientEndColor", gradientEndColor)
        obj.put("wallpaperAware", wallpaperAware)
        obj.put("customWallpaperUri", customWallpaperUri)
        return obj
    }

    companion object {
        fun parseHexColor(hex: String): Color {
            return try {
                Color(AndroidColor.parseColor(hex))
            } catch (e: Exception) {
                if (hex.startsWith("#") && hex.length == 7) {
                    // Try adding transparency or default opacity
                    try { LightenColor(hex) } catch (x: Exception) { Color.LightGray }
                } else {
                    Color.White
                }
            }
        }

        private fun LightenColor(hex: String): Color {
            val colorInt = AndroidColor.parseColor(hex)
            return Color(colorInt)
        }

        fun fromJsonObject(obj: JSONObject): ComponentStyle {
            return ComponentStyle(
                bgColor = obj.optString("bgColor", "#0F0F12"),
                opacity = obj.optDouble("opacity", 0.8).toFloat(),
                blur = obj.optDouble("blur", 15.0).toFloat(),
                borderColor = obj.optString("borderColor", "#FFFFFF"),
                borderThickness = obj.optDouble("borderThickness", 1.0).toFloat(),
                glowColor = obj.optString("glowColor", "#7C4DFF"),
                glowStrength = obj.optDouble("glowStrength", 0.3).toFloat(),
                cornerRadius = obj.optInt("cornerRadius", 16),
                textColor = obj.optString("textColor", "#FFFFFF"),
                accentColor = obj.optString("accentColor", "#D0BCFF"),
                shadowIntensity = obj.optDouble("shadowIntensity", 4.0).toFloat(),
                gradientEnabled = obj.optBoolean("gradientEnabled", false),
                gradientStartColor = obj.optString("gradientStartColor", "#160E2E"),
                gradientEndColor = obj.optString("gradientEndColor", "#20113B"),
                wallpaperAware = obj.optBoolean("wallpaperAware", false),
                customWallpaperUri = obj.optString("customWallpaperUri", "")
            )
        }
    }
}

data class AppAppearanceConfig(
    val dockableIsland: ComponentStyle = ComponentStyle(
        bgColor = "#000000",
        opacity = 0.96f,
        blur = 4f,
        borderColor = "#1A1A1A",
        borderThickness = 1.2f,
        glowColor = "#FFFFFF",
        glowStrength = 0.15f,
        cornerRadius = 24,
        textColor = "#FFFFFF",
        accentColor = "#00E6FF",
        shadowIntensity = 8f,
        gradientEnabled = false,
        gradientStartColor = "#000000",
        gradientEndColor = "#111111",
        wallpaperAware = false,
        customWallpaperUri = ""
    ),
    val floatingBubble: ComponentStyle = ComponentStyle(
        bgColor = "#0A0A0F",
        opacity = 0.65f,
        blur = 20f,
        borderColor = "#D0BCFF",
        borderThickness = 1f,
        glowColor = "#7C4DFF",
        glowStrength = 0.40f,
        cornerRadius = 28,
        textColor = "#FFFFFF",
        accentColor = "#D0BCFF",
        shadowIntensity = 6f,
        gradientEnabled = true,
        gradientStartColor = "#100B26",
        gradientEndColor = "#05040A",
        wallpaperAware = true,
        customWallpaperUri = ""
    ),
    val expandedBubblePanel: ComponentStyle = ComponentStyle(
        bgColor = "#0A0A0F",
        opacity = 0.70f,
        blur = 25f,
        borderColor = "#D0BCFF",
        borderThickness = 1.2f,
        glowColor = "#7C4DFF",
        glowStrength = 0.45f,
        cornerRadius = 24,
        textColor = "#FFFFFF",
        accentColor = "#D0BCFF",
        shadowIntensity = 8f,
        gradientEnabled = true,
        gradientStartColor = "#100B26",
        gradientEndColor = "#05040A",
        wallpaperAware = true,
        customWallpaperUri = ""
    ),
    val timerWidget: ComponentStyle = ComponentStyle(
        bgColor = "#16161C",
        opacity = 0.85f,
        blur = 15f,
        borderColor = "#581C87",
        borderThickness = 1f,
        glowColor = "#7C4DFF",
        glowStrength = 0.25f,
        cornerRadius = 20,
        textColor = "#E4E4E7",
        accentColor = "#D0BCFF",
        shadowIntensity = 4f,
        gradientEnabled = false,
        gradientStartColor = "#1E1B4B",
        gradientEndColor = "#311042",
        wallpaperAware = true,
        customWallpaperUri = ""
    ),
    val stopwatchWidget: ComponentStyle = ComponentStyle(
        bgColor = "#16161C",
        opacity = 0.85f,
        blur = 15f,
        borderColor = "#FF1E56",
        borderThickness = 1f,
        glowColor = "#FF1E56",
        glowStrength = 0.25f,
        cornerRadius = 20,
        textColor = "#E4E4E7",
        accentColor = "#FF2A6D",
        shadowIntensity = 4f,
        gradientEnabled = false,
        gradientStartColor = "#1E1B4B",
        gradientEndColor = "#311042",
        wallpaperAware = true,
        customWallpaperUri = ""
    ),
    val notificationControls: ComponentStyle = ComponentStyle(
        bgColor = "#1A1525",
        opacity = 1.0f,
        blur = 0f,
        borderColor = "#1A1525",
        borderThickness = 0f,
        glowColor = "#D0BCFF",
        glowStrength = 0.1f,
        cornerRadius = 12,
        textColor = "#FFFFFF",
        accentColor = "#D0BCFF",
        shadowIntensity = 0f,
        gradientEnabled = false,
        gradientStartColor = "#1A1525",
        gradientEndColor = "#140E1B",
        wallpaperAware = false,
        customWallpaperUri = ""
    )
) {
    fun toSerializedString(): String {
        val root = JSONObject()
        root.put("dockableIsland", dockableIsland.toJsonObject())
        root.put("floatingBubble", floatingBubble.toJsonObject())
        root.put("expandedBubblePanel", expandedBubblePanel.toJsonObject())
        root.put("timerWidget", timerWidget.toJsonObject())
        root.put("stopwatchWidget", stopwatchWidget.toJsonObject())
        root.put("notificationControls", notificationControls.toJsonObject())
        return root.toString()
    }

    companion object {
        fun fromSerializedString(jsonStr: String): AppAppearanceConfig {
            return try {
                val root = JSONObject(jsonStr)
                AppAppearanceConfig(
                    dockableIsland = ComponentStyle.fromJsonObject(root.getJSONObject("dockableIsland")),
                    floatingBubble = ComponentStyle.fromJsonObject(root.getJSONObject("floatingBubble")),
                    expandedBubblePanel = ComponentStyle.fromJsonObject(root.getJSONObject("expandedBubblePanel")),
                    timerWidget = ComponentStyle.fromJsonObject(root.getJSONObject("timerWidget")),
                    stopwatchWidget = ComponentStyle.fromJsonObject(root.getJSONObject("stopwatchWidget")),
                    notificationControls = ComponentStyle.fromJsonObject(root.getJSONObject("notificationControls"))
                )
            } catch (e: Exception) {
                AppAppearanceConfig() // Fallback to default styling configuration
            }
        }
    }
}

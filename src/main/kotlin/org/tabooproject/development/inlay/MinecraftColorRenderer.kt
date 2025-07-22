package org.tabooproject.development.inlay

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

/**
 * Minecraft颜色渲染器
 * 
 * 用于处理Minecraft格式的颜色代码
 * 
 * @since 1.42
 */
object MinecraftColorRenderer {

    /**
     * 标准Minecraft颜色代码映射
     */
    private val MC_COLORS = mapOf(
        '0' to Color(0, 0, 0),              // BLACK
        '1' to Color(0, 0, 170),            // DARK_BLUE
        '2' to Color(0, 170, 0),            // DARK_GREEN
        '3' to Color(0, 170, 170),          // DARK_AQUA
        '4' to Color(170, 0, 0),            // DARK_RED
        '5' to Color(170, 0, 170),          // DARK_PURPLE
        '6' to Color(255, 170, 0),          // GOLD
        '7' to Color(170, 170, 170),        // GRAY
        '8' to Color(85, 85, 85),           // DARK_GRAY
        '9' to Color(85, 85, 255),          // BLUE
        'a' to Color(85, 255, 85),          // GREEN
        'b' to Color(85, 255, 255),         // AQUA
        'c' to Color(255, 85, 85),          // RED
        'd' to Color(255, 85, 255),         // LIGHT_PURPLE
        'e' to Color(255, 255, 85),         // YELLOW
        'f' to Color(255, 255, 255)         // WHITE
    )

    /**
     * 格式代码映射
     */
    private val FORMAT_CODES = mapOf(
        'k' to TextAttributes(null, null, null, null, Font.PLAIN),  // OBFUSCATED
        'l' to TextAttributes(null, null, null, null, Font.BOLD),   // BOLD
        'm' to TextAttributes(null, null, null, null, Font.PLAIN),  // STRIKETHROUGH
        'n' to TextAttributes(null, null, null, null, Font.PLAIN),  // UNDERLINE
        'o' to TextAttributes(null, null, null, null, Font.ITALIC), // ITALIC
        'r' to TextAttributes(null, null, null, null, Font.PLAIN)   // RESET
    )

    /**
     * 检查文本中是否有颜色代码
     * 
     * @param text 要检查的文本
     * @return 如果包含颜色代码返回true
     */
    fun hasColorCodes(text: String): Boolean {
        val standardColorPattern = "§[0-9a-fk-or]".toRegex()
        val rgbColorPattern = "§\\{#[0-9a-fA-F]{6}}".toRegex()
        
        return standardColorPattern.containsMatchIn(text) || rgbColorPattern.containsMatchIn(text)
    }

    /**
     * 解析RGB颜色代码
     * 
     * @param colorCode RGB颜色代码 (#RRGGBB)
     * @return 颜色对象
     */
    fun parseRGBColor(colorCode: String): Color? {
        if (!colorCode.startsWith("#") || colorCode.length != 7) {
            return null
        }
        
        return try {
            val r = colorCode.substring(1, 3).toInt(16)
            val g = colorCode.substring(3, 5).toInt(16)
            val b = colorCode.substring(5, 7).toInt(16)
            Color(r, g, b)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取Minecraft颜色对应的TextAttributesKey
     * 
     * @param colorCode 颜色代码
     * @return TextAttributesKey
     */
    fun getColorAttributes(colorCode: Char): TextAttributesKey {
        val color = MC_COLORS[colorCode] ?: return DefaultLanguageHighlighterColors.STRING
        val attributes = TextAttributes(color, null, null, null, Font.PLAIN)
        return TextAttributesKey.createTextAttributesKey("MC_COLOR_$colorCode", attributes)
    }
    
    /**
     * 获取格式代码对应的TextAttributesKey
     * 
     * @param formatCode 格式代码
     * @return TextAttributesKey
     */
    fun getFormatAttributes(formatCode: Char): TextAttributesKey {
        val attributes = FORMAT_CODES[formatCode] ?: return DefaultLanguageHighlighterColors.STRING
        return TextAttributesKey.createTextAttributesKey("MC_FORMAT_$formatCode", attributes)
    }
}

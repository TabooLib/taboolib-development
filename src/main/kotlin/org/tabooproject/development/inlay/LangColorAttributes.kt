package org.tabooproject.development.inlay

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color

/**
 * TabooLib语言文件颜色属性管理器
 * 
 * 为不同的Minecraft颜色代码创建对应的TextAttributesKey
 * 
 * @since 1.42
 */
object LangColorAttributes {

    /**
     * Minecraft颜色代码映射
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
     * 为每种颜色创建TextAttributesKey
     */
    private val COLOR_ATTRIBUTES = MC_COLORS.mapValues { (colorCode, color) ->
        TextAttributesKey.createTextAttributesKey(
            "TABOOLIB_LANG_COLOR_$colorCode",
            TextAttributes().apply {
                foregroundColor = color
            }
        )
    }

    /**
     * 获取指定颜色代码的TextAttributesKey
     */
    fun getColorAttribute(colorCode: Char): TextAttributesKey? {
        return COLOR_ATTRIBUTES[colorCode.lowercase().first()]
    }

    /**
     * 根据十六进制颜色创建TextAttributesKey
     */
    fun createHexColorAttribute(hexColor: String): TextAttributesKey {
        val color = try {
            Color.decode(if (hexColor.startsWith("#")) hexColor else "#$hexColor")
        } catch (e: Exception) {
            Color.WHITE
        }
        
        return TextAttributesKey.createTextAttributesKey(
            "TABOOLIB_LANG_HEX_${hexColor.uppercase()}",
            TextAttributes().apply {
                foregroundColor = color
            }
        )
    }

    /**
     * 从翻译文本中提取第一个颜色并返回对应的TextAttributesKey
     */
    fun extractColorAttribute(text: String): TextAttributesKey? {
        // 检查十六进制颜色 &#rrggbb
        val hexPattern = Regex("&#([0-9a-fA-F]{6})")
        hexPattern.find(text)?.let { matchResult ->
            return createHexColorAttribute(matchResult.groupValues[1])
        }
        
        // 检查RGB颜色代码 §{#rrggbb}
        val rgbPattern = Regex("[§&]\\{#([0-9a-fA-F]{6})}")
        rgbPattern.find(text)?.let { matchResult ->
            return createHexColorAttribute(matchResult.groupValues[1])
        }
        
        // 检查标准颜色代码 §x 或 &x
        val colorPattern = Regex("[§&]([0-9a-fk-or])")
        colorPattern.find(text)?.let { matchResult ->
            val colorCode = matchResult.groupValues[1].lowercase().first()
            if (colorCode in MC_COLORS) {
                return getColorAttribute(colorCode)
            }
        }
        
        return null
    }

    /**
     * 获取所有颜色属性键（用于注册到颜色设置）
     */
    fun getAllColorAttributes(): Collection<TextAttributesKey> {
        return COLOR_ATTRIBUTES.values
    }
}
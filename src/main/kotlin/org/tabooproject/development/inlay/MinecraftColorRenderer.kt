package org.tabooproject.development.inlay

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.Gray
import java.awt.Color
import java.awt.Font
import java.util.regex.Pattern

/**
 * Minecraft 颜色渲染器
 * 
 * 将 Minecraft 颜色代码转换为真正的颜色显示
 * 
 * @since 1.32
 */
object MinecraftColorRenderer {
    
    /**
     * 标准 Minecraft 颜色映射
     */
    private val MINECRAFT_COLORS = mapOf(
        '0' to Gray._0,           // 黑色
        '1' to Color(0, 0, 170),         // 深蓝色
        '2' to Color(0, 170, 0),         // 深绿色
        '3' to Color(0, 170, 170),       // 深青色
        '4' to Color(170, 0, 0),         // 深红色
        '5' to Color(170, 0, 170),       // 紫色
        '6' to Color(255, 170, 0),       // 金色
        '7' to Gray._170,     // 灰色
        '8' to Gray._85,        // 深灰色
        '9' to Color(85, 85, 255),       // 蓝色
        'a' to Color(85, 255, 85),       // 绿色
        'b' to Color(85, 255, 255),      // 青色
        'c' to Color(255, 85, 85),       // 红色
        'd' to Color(255, 85, 255),      // 粉色
        'e' to Color(255, 255, 85),      // 黄色
        'f' to Gray._255      // 白色
    )
    
    /**
     * 格式代码映射
     */
    private val FORMAT_CODES = setOf('k', 'l', 'm', 'n', 'o', 'r')
    
    /**
     * 检查文本是否包含颜色代码
     */
    fun hasColorCodes(text: String): Boolean {
        return MinecraftColorProcessor.hasColorCodes(text)
    }
    
    /**
     * 创建带颜色的文本展示
     * 
     * @param text 原始文本
     * @param factory 展示工厂
     * @return 带颜色的文本展示
     */
    fun createColoredPresentation(text: String, factory: PresentationFactory): InlayPresentation {
        val segments = parseColorSegments(text)

        if (segments.size == 1 && segments[0].color == null) {
            // 没有颜色代码，返回普通文本
            return factory.smallText(text)
        }

        // 创建带颜色的文本展示
        val textBuilder = StringBuilder()

        for (segment in segments) {
            if (segment.color != null) {
                // 添加颜色指示符
                val colorIndicator = getColorIndicator(segment.color)
                textBuilder.append(colorIndicator)
            }
            textBuilder.append(segment.text)
        }

        return factory.smallText(textBuilder.toString())
    }

    /**
     * 解析颜色段落
     * 
     * @param text 原始文本
     * @return 颜色段落列表
     */
    private fun parseColorSegments(text: String): List<ColorSegment> {
        val segments = mutableListOf<ColorSegment>()
        val currentText = StringBuilder()
        var currentColor: Color? = null
        var bold = false
        var italic = false
        var underline = false
        var strikethrough = false
        
        var i = 0
        while (i < text.length) {
            val char = text[i]
            
            if (char == '&' || char == '§') {
                if (i + 1 < text.length) {
                    val nextChar = text[i + 1].lowercaseChar()
                    
                    when {
                        // RGB 颜色格式 &{#rrggbb}
                        nextChar == '{' && i + 9 < text.length && text[i + 2] == '#' && text[i + 9] == '}' -> {
                            // 保存当前段落
                            if (currentText.isNotEmpty()) {
                                segments.add(ColorSegment(
                                    currentText.toString(),
                                    currentColor,
                                    bold,
                                    italic,
                                    underline,
                                    strikethrough
                                ))
                                currentText.clear()
                            }
                            
                            // 解析 RGB 颜色
                            val hexColor = text.substring(i + 3, i + 9)
                            currentColor = parseHexColor(hexColor)
                            
                            i += 10 // 跳过整个 RGB 代码（&{#rrggbb}共10个字符）
                            continue
                        }
                        
                        // 十六进制颜色格式 &#rrggbb
                        nextChar == '#' && i + 7 < text.length -> {
                            // 保存当前段落
                            if (currentText.isNotEmpty()) {
                                segments.add(ColorSegment(
                                    currentText.toString(),
                                    currentColor,
                                    bold,
                                    italic,
                                    underline,
                                    strikethrough
                                ))
                                currentText.clear()
                            }
                            
                            // 解析十六进制颜色
                            val hexColor = text.substring(i + 2, i + 8)
                            currentColor = parseHexColor(hexColor)
                            
                            i += 8 // 跳过整个十六进制代码
                            continue
                        }
                        
                        // 标准颜色代码
                        nextChar in MINECRAFT_COLORS.keys || nextChar in FORMAT_CODES -> {
                            // 保存当前段落
                            if (currentText.isNotEmpty()) {
                                segments.add(ColorSegment(
                                    currentText.toString(),
                                    currentColor,
                                    bold,
                                    italic,
                                    underline,
                                    strikethrough
                                ))
                                currentText.clear()
                            }
                            
                            when (nextChar) {
                                'r' -> {
                                    // 重置所有格式
                                    currentColor = null
                                    bold = false
                                    italic = false
                                    underline = false
                                    strikethrough = false
                                }
                                'l' -> bold = true
                                'o' -> italic = true
                                'n' -> underline = true
                                'm' -> strikethrough = true
                                'k' -> {
                                    // 随机字符效果，这里用特殊颜色表示
                                    currentColor = Gray._128
                                }
                                else -> {
                                    // 颜色代码
                                    currentColor = MINECRAFT_COLORS[nextChar]
                                }
                            }
                            
                            i += 2 // 跳过颜色代码
                            continue
                        }
                    }
                }
            }
            
            currentText.append(char)
            i++
        }
        
        // 添加最后一个段落
        if (currentText.isNotEmpty()) {
            segments.add(ColorSegment(
                currentText.toString(),
                currentColor,
                bold,
                italic,
                underline,
                strikethrough
            ))
        }
        
        return segments
    }
    
    /**
     * 解析十六进制颜色
     * 
     * @param hex 十六进制颜色字符串
     * @return Color 对象
     */
    private fun parseHexColor(hex: String): Color? {
        return try {
            Color.decode("#$hex")
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * 获取颜色指示符
     * 
     * @param color 颜色
     * @return 颜色指示符字符串
     */
    private fun getColorIndicator(color: Color): String {
        return when (color) {
            MINECRAFT_COLORS['0'] -> "⚫" // 黑色
            MINECRAFT_COLORS['1'] -> "🔵" // 深蓝色
            MINECRAFT_COLORS['2'] -> "🟢" // 深绿色
            MINECRAFT_COLORS['3'] -> "🔷" // 深青色
            MINECRAFT_COLORS['4'] -> "🔴" // 深红色
            MINECRAFT_COLORS['5'] -> "🟣" // 紫色
            MINECRAFT_COLORS['6'] -> "🟡" // 金色
            MINECRAFT_COLORS['7'] -> "⚪" // 灰色
            MINECRAFT_COLORS['8'] -> "⬛" // 深灰色
            MINECRAFT_COLORS['9'] -> "🔷" // 蓝色
            MINECRAFT_COLORS['a'] -> "🟢" // 绿色
            MINECRAFT_COLORS['b'] -> "🔷" // 青色
            MINECRAFT_COLORS['c'] -> "🔴" // 红色
            MINECRAFT_COLORS['d'] -> "🩷" // 粉色
            MINECRAFT_COLORS['e'] -> "🟡" // 黄色
            MINECRAFT_COLORS['f'] -> "⚪" // 白色
            else -> "🎨" // RGB 或其他颜色
        }
    }
    
    /**
     * 获取颜色名称
     * 
     * @param color 颜色
     * @return 颜色名称
     */
    private fun getColorName(color: Color): String {
        return when (color) {
            MINECRAFT_COLORS['0'] -> "黑色"
            MINECRAFT_COLORS['1'] -> "深蓝色"
            MINECRAFT_COLORS['2'] -> "深绿色"
            MINECRAFT_COLORS['3'] -> "深青色"
            MINECRAFT_COLORS['4'] -> "深红色"
            MINECRAFT_COLORS['5'] -> "紫色"
            MINECRAFT_COLORS['6'] -> "金色"
            MINECRAFT_COLORS['7'] -> "灰色"
            MINECRAFT_COLORS['8'] -> "深灰色"
            MINECRAFT_COLORS['9'] -> "蓝色"
            MINECRAFT_COLORS['a'] -> "绿色"
            MINECRAFT_COLORS['b'] -> "青色"
            MINECRAFT_COLORS['c'] -> "红色"
            MINECRAFT_COLORS['d'] -> "粉色"
            MINECRAFT_COLORS['e'] -> "黄色"
            MINECRAFT_COLORS['f'] -> "白色"
            else -> "RGB(${color.red}, ${color.green}, ${color.blue})"
        }
    }
    
    /**
     * 颜色段落数据类
     * 
     * @property text 文本内容
     * @property color 颜色
     * @property bold 是否粗体
     * @property italic 是否斜体
     * @property underline 是否下划线
     * @property strikethrough 是否删除线
     */
    private data class ColorSegment(
        val text: String,
        val color: Color?,
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean,
        val strikethrough: Boolean
    )
}
package org.tabooproject.development.inlay

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

/**
 * TabooLib语言文件颜色设置页面
 * 
 * 在IntelliJ的颜色设置中注册TabooLib语言颜色
 * 
 * @since 1.42
 */
class LangColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon? = null

    override fun getHighlighter(): SyntaxHighlighter = PlainSyntaxHighlighter()

    override fun getDemoText(): String {
        return """
            // TabooLib语言文件颜色示例
            player.sendLang("red-text")      // 红色文本
            player.sendLang("green-text")    // 绿色文本
            player.sendLang("blue-text")     // 蓝色文本
            player.sendLang("yellow-text")   // 黄色文本
            player.sendLang("rainbow-text")  // 彩虹文本
            player.sendLang("hex-color")     // 十六进制颜色
            player.sendLang("rgb-color")     // RGB颜色
        """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        val attributes = mutableListOf<AttributesDescriptor>()
        
        // 添加标准Minecraft颜色
        val colorNames = mapOf(
            '0' to "Black",
            '1' to "Dark Blue", 
            '2' to "Dark Green",
            '3' to "Dark Aqua",
            '4' to "Dark Red",
            '5' to "Dark Purple",
            '6' to "Gold",
            '7' to "Gray",
            '8' to "Dark Gray",
            '9' to "Blue",
            'a' to "Green",
            'b' to "Aqua", 
            'c' to "Red",
            'd' to "Light Purple",
            'e' to "Yellow",
            'f' to "White"
        )
        
        colorNames.forEach { (code, name) ->
            LangColorAttributes.getColorAttribute(code)?.let { attributeKey ->
                attributes.add(AttributesDescriptor("TabooLib Lang: $name", attributeKey))
            }
        }
        
        return attributes.toTypedArray()
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "TabooLib"
}
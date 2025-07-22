package org.tabooproject.development.inlay

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.tabooproject.development.isSendLangCall
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

/**
 * Minecraft语言文件边栏颜色块提供器
 *
 * 在代码行边缘显示实际的颜色色块，直观显示文本颜色
 *
 * @since 1.42
 */
class MinecraftLangGutterProvider : LineMarkerProvider {

    companion object {
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
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // 只处理方法调用表达式
        if (element !is KtCallExpression) {
            return null
        }

        // 检查是否是sendLang调用
        if (!isSendLangCall(element)) {
            return null
        }

        // 获取第一个参数
        val arguments = element.valueArgumentList?.arguments ?: return null
        if (arguments.isEmpty()) return null

        val firstArg = arguments[0].getArgumentExpression()
        if (firstArg !is KtStringTemplateExpression) return null

        // 提取语言键
        val langKey = extractStringLiteral(firstArg) ?: return null

        // 查找翻译
        val project = element.project
        val translation = findTranslationInProject(project, langKey) ?: return null

        // 提取所有颜色并创建色块图标
        val colors = extractColors(translation)
        if (colors.isEmpty()) return null

        val colorIcon = createColorBlockIcon(colors)
        val tooltip = createTooltipText(translation, colors)

        return LineMarkerInfo(
            element,
            element.textRange,
            colorIcon,
            { tooltip },
            null,
            GutterIconRenderer.Alignment.LEFT
        )
    }

    /**
     * 提取翻译文本中的所有颜色
     */
    private fun extractColors(text: String): List<Color> {
        val colors = mutableListOf<Color>()
        
        // 查找标准颜色代码 §x 或 &x
        val colorPattern = Regex("[§&]([0-9a-fk-or])")
        colorPattern.findAll(text).forEach { matchResult ->
            val colorCode = matchResult.groupValues[1].lowercase().first()
            MC_COLORS[colorCode]?.let { color ->
                if (!colors.contains(color)) {
                    colors.add(color)
                }
            }
        }
        
        // 查找十六进制颜色 &#rrggbb
        val hexPattern = Regex("&#([0-9a-fA-F]{6})")
        hexPattern.findAll(text).forEach { matchResult ->
            try {
                val color = Color.decode("#" + matchResult.groupValues[1])
                if (!colors.contains(color)) {
                    colors.add(color)
                }
            } catch (e: Exception) {
                // 忽略无效的颜色代码
            }
        }
        
        // 查找RGB颜色代码 §{#rrggbb}
        val rgbPattern = Regex("[§&]\\{#([0-9a-fA-F]{6})}")
        rgbPattern.findAll(text).forEach { matchResult ->
            try {
                val color = Color.decode("#" + matchResult.groupValues[1])
                if (!colors.contains(color)) {
                    colors.add(color)
                }
            } catch (e: Exception) {
                // 忽略无效的颜色代码
            }
        }
        
        return colors
    }

    /**
     * 创建颜色色块图标
     */
    private fun createColorBlockIcon(colors: List<Color>): Icon {
        val iconSize = 16
        val blockSize = if (colors.size <= 2) iconSize else iconSize / 2
        val image = BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        when (colors.size) {
            1 -> {
                // 单个颜色：填充整个图标
                g2d.color = colors[0]
                g2d.fillRect(0, 0, iconSize, iconSize)
                g2d.color = Color.GRAY
                g2d.drawRect(0, 0, iconSize - 1, iconSize - 1)
            }
            2 -> {
                // 两个颜色：垂直分割
                g2d.color = colors[0]
                g2d.fillRect(0, 0, iconSize / 2, iconSize)
                g2d.color = colors[1]
                g2d.fillRect(iconSize / 2, 0, iconSize / 2, iconSize)
                g2d.color = Color.GRAY
                g2d.drawRect(0, 0, iconSize - 1, iconSize - 1)
                g2d.drawLine(iconSize / 2, 0, iconSize / 2, iconSize - 1)
            }
            3 -> {
                // 三个颜色：L型布局
                g2d.color = colors[0]
                g2d.fillRect(0, 0, blockSize, blockSize)
                g2d.color = colors[1]
                g2d.fillRect(blockSize, 0, blockSize, blockSize)
                g2d.color = colors[2]
                g2d.fillRect(0, blockSize, iconSize, blockSize)
                g2d.color = Color.GRAY
                g2d.drawRect(0, 0, iconSize - 1, iconSize - 1)
                g2d.drawLine(blockSize, 0, blockSize, blockSize - 1)
                g2d.drawLine(0, blockSize, iconSize - 1, blockSize)
            }
            else -> {
                // 4个或更多颜色：2x2网格，最多显示4个
                val displayColors = colors.take(4)
                for (i in displayColors.indices) {
                    val x = (i % 2) * blockSize
                    val y = (i / 2) * blockSize
                    g2d.color = displayColors[i]
                    g2d.fillRect(x, y, blockSize, blockSize)
                }
                g2d.color = Color.GRAY
                g2d.drawRect(0, 0, iconSize - 1, iconSize - 1)
                g2d.drawLine(blockSize, 0, blockSize, iconSize - 1)
                g2d.drawLine(0, blockSize, iconSize - 1, blockSize)
            }
        }
        
        g2d.dispose()
        return ImageIcon(image)
    }

    /**
     * 创建工具提示文本
     */
    private fun createTooltipText(translation: String, colors: List<Color>): String {
        val colorInfo = colors.joinToString(", ") { color ->
            "RGB(${color.red}, ${color.green}, ${color.blue})"
        }
        return "翻译：${stripColorCodes(translation)}\n颜色：$colorInfo"
    }

    /**
     * 提取字符串字面量
     */
    private fun extractStringLiteral(stringTemplate: KtStringTemplateExpression): String? {
        // 处理完整的字符串内容
        val fullText = stringTemplate.text

        // 处理带引号的情况 (如 "key")
        if ((fullText.startsWith("\"") && fullText.endsWith("\"")) ||
            (fullText.startsWith("'") && fullText.endsWith("'"))) {
            return fullText.substring(1, fullText.length - 1)
        }

        // 使用entries提取内容
        if (stringTemplate.entries.size == 1) {
            val entry = stringTemplate.entries[0]
            if (entry is org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry) {
                return entry.text
            }
        }

        return null
    }

    /**
     * 在项目中查找翻译文本
     */
    private fun findTranslationInProject(project: Project, langKey: String): String? {
        // 使用与LangFoldingBuilder相同的测试数据
        val testTranslations = mapOf(
            "test-key" to "这是测试文本",
            "player.join" to "玩家 {0} 加入了游戏", 
            "player.quit" to "玩家 {0} 离开了游戏",
            
            // 单颜色示例
            "red-text" to "&c这是红色文本",
            "green-text" to "&a这是绿色文本",
            "blue-text" to "&9这是蓝色文本",
            "yellow-text" to "&e这是黄色文本",
            
            // 多颜色示例
            "test-color" to "&4红色&a绿色&b蓝色文本",
            "rainbow-text" to "&4红&6橙&e黄&a绿&3青&2蓝&5紫",
            "two-color" to "&c红色&9蓝色混合",
            "three-color" to "&e黄色&a绿色&5紫色文字",
            "complex-color" to "&4警告：&e这是一条&a重要&c的消息",
            
            // 自定义颜色示例
            "hex-color" to "&#FF5733这是十六进制颜色",
            "rgb-color" to "&{#00FF00}这是RGB颜色",
            "mixed-color" to "&c红色&#FFD700金色&9蓝色",
            
            // 超多颜色示例（测试彩虹显示）
            "super-rainbow" to "&4红&c亮红&6橙&e黄&2深绿&a绿&b青&3深青&1深蓝&9蓝&5深紫&d紫",
            
            // 格式代码混合
            "formatted-text" to "&l&4粗体红色&r&o&a斜体绿色"
        )
        
        return testTranslations[langKey]
    }

    /**
     * 移除字符串中的颜色代码
     */
    private fun stripColorCodes(text: String): String {
        // 移除标准颜色代码 §x 和 &x
        var result = text.replace(Regex("[§&][0-9a-fk-or]"), "")
        
        // 移除RGB颜色代码 §{#rrggbb} 和 &{#rrggbb}
        result = result.replace(Regex("[§&]\\{#[0-9a-fA-F]{6}}"), "")
        
        // 移除十六进制颜色代码 &#rrggbb
        result = result.replace(Regex("&#[0-9a-fA-F]{6}"), "")
        
        return result
    }
}

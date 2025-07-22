package org.tabooproject.development.inlay

import java.util.regex.Pattern

/**
 * Minecraft 颜色代码处理器
 * 
 * 支持处理 Minecraft 的标准颜色代码和 TabooLib 的扩展 RGB 颜色格式
 * 
 * @since 1.32
 */
object MinecraftColorProcessor {
    
    /**
     * 标准 Minecraft 颜色代码（&和§）
     */
    private val STANDARD_COLOR_PATTERN = Pattern.compile("[&§][0-9a-fk-or]", Pattern.CASE_INSENSITIVE)
    
    /**
     * TabooLib RGB 颜色格式 &{#rrggbb}
     */
    private val RGB_COLOR_PATTERN = Pattern.compile("&\\{#([0-9a-fA-F]{6})\\}")
    
    /**
     * 十六进制颜色代码 &#rrggbb
     */
    private val HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})")
    
    /**
     * 颜色代码到描述的映射
     */
    private val COLOR_DESCRIPTIONS = mapOf(
        '0' to "黑色",
        '1' to "深蓝色",
        '2' to "深绿色", 
        '3' to "深青色",
        '4' to "深红色",
        '5' to "紫色",
        '6' to "金色",
        '7' to "灰色",
        '8' to "深灰色",
        '9' to "蓝色",
        'a' to "绿色",
        'b' to "青色",
        'c' to "红色",
        'd' to "粉色",
        'e' to "黄色",
        'f' to "白色",
        'k' to "随机字符",
        'l' to "粗体",
        'm' to "删除线",
        'n' to "下划线",
        'o' to "斜体",
        'r' to "重置"
    )
    
    /**
     * 处理文本中的颜色代码
     * 
     * @param text 原始文本
     * @param mode 处理模式
     * @return 处理后的文本
     */
    fun processColorCodes(text: String, mode: ColorProcessMode = ColorProcessMode.STRIP): String {
        return when (mode) {
            ColorProcessMode.STRIP -> stripColorCodes(text)
            ColorProcessMode.DESCRIBE -> describeColorCodes(text)
            ColorProcessMode.PREVIEW -> createPreviewText(text)
        }
    }
    
    /**
     * 移除所有颜色代码，保留纯文本
     * 
     * @param text 原始文本
     * @return 移除颜色代码后的纯文本
     */
    private fun stripColorCodes(text: String): String {
        return text
            .let { RGB_COLOR_PATTERN.matcher(it).replaceAll("") }
            .let { HEX_COLOR_PATTERN.matcher(it).replaceAll("") }
            .let { STANDARD_COLOR_PATTERN.matcher(it).replaceAll("") }
    }
    
    /**
     * 将颜色代码转换为描述性文本
     * 
     * @param text 原始文本
     * @return 包含颜色描述的文本
     */
    private fun describeColorCodes(text: String): String {
        var result = text
        
        // 处理 RGB 颜色
        result = RGB_COLOR_PATTERN.matcher(result).replaceAll { matchResult ->
            val colorCode = matchResult.group(1)
            "[RGB:#$colorCode]"
        }
        
        // 处理十六进制颜色
        result = HEX_COLOR_PATTERN.matcher(result).replaceAll { matchResult ->
            val colorCode = matchResult.group(1)
            "[颜色:#$colorCode]"
        }
        
        // 处理标准颜色代码
        result = STANDARD_COLOR_PATTERN.matcher(result).replaceAll { matchResult ->
            val fullMatch = matchResult.group()
            val colorChar = fullMatch.last().lowercaseChar()
            val description = COLOR_DESCRIPTIONS[colorChar] ?: "未知"
            "[$description]"
        }
        
        return result
    }
    
    /**
     * 创建预览格式的文本（简化显示）
     * 
     * @param text 原始文本
     * @return 适合预览的文本
     */
    private fun createPreviewText(text: String): String {
        var result = text
        
        // 简化 RGB 颜色显示
        result = RGB_COLOR_PATTERN.matcher(result).replaceAll { matchResult ->
            val colorCode = matchResult.group(1)
            "⬢" // 使用彩色方块符号表示颜色
        }
        
        // 简化十六进制颜色显示
        result = HEX_COLOR_PATTERN.matcher(result).replaceAll("⬢")
        
        // 简化标准颜色代码
        result = STANDARD_COLOR_PATTERN.matcher(result).replaceAll { matchResult ->
            val colorChar = matchResult.group().last().lowercaseChar()
            when (colorChar) {
                in '0'..'9', in 'a'..'f' -> "⬢" // 颜色
                'k' -> "✦" // 随机字符
                'l' -> "𝐁" // 粗体
                'm' -> "s̶" // 删除线
                'n' -> "u̲" // 下划线
                'o' -> "𝐼" // 斜体
                'r' -> "↺" // 重置
                else -> ""
            }
        }
        
        return result
    }
    
    /**
     * 检查文本是否包含颜色代码
     * 
     * @param text 要检查的文本
     * @return 如果包含颜色代码返回 true
     */
    fun hasColorCodes(text: String): Boolean {
        return STANDARD_COLOR_PATTERN.matcher(text).find() ||
               RGB_COLOR_PATTERN.matcher(text).find() ||
               HEX_COLOR_PATTERN.matcher(text).find()
    }
    
    /**
     * 获取文本中的颜色代码统计
     * 
     * @param text 要分析的文本
     * @return 颜色代码统计信息
     */
    fun getColorCodeStats(text: String): ColorCodeStats {
        val standardMatches = STANDARD_COLOR_PATTERN.matcher(text)
        val rgbMatches = RGB_COLOR_PATTERN.matcher(text)
        val hexMatches = HEX_COLOR_PATTERN.matcher(text)
        
        var standardCount = 0
        var rgbCount = 0
        var hexCount = 0
        
        while (standardMatches.find()) standardCount++
        while (rgbMatches.find()) rgbCount++
        while (hexMatches.find()) hexCount++
        
        return ColorCodeStats(standardCount, rgbCount, hexCount)
    }
}

/**
 * 颜色代码处理模式
 */
enum class ColorProcessMode {
    /** 移除所有颜色代码 */
    STRIP,
    /** 将颜色代码转换为描述 */
    DESCRIBE,
    /** 创建预览格式 */
    PREVIEW
}

/**
 * 颜色代码统计信息
 * 
 * @property standardCodes 标准颜色代码数量（&和§）
 * @property rgbCodes RGB颜色代码数量
 * @property hexCodes 十六进制颜色代码数量
 */
data class ColorCodeStats(
    val standardCodes: Int,
    val rgbCodes: Int,
    val hexCodes: Int
) {
    val totalCodes: Int get() = standardCodes + rgbCodes + hexCodes
    val hasColors: Boolean get() = totalCodes > 0
} 
package org.tabooproject.development.inlay

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange

/**
 * 支持颜色显示的折叠描述符
 * 
 * 扩展标准的FoldingDescriptor来支持Minecraft颜色代码渲染
 * 
 * @since 1.42
 */
class ColoredFoldingDescriptor(
    node: ASTNode,
    range: TextRange,
    group: FoldingGroup?,
    private val originalText: String,
    placeholderText: String
) : FoldingDescriptor(node, range, group, placeholderText) {

    /**
     * 获取此折叠区域的颜色属性
     */
    fun getColorAttribute(): TextAttributesKey? {
        return LangColorAttributes.extractColorAttribute(originalText)
    }

    /**
     * 检查是否包含颜色代码
     */
    fun hasColorCodes(): Boolean {
        return getColorAttribute() != null
    }
}
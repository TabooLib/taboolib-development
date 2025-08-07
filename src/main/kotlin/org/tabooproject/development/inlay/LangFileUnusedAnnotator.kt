package org.tabooproject.development.inlay

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import org.yaml.snakeyaml.Yaml
import java.awt.Color
import java.awt.Font

/**
 * TabooLib 语言文件未使用键注解器
 * 
 * 为语言文件中未使用的键添加灰色显示
 * 
 * @since 1.42
 */
class LangFileUnusedAnnotator : Annotator {

    companion object {
        // 定义未使用语言键的高亮样式
        val UNUSED_LANG_KEY_ATTRIBUTES = TextAttributesKey.createTextAttributesKey(
            "TABOOLIB_UNUSED_LANG_KEY",
            TextAttributes().apply {
                foregroundColor = Color.GRAY
                fontType = Font.ITALIC
            }
        )
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile?.virtualFile ?: return
        
        // 只处理语言文件
        if (!LangFiles.isLangFile(file)) return
        
        // 检查是否是YAML键值对的键部分
        val langKey = extractLangKeyFromElement(element) ?: return
        
        // 检查这个语言键是否被使用
        val project = element.project
        val isUsed = LangUsageAnalyzer.isLangKeyUsed(project, langKey)
        
        if (!isUsed) {
            // 为未使用的语言键添加灰色高亮
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.textRange)
                .textAttributes(UNUSED_LANG_KEY_ATTRIBUTES)
                .tooltip("语言键 '$langKey' 未在项目中使用")
                .create()
        }
    }

    /**
     * 从PSI元素中提取语言键
     * 这是一个简化的实现，实际需要根据YAML文件结构来解析
     */
    private fun extractLangKeyFromElement(element: PsiElement): String? {
        val text = element.text.trim()
        
        // 简单的YAML键值对匹配
        if (text.contains(":")) {
            val key = text.substringBefore(":").trim()
            if (key.isNotEmpty() && !key.startsWith("#")) {
                return key
            }
        }
        
        return null
    }
}
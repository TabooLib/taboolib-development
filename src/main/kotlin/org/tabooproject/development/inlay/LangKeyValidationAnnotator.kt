package org.tabooproject.development.inlay

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.tabooproject.development.isSendLangCall
import java.awt.Color
import java.awt.Font

/**
 * TabooLib语言键验证注解器
 * 
 * 实时验证语言键是否存在，为缺失的语言键提供视觉提示
 * 
 * @since 1.42
 */
class LangKeyValidationAnnotator : Annotator {

    companion object {
        // 定义缺失语言键的高亮样式
        val MISSING_LANG_KEY_ATTRIBUTES = TextAttributesKey.createTextAttributesKey(
            "TABOOLIB_MISSING_LANG_KEY",
            TextAttributes().apply {
                backgroundColor = Color(255, 235, 235) // 浅红色背景
                effectType = com.intellij.openapi.editor.markup.EffectType.WAVE_UNDERSCORE
                effectColor = Color.RED
                fontType = Font.ITALIC
            }
        )

        // 定义存在语言键的高亮样式
        val VALID_LANG_KEY_ATTRIBUTES = TextAttributesKey.createTextAttributesKey(
            "TABOOLIB_VALID_LANG_KEY",
            TextAttributes().apply {
                backgroundColor = Color(235, 255, 235) // 浅绿色背景
                effectType = com.intellij.openapi.editor.markup.EffectType.BOXED
                effectColor = Color(0, 150, 0)
            }
        )
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // 只处理字符串模板表达式
        if (element !is KtStringTemplateExpression) return
        
        // 检查是否在sendLang或asLangText方法调用中
        val parent = element.parent
        val grandParent = parent?.parent
        
        if (grandParent !is KtCallExpression) return
        if (!isSendLangCall(grandParent)) return
        
        // 获取第一个参数(语言键)
        val args = grandParent.valueArgumentList?.arguments
        if (args.isNullOrEmpty() || args[0].getArgumentExpression() != element) return
        
        // 提取语言键
        val langKey = extractStringLiteral(element) ?: return
        
        // 检查语言键是否存在
        val project = element.project
        val translation = LangIndex.getTranslation(project, langKey)
        
        if (translation == null) {
            // 语言键不存在，添加错误高亮
            holder.newAnnotation(HighlightSeverity.WARNING, "语言键 '$langKey' 未在语言文件中定义")
                .range(element.textRange)
                .textAttributes(MISSING_LANG_KEY_ATTRIBUTES)
                .tooltip("语言键 '$langKey' 未在默认语言文件中找到。点击检查器中的快速修复可以自动添加。")
                .create()
        } else {
            // 语言键存在，添加成功高亮（可选）
            if (LangFoldingSettings.instance.showValidLangKeyHighlight) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.textRange)
                    .textAttributes(VALID_LANG_KEY_ATTRIBUTES)
                    .tooltip("语言键 '$langKey': $translation")
                    .create()
            }
        }
    }

    /**
     * 提取字符串字面量
     */
    private fun extractStringLiteral(stringTemplate: KtStringTemplateExpression): String? {
        val fullText = stringTemplate.text
        
        // 处理带引号的情况
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
}
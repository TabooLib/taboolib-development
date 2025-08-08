package org.tabooproject.development.inlay

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * TabooLib语言文件折叠颜色注解器
 * 
 * 为sendLang调用的折叠区域添加颜色高亮
 * 
 * @since 1.42
 */
class LangFoldingColorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // 只处理字符串模板表达式
        if (element !is KtStringTemplateExpression) return
        
        // 检查是否在sendLang方法调用中
        val parent = element.parent
        val grandParent = parent?.parent
        
        if (grandParent !is KtCallExpression) return
        if (!isSendLangCall(grandParent)) return
        
        // 获取第一个参数(语言键)
        val args = grandParent.valueArgumentList?.arguments
        if (args.isNullOrEmpty() || args[0].getArgumentExpression() != element) return
        
        // 提取语言键
        val langKey = extractStringLiteral(element) ?: return
        
        // 查找翻译
        val project = element.project
        val translation = findTranslationInProject(project, langKey) ?: return
        
        // 获取颜色属性
        val colorAttribute = LangColorAttributes.extractColorAttribute(translation)
        if (colorAttribute != null) {
            // 为整个字符串元素添加颜色高亮
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.textRange)
                .textAttributes(colorAttribute)
                .create()
        }
    }

    /**
     * 判断是否是sendLang方法调用
     */
    private fun isSendLangCall(callExpression: KtCallExpression): Boolean {
        val calleeText = callExpression.calleeExpression?.text ?: return false
        
        // 直接调用sendLang
        if (calleeText == "sendLang") {
            return true
        }
        
        // 处理 player.sendLang 这种模式
        if (calleeText.endsWith(".sendLang")) {
            return true
        }
        
        // 处理其它可能的sendLang调用方式
        val dotIndex = calleeText.lastIndexOf('.')
        if (dotIndex > 0 && calleeText.substring(dotIndex + 1) == "sendLang") {
            return true
        }
        
        return false
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
    private fun findTranslationInProject(project: com.intellij.openapi.project.Project, langKey: String): String? {
        // 使用与LangFoldingBuilder相同的测试数据
        val testTranslations = mapOf(
            "test-key" to "这是测试文本",
            "player.join" to "玩家 {0} 加入了游戏", 
            "player.quit" to "玩家 {0} 离开了游戏",
            "test-color" to "&4红色&a绿色&b蓝色文本",
            "red-text" to "&c这是红色文本",
            "green-text" to "&a这是绿色文本",
            "blue-text" to "&9这是蓝色文本",
            "yellow-text" to "&e这是黄色文本",
            "rainbow-text" to "&4红&6橙&e黄&a绿&3青&2蓝&5紫",
            "hex-color" to "&#FF5733这是十六进制颜色",
            "rgb-color" to "&{#00FF00}这是RGB颜色"
        )
        
        return testTranslations[langKey]
    }
}
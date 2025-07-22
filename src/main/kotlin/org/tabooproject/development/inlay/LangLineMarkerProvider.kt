package org.tabooproject.development.inlay

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.tabooproject.development.isSendLangCall
import javax.swing.Icon

/**
 * TabooLib 语言文件行标记提供器
 * 
 * 在使用语言键的地方添加导航到语言文件的图标
 * 
 * @since 1.42
 */
class LangLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // 检查是否是sendLang方法调用
        if (element !is KtCallExpression) return
        if (!isSendLangCall(element)) return
        
        // 获取第一个参数，即语言键
        val arguments = element.valueArgumentList?.arguments
        if (arguments.isNullOrEmpty()) return
        
        val firstArg = arguments[0].getArgumentExpression()
        if (firstArg !is KtStringTemplateExpression) return
        
        // 提取语言键
        val langKey = extractStringLiteral(firstArg) ?: return
        
        // 查找对应的语言文件
        val project = element.project
        val langFiles = LangIndex.findLangFiles(element)
        if (langFiles.isEmpty()) return
        
        val defaultLangFile = langFiles.find { LangFiles.isDefaultLocale(it) } ?: langFiles.first()
        
        // 获取PsiFile
        val psiManager = PsiManager.getInstance(project)
        val psiFile = psiManager.findFile(defaultLangFile) ?: return
        
        // 添加行标记
        val targetElement = firstArg
        val tooltip = "导航到语言文件中的 '$langKey'"
        
        val builder = NavigationGutterIconBuilder
            .create(LANG_ICON)
            .setTargets(psiFile)
            .setTooltipText(tooltip)
            .setPopupTitle("TabooLib 国际化")
            .setEmptyPopupText("无法找到对应的语言文件")
        
        result.add(builder.createLineMarkerInfo(targetElement))
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
    
    companion object {
        // 使用IntelliJ内置的国际化图标
        private val LANG_ICON: Icon = AllIcons.Nodes.ResourceBundle
    }
} 
package org.tabooproject.development.inlay

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.tabooproject.development.isSendLangCall
import javax.swing.Icon

/**
 * TabooLib 语言文件使用情况行标记提供器
 * 
 * 在语言文件中显示每个语言键的使用次数，并提供跳转到使用位置的功能
 * 
 * @since 1.42
 */
class LangFileUsageLineMarker : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val file = element.containingFile?.virtualFile ?: return
        
        // 只处理语言文件
        if (!LangFiles.isLangFile(file)) return
        
        // 提取语言键
        val langKey = extractLangKeyFromElement(element) ?: return
        
        // 查找使用位置
        val project = element.project
        val usages = LangUsageAnalyzer.findUsages(project, langKey)
        
        if (usages.isEmpty()) {
            // 未使用的键，显示警告图标
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.General.InspectionsEye)
                .setTargets(emptyList<PsiElement>())
                .setTooltipText("语言键 '$langKey' 未被使用")
                .setPopupTitle("未使用的语言键")
                .setEmptyPopupText("此语言键在项目中未被使用")
            
            result.add(builder.createLineMarkerInfo(element))
        } else {
            // 有使用的键，显示使用次数和跳转选项
            val psiManager = PsiManager.getInstance(project)
            val targets = mutableListOf<PsiElement>()
            
            usages.forEach { usage ->
                val psiFile = psiManager.findFile(usage.file)
                if (psiFile != null) {
                    val targetElement = findElementAtOffset(psiFile, usage.offset)
                    if (targetElement != null) {
                        targets.add(targetElement)
                    }
                }
            }
            
            if (targets.isNotEmpty()) {
                val usageCount = usages.size
                val tooltip = "语言键 '$langKey' 被使用了 $usageCount 次"
                
                val builder = NavigationGutterIconBuilder
                    .create(getUsageIcon(usageCount))
                    .setTargets(targets)
                    .setTooltipText(tooltip)
                    .setPopupTitle("语言键使用位置")
                    .setEmptyPopupText("无法找到使用位置")
                
                result.add(builder.createLineMarkerInfo(element))
            }
        }
    }

    /**
     * 从元素中提取语言键
     */
    private fun extractLangKeyFromElement(element: PsiElement): String? {
        val text = element.text.trim()
        
        // 简单的YAML键值对匹配
        if (text.contains(":")) {
            val key = text.substringBefore(":").trim()
            if (key.isNotEmpty() && !key.startsWith("#") && !key.startsWith("\"") && !key.startsWith("'")) {
                return key
            }
        }
        
        return null
    }

    /**
     * 根据使用次数获取相应图标
     */
    private fun getUsageIcon(usageCount: Int): Icon {
        return when {
            usageCount == 0 -> AllIcons.General.InspectionsEye
            usageCount == 1 -> AllIcons.Gutter.Unique
            usageCount <= 5 -> AllIcons.General.ArrowRight
            else -> AllIcons.General.BalloonInformation
        }
    }

    /**
     * 在指定偏移位置查找PSI元素
     */
    private fun findElementAtOffset(psiFile: com.intellij.psi.PsiFile, offset: Int): PsiElement? {
        val elementAtOffset = psiFile.findElementAt(offset) ?: return null
        
        // 查找包含的sendLang调用
        val callExpression = com.intellij.psi.util.PsiTreeUtil.getParentOfType(elementAtOffset, KtCallExpression::class.java)
        if (callExpression != null && isSendLangCall(callExpression)) {
            // 返回语言键参数的字符串模板
            val arguments = callExpression.valueArgumentList?.arguments
            if (!arguments.isNullOrEmpty()) {
                val firstArg = arguments[0].getArgumentExpression()
                if (firstArg is KtStringTemplateExpression) {
                    return firstArg
                }
            }
        }
        
        return elementAtOffset
    }
}
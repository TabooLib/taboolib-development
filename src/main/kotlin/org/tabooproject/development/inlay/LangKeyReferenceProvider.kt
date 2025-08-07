package org.tabooproject.development.inlay

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.tabooproject.development.isSendLangCall

/**
 * TabooLib 语言键引用提供器
 * 
 * 提供从语言文件到代码使用位置的引用跳转
 * 
 * @since 1.42
 */
class LangKeyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // 检查是否是语言文件中的键
        val file = element.containingFile?.virtualFile
        if (file == null || !LangFiles.isLangFile(file)) {
            return PsiReference.EMPTY_ARRAY
        }

        val langKey = extractLangKeyFromYamlElement(element) ?: return PsiReference.EMPTY_ARRAY
        
        return arrayOf(LangKeyReference(element, langKey))
    }

    /**
     * 从YAML元素中提取语言键
     */
    private fun extractLangKeyFromYamlElement(element: PsiElement): String? {
        val text = element.text.trim()
        
        // 简单的YAML键匹配
        if (text.contains(":")) {
            val key = text.substringBefore(":").trim()
            if (key.isNotEmpty() && !key.startsWith("#")) {
                return key
            }
        }
        
        return null
    }
}

/**
 * TabooLib 语言键引用
 */
class LangKeyReference(
    element: PsiElement,
    private val langKey: String
) : PsiReferenceBase<PsiElement>(element) {

    override fun resolve(): PsiElement? {
        // 查找使用这个语言键的第一个位置
        val usages = LangUsageAnalyzer.findUsages(element.project, langKey)
        if (usages.isEmpty()) return null

        val firstUsage = usages.first()
        val psiManager = PsiManager.getInstance(element.project)
        val psiFile = psiManager.findFile(firstUsage.file) ?: return null

        // 找到具体的PSI元素
        return findElementAtOffset(psiFile, firstUsage.offset)
    }

    override fun getVariants(): Array<Any> {
        // 返回所有可能的语言键
        val allLangs = LangIndex.getProjectDefaultLangs(element.project)
        return allLangs.map { it.key }.toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        // 处理重命名（暂时不实现）
        return element
    }

    /**
     * 在指定偏移位置查找PSI元素
     */
    private fun findElementAtOffset(psiFile: PsiFile, offset: Int): PsiElement? {
        val elementAtOffset = psiFile.findElementAt(offset) ?: return null
        
        // 查找包含的sendLang调用
        val callExpression = PsiTreeUtil.getParentOfType(elementAtOffset, KtCallExpression::class.java)
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

/**
 * TabooLib 语言键引用贡献器
 * 
 * 为语言文件中的键注册引用提供器
 */
class LangKeyReferenceContributor : PsiReferenceContributor() {
    
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // 为YAML文件中的所有元素注册引用提供器
        registrar.registerReferenceProvider(
            com.intellij.patterns.PlatformPatterns.psiElement(),
            LangKeyReferenceProvider()
        )
    }
}
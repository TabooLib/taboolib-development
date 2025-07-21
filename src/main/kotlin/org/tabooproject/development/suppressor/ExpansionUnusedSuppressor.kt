package org.tabooproject.development.suppressor

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeReference

private const val INSPECTION = "unused"

private val classes = listOf(
    "taboolib.platform.compat.PlaceholderExpansion",
    "taboolib.common.platform.Plugin",
)

/**
 * 抑制器，用于抑制继承自 TabooLib 特定基类的类的"未使用"警告
 *
 * @since 1.31
 */
class ExpansionUnusedSuppressor: InspectionSuppressor {
    
    /**
     * 检查是否应该抑制指定元素的检查
     *
     * @param element 要检查的 PSI 元素
     * @param toolId 检查工具 ID
     * @return 如果应该抑制检查返回 true
     */
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION) {
            return false
        }

        return classes.any { className ->
            checkIfClassImplementsOrExtends(element, className)
        }
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    /**
     * 检查元素所在的类是否实现或继承了指定的类名
     * 
     * 使用 K2 兼容的方式进行类型检查，避免使用过时的 analyze API
     *
     * @param element PSI 元素
     * @param className 要检查的类名
     * @return 如果类实现或继承了指定类名返回 true
     */
    private fun checkIfClassImplementsOrExtends(element: PsiElement, className: String): Boolean {
        val ktClass = PsiTreeUtil.getParentOfType(element, KtClassOrObject::class.java) ?: return false
        
        // K2 兼容的简化检查方式：基于文本匹配超类型列表
        // 这种方式虽然不如语义分析精确，但在 K2 环境下更稳定
        return ktClass.isSubclassOf(className) || ktClass.implementsInterface(className)
    }

    /**
     * 检查类是否是指定类的子类（基于文本匹配）
     */
    private fun KtClassOrObject.isSubclassOf(className: String): Boolean {
        // 检查自身的 FQ 名称
        if (fqName?.asString() == className) return true

        // 检查超类型列表中的类型引用
        val superTypes = this.superTypeListEntries
        return superTypes.any { typeEntry ->
            val typeReference = typeEntry.typeReference
            typeReference?.text?.let { typeText ->
                // 简化的文本匹配，支持常见的类型引用格式
                typeText == className.substringAfterLast(".") || 
                typeText.contains(className) ||
                typeText.endsWith(className.substringAfterLast("."))
            } == true
        }
    }

    /**
     * 检查类是否实现了指定的接口（基于文本匹配）
     */
    private fun KtClassOrObject.implementsInterface(interfaceName: String): Boolean {
        return isSubclassOf(interfaceName)
    }

    /**
     * 获取类型引用的简化名称
     */
    private fun KtTypeReference.getFqName(): String? {
        return text
    }
}
package org.tabooproject.development.completion

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * TabooLib 补全排序权重器
 *
 * 当补全候选中存在同名方法且都来自包含 "taboolib" 的包时，
 * 优先展示以 "taboolib." 开头的原始 API，降低被 shadow 重定位后的 API 优先级。
 */
class TabooLibCompletionWeigher : CompletionWeigher() {

    override fun weigh(element: LookupElement, location: CompletionLocation): Comparable<*> {
        val psi = element.psiElement ?: return 0

        val fqName = when (psi) {
            is PsiMember -> psi.containingClass?.qualifiedName
            is KtNamedDeclaration -> psi.fqName?.parent()?.asString()
            else -> null
        } ?: return 0

        if (!fqName.contains("taboolib")) return 0
        return if (fqName.startsWith("taboolib.")) 1 else -100
    }
}

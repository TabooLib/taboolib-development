package org.tabooproject.development.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.tabooproject.development.isReflectContext

/**
 * TabooLib 字面量补全置信度
 * 
 * 在反射上下文中允许字符串字面量的自动补全
 * 
 * @since 1.31
 */
class TabooLiteralConfidence: CompletionConfidence() {

    @Deprecated("Overriding deprecated member")
    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        return if (SkipAutopopupInStrings.isInStringLiteral(contextElement) && isReflectContext(contextElement)) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }

}
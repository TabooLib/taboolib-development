package org.tabooproject.development.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.tabooproject.development.isReflectContext

class TabooLiteralConfidence: CompletionConfidence() {

    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        return if (SkipAutopopupInStrings.isInStringLiteral(contextElement) && isReflectContext(contextElement)) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }

}
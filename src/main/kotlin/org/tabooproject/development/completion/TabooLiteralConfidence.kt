package org.tabooproject.development.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.CompositeCondition

class TabooLiteralConfidence: CompletionConfidence() {

    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        return if (SkipAutopopupInStrings.isInStringLiteral(contextElement)) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }

}
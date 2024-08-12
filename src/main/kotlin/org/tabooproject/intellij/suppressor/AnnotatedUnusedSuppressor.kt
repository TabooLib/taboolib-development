package org.tabooproject.intellij.suppressor

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotated
import org.tabooproject.intellij.findContainingAnnotated

private val ANNOTATIONS = hashSetOf(
    "SubscribeEvent",
    "Schedule",
    "Awake",
    "CommandBody",
    "CommandHeader",
    "KetherParser",
    "KetherProperty"
)

private const val INSPECTION = "unused"

class AnnotatedUnusedSuppressor: InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION) {
            return false
        }

        return element.findContainingAnnotated()?.hasSuppressUnusedAnnotation() ?: false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    private fun KtAnnotated.hasSuppressUnusedAnnotation(): Boolean {
        val annotationEntries = annotationEntries
        return annotationEntries.any {
            ANNOTATIONS.contains(it.shortName?.asString() ?: return false)
        }
    }

}
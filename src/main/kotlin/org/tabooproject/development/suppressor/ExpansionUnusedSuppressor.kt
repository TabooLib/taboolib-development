package org.tabooproject.development.suppressor

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeReference

private const val INSPECTION = "unused"

private val classes = listOf(
    "taboolib.platform.compat.PlaceholderExpansion",
    "taboolib.common.platform.Plugin",
)

class ExpansionUnusedSuppressor: InspectionSuppressor {
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

    private fun checkIfClassImplementsOrExtends(element: PsiElement, className: String): Boolean {
        val ktClass = PsiTreeUtil.getParentOfType(element, KtClassOrObject::class.java) ?: return false
        return analyze(ktClass) {
            ktClass.implementsInterface(className) || ktClass.isSubclassOf(className)
        }
    }

    private fun KtClassOrObject.isSubclassOf(className: String): Boolean {
        if (fqName?.asString() == className) return true

        val superTypes = this.superTypeListEntries
        return superTypes.any { typeEntry ->
            val typeReference = typeEntry.typeReference
            val typeFqName = typeReference?.getFqName()
            typeFqName == className
        }
    }

    private fun KtClassOrObject.implementsInterface(interfaceName: String): Boolean {
        if (isSubclassOf(interfaceName)) return true

        val superTypes = this.superTypeListEntries
        return superTypes.any { typeEntry ->
            val typeReference = typeEntry.typeReference
            val typeFqName = typeReference?.getFqName()
            typeFqName == interfaceName
        }
    }

    private fun KtTypeReference.getFqName(): String? {
        return analyze(this) {
            val type = type
            val symbol = type.expandedSymbol
            symbol?.classId?.asSingleFqName()?.asString()
        }
    }
}
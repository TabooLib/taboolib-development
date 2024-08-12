package org.tabooproject.intellij.suppressor

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

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
        val context = ktClass.getResolutionFacade().analyze(ktClass, BodyResolveMode.FULL)
        return ktClass.implementsInterface(className, context) || ktClass.isSubclassOf(className, context)
    }

    private fun KtClassOrObject.isSubclassOf(className: String, context: BindingContext): Boolean {
        if (fqName?.asString() == className) return true

        val superTypes = this.superTypeListEntries
        return superTypes.any { typeEntry ->
            val typeReference = typeEntry.typeReference
            val typeFqName = typeReference?.getFqName(context)
            typeFqName == className
        }
    }

    private fun KtClassOrObject.implementsInterface(interfaceName: String, context: BindingContext): Boolean {
        if (isSubclassOf(interfaceName, context)) return true

        val superTypes = this.superTypeListEntries
        return superTypes.any { typeEntry ->
            val typeReference = typeEntry.typeReference
            val typeFqName = typeReference?.getFqName(context)
            typeFqName == interfaceName
        }
    }

    private fun KtTypeReference.getFqName(context: BindingContext): String? {
        val type = context[BindingContext.TYPE, this]
        return type?.constructor?.declarationDescriptor?.fqNameSafe?.asString()
    }
}
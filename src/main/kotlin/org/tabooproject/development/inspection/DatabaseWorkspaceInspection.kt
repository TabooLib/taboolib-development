package org.tabooproject.development.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class DatabaseWorkspaceInspection: AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                val calleeExpression = expression.calleeExpression?.text
                if (calleeExpression == "workspace") {
                    val qualifiedExpression = expression.parent as? KtDotQualifiedExpression
                    val receiverExpression = qualifiedExpression?.receiverExpression
                    val context = receiverExpression?.analyze(BodyResolveMode.PARTIAL)
                    val type = context?.get(BindingContext.EXPRESSION_TYPE_INFO, receiverExpression)?.type
                    val classDescriptor = type?.constructor?.declarationDescriptor as? ClassDescriptor
                    val fqName = classDescriptor?.fqNameSafe?.asString()

                    if (fqName != "taboolib.module.database.Table") return

                    val parent = expression.parent

                    if (parent is KtDotQualifiedExpression) {
                        val grandParent = parent.parent
                        if (grandParent is KtBlockExpression) {
                            val hasRunCall = grandParent.statements.any {
                                it is KtCallExpression && it.calleeExpression != null
                            }
                            if (!hasRunCall) {
                                holder.registerProblem(
                                    expression,
                                    "Calling 'workspace' without any method",
                                    ProblemHighlightType.WARNING,
                                    AddRunQuickFix()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    class AddRunQuickFix : LocalQuickFix {
        override fun getName() = "Add 'run' method after workspace"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val parent = element.parent
            if (parent is KtDotQualifiedExpression) {
                val factory = KtPsiFactory(project)
                val newExpression = factory.createExpression("${parent.text}.run()")
                parent.replace(newExpression)
            }
        }
    }
}
package org.tabooproject.development.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.tabooproject.development.fqName

class DatabaseWorkspaceInspection: AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                val calleeExpression = expression.calleeExpression?.text
                if (calleeExpression == "workspace") {

                    val qualifiedExpression = expression.parent as? KtDotQualifiedExpression
                    val fqName = qualifiedExpression?.fqName ?: return

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
                                    "Not use any functions after 'workspace'",
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
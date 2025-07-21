package org.tabooproject.development.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.tabooproject.development.fqName

/**
 * TabooLib 数据库工作区检查器
 * 
 * 检查 workspace() 调用后是否缺少后续方法调用，这可能导致数据库操作未提交
 * 
 * @since 1.31
 */
class DatabaseWorkspaceInspection: AbstractKotlinInspection() {
    
    /**
     * 构建检查访问器
     * 
     * @param holder 问题收集器
     * @param isOnTheFly 是否在线检查
     * @return Kotlin 访问器
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                val calleeExpression = expression.calleeExpression?.text

                if (calleeExpression == "workspace") {

                    val qualifiedExpression = expression.parent as? KtDotQualifiedExpression
                    val fqName = qualifiedExpression?.fqName ?: return

                    // 确保是 TabooLib 的 Table.workspace() 调用
                    if (fqName != "taboolib.module.database.Table") return

                    val parent = expression.parent

                    if (parent is KtDotQualifiedExpression) {
                        val grandParent = parent.parent
                        if (grandParent is KtBlockExpression) {
                            // 检查在同一个代码块中是否有后续的方法调用
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

    /**
     * 快速修复：在 workspace 后添加 run 方法调用
     */
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
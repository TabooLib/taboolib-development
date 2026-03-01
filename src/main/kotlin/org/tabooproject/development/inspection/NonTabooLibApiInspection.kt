package org.tabooproject.development.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.tabooproject.development.fqName

/**
 * 非 TabooLib API 调用检查器
 *
 * 标记使用了被 shadow 重定位后的 taboolib API 的导入和全限定调用，
 * 这些 API 的包名包含 "taboolib" 但不以 "taboolib." 开头。
 */
class NonTabooLibApiInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return object : KtVisitorVoid() {

            override fun visitImportDirective(importDirective: KtImportDirective) {
                val importPath = importDirective.importedFqName?.asString() ?: return
                if (isRelocatedTabooLib(importPath)) {
                    holder.registerProblem(
                        importDirective,
                        "调用非taboolib api",
                        ProblemHighlightType.ERROR,
                        ReplaceWithTabooLibImportFix(importPath)
                    )
                }
            }

            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val text = expression.text
                if (!text.contains("taboolib")) return

                val fqName = expression.fqName ?: return
                if (isRelocatedTabooLib(fqName)) {
                    holder.registerProblem(
                        expression,
                        "调用非taboolib api",
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
    }

    companion object {

        fun isRelocatedTabooLib(fqName: String): Boolean {
            return !fqName.startsWith("taboolib.") &&
                    (fqName.contains(".taboolib.") || fqName.endsWith(".taboolib"))
        }
    }

    /**
     * 快速修复：将重定位的 taboolib 导入替换为原始 taboolib 导入
     */
    class ReplaceWithTabooLibImportFix(private val originalFqName: String) : LocalQuickFix {

        private val taboolibFqName = originalFqName.substring(originalFqName.indexOf("taboolib"))

        override fun getName() = "替换为 taboolib 导入: $taboolibFqName"

        override fun getFamilyName() = "替换为 taboolib api"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val importDirective = descriptor.psiElement as? KtImportDirective ?: return
            val factory = KtPsiFactory(project)
            val newImport = factory.createImportDirective(ImportPath.fromString(taboolibFqName))
            importDirective.replace(newImport)
        }
    }
}

package org.tabooproject.development.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * 非 TabooLib API 调用检查器
 *
 * 检查注解是否来自被 shadow 重定位后的 taboolib 包，
 * 即包名包含 "taboolib" 但不以 "taboolib." 开头。
 * 错误直接报在注解上。
 */
class NonTabooLibApiInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return object : KtVisitorVoid() {

            override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                val typeRef = annotationEntry.typeReference ?: return
                val userType = typeRef.typeElement as? KtUserType ?: return
                val shortName = userType.referenceExpression?.text ?: return

                val ktFile = annotationEntry.containingKtFile

                // 从 import 中查找注解的完全限定名
                val importDirective = ktFile.importDirectives.firstOrNull {
                    it.importedFqName?.shortName()?.asString() == shortName
                } ?: return
                val fqName = importDirective.importedFqName?.asString() ?: return

                if (isRelocatedTabooLib(fqName)) {
                    holder.registerProblem(
                        annotationEntry,
                        "非本项目的Taboolib路径",
                        ProblemHighlightType.ERROR,
                        ReplaceWithTabooLibImportFix(fqName)
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
            val annotationEntry = descriptor.psiElement as? KtAnnotationEntry ?: return
            val ktFile = annotationEntry.containingKtFile
            val factory = KtPsiFactory(project)

            // 查找并替换对应的 import
            val shortName = taboolibFqName.substringAfterLast('.')
            val importDirective = ktFile.importDirectives.firstOrNull {
                val importedFqName = it.importedFqName?.asString() ?: return@firstOrNull false
                importedFqName == originalFqName || importedFqName.endsWith(".$shortName")
            }
            if (importDirective != null) {
                val newImport = factory.createImportDirective(ImportPath.fromString(taboolibFqName))
                importDirective.replace(newImport)
            }
        }
    }
}

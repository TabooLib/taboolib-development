package org.tabooproject.development.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath


class InfoFuncCompletion: CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        // 确保前面是个对象, 否则不提供打印
        (position.parent.parent as? KtDotQualifiedExpression)?.receiverExpression ?: return
        result.addElement(
            PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("info")
                    .withIcon(PlatformIcons.METHOD_ICON)
                    .withTailText(" 打印日志 ", true)
                    .withInsertHandler handler@{ context, _ ->
                        val editor = context.editor
                        val startOffset = context.startOffset
                        val tailOffset = context.tailOffset
                        val element = context.file.findElementAt(startOffset)
                        if (element != null) {
                            val parent = element.parent.parent

                            val ktFile = element.containingFile as? KtFile ?: return@handler

                            if (parent != null) {
                                try {
                                    val objectText = parent.text.let {
                                        val index = it.indexOfLast { c -> c == '.' }
                                        it.substring(0, index)
                                    }
                                    context.document.replaceString(
                                        parent.textRange.startOffset,
                                        tailOffset,
                                        "info($objectText)"
                                    )
                                    editor.caretModel
                                        .moveToOffset(parent.textRange.startOffset + 5 + objectText.length)

                                    PsiDocumentManager.getInstance(element.project).commitDocument(context.document)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                // 检查和引入info包
                                val import =
                                    PsiTreeUtil.findChildrenOfType(ktFile, KtImportDirective::class.java)
                                val hasImport =
                                    import.any { it.importPath?.pathStr == "taboolib.common.platform.function.info" }
                                if (!hasImport) {
                                    val factory = KtPsiFactory(context.project)
                                    val importDirective =
                                        factory.createImportDirective(ImportPath.fromString("taboolib.common.platform.function.info"))
                                    ktFile.importList?.add(importDirective)
                                }
                            }
                        }
                    },
                -1000.0,
            ),
        )
    }

}
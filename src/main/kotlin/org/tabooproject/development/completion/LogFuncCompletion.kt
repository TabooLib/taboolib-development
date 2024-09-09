package org.tabooproject.development.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.tabooproject.development.checkAndImportPackage


class LogFuncCompletion: CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position

        if (!PsiUtilCore.findLanguageFromElement(position).isKindOf(KotlinLanguage.INSTANCE)) {
            return
        }

        // 确保前面是个对象, 否则不提供打印
        (position.parent.parent as? KtDotQualifiedExpression)?.receiverExpression ?: return

        // 添加日志助手
        result.addElement(
            PrioritizedLookupElement.withPriority(
                generate("info", "打印日志"),
                -1000.0,
            ),
        )
        result.addElement(
            PrioritizedLookupElement.withPriority(
                generate("warning", "打印警告日志"),
                -1001.0,
            ),
        )
        result.addElement(
            PrioritizedLookupElement.withPriority(
                generate("severe", "打印错误日志"),
                -1002.0,
            ),
        )
    }

    private fun generate(name: String, description: String): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withIcon(PlatformIcons.METHOD_ICON)
            .withTailText(" $description ", true)
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

                        ktFile.checkAndImportPackage("taboolib.common.platform.function.${name}")
                    }
                }
            }
    }

}
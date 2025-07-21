package org.tabooproject.development.completion

import ai.grazie.utils.capitalize
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.jvm.JvmModifier
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.kotlin.desc
import org.tabooproject.development.checkAndImportPackage
import org.tabooproject.development.getPsiClass

class ReflectMethodReflectCompletion : CompletionContributor() {

    init {
        extend(
            null,
            PlatformPatterns.psiElement().inside(KtLiteralStringTemplateEntry::class.java),
            InvokeMethodReflectCompletionProvider
        )

        extend(
            null,
            PlatformPatterns.psiElement().inside(KtLiteralStringTemplateEntry::class.java),
            GetPropertyReflectCompletionProvider
        )
    }


}

object GetPropertyReflectCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {

        val element = parameters.position
        val parent = element.parent

        if (parent is KtLiteralStringTemplateEntry) {
            val currentElement: KtCallExpression = PsiTreeUtil.findFirstParent(parent) {
                it is KtCallExpression
            } as? KtCallExpression ?: return

            val calleeExpression = currentElement.calleeExpression ?: return
            val calleeText = calleeExpression.text

            if (calleeText == "getProperty") {
                val qualifiedExpression = (PsiTreeUtil.findFirstParent(calleeExpression) {
                    it is KtDotQualifiedExpression
                } as? KtDotQualifiedExpression) ?: return

                val clazz = qualifiedExpression.getPsiClass() ?: return

                val currentText =
                    parent.text.substring(0, parent.text.length - CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.length)
                val properties = currentText.split("/").toMutableList()

                val searchClasses = if (clazz.isInterface) {
                    ClassInheritorsSearch.search(
                        clazz
                    ).toList()
                } else {
                    listOf(clazz)
                }

                println("search classes")

                val fields = ArrayList<Pair<PsiField, PsiClass>>()

                searchClasses.forEach { searchClass ->
                    fields += searchProperty(properties, searchClass)
                }

                fields.forEach { (field, fromClass) ->
                    result.addElement(
                        LookupElementBuilder.create(field.name)
                            .withIcon(PlatformIcons.FIELD_ICON)
                            .withTailText(" ${field.type.presentableText} ${
                                if (clazz != fromClass) {
                                    "from ${fromClass.kotlinFqName?.asString() ?: "unknown"}"
                                } else ""
                            }", true)
                            .withInsertHandler handler@{ context, _ ->
                                handleInsert(context, field)
                            }
                    )
                }
            }
        }
    }

    private fun handleInsert(context: InsertionContext, field: PsiField) {
        val startOffset = context.startOffset
        val currElement = context.file.findElementAt(startOffset) ?: return

        val callExpression = PsiTreeUtil.findFirstParent(currElement) {
            it is KtCallExpression
        } as? KtCallExpression ?: return

        val generics =
            PsiTreeUtil.findChildOfType(callExpression, KtTypeArgumentList::class.java)

        val ktFile = context.file as? KtFile ?: return

        val factory = KtPsiFactory(context.project)

        val invokeNameReference = PsiTreeUtil.findChildOfType(
            callExpression,
            KtNameReferenceExpression::class.java
        ) ?: return

        val returnTypeElement = field.type
        // 导入包
        val primitiveType = returnTypeElement as? PsiPrimitiveType
        if (primitiveType == null && returnTypeElement.canonicalText != "java.lang.String") {
            ktFile.checkAndImportPackage(returnTypeElement.canonicalText)
        }
        val shortName = primitiveType?.name?.capitalize() ?: returnTypeElement.presentableText

        // 如果没有泛型, 就要添加泛型
        if (generics == null) {
            callExpression.addAfter(factory.createTypeArguments("<${shortName}>"), invokeNameReference)
        } else {
            // 替换泛型
            generics.replace(factory.createTypeArguments("<${shortName}>"))
        }
    }

    private fun searchProperty(list: MutableList<String>, currentPackage: PsiClass): Array<Pair<PsiField, PsiClass>> {
        if (list.isEmpty()) return emptyArray()

        if (list.size == 1) {
            return currentPackage.fields.map { it to currentPackage }.toTypedArray()
        } else {
            val fieldName = list.removeFirst()
            val field = currentPackage.fields.firstOrNull {
                it.name == fieldName
            } ?: return emptyArray()

            val psiClass = field.type.let {
                PsiTypesUtil.getPsiClass(it)
            } ?: return emptyArray()

            return searchProperty(list, psiClass)
        }
    }
}

object InvokeMethodReflectCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {

        val element = parameters.position
        val parent = element.parent

        if (parent is KtLiteralStringTemplateEntry) {
            val currentElement: KtCallExpression = PsiTreeUtil.findFirstParent(parent) {
                it is KtCallExpression
            } as? KtCallExpression ?: return

            val calleeExpression = currentElement.calleeExpression ?: return
            val calleeText = calleeExpression.text

            if (calleeText == "invokeMethod") {
                val qualifiedExpression = (PsiTreeUtil.findFirstParent(calleeExpression) {
                    it is KtDotQualifiedExpression
                } as? KtDotQualifiedExpression) ?: return

                val clazz = qualifiedExpression.getPsiClass() ?: return

                val methods = HashSet<PsiMethod>()
                clazz.findMethods(methods)
                methods.forEach { method ->
                    result.addElement(
                        LookupElementBuilder.create(method.name)
                            .withIcon(PlatformIcons.METHOD_ICON)
                            .withTailText(" ${method.desc} ", true)
                            .withInsertHandler handler@{ context, lookElement ->
                                handleInsert(context, method)
                            }
                    )
                }
            }
        }
    }

    private fun handleInsert(context: InsertionContext, method: PsiMethod) {
        val startOffset = context.startOffset
        val currElement = context.file.findElementAt(startOffset) ?: return

        val callExpression = PsiTreeUtil.findFirstParent(currElement) {
            it is KtCallExpression
        } as? KtCallExpression ?: return

        val generics =
            PsiTreeUtil.findChildOfType(callExpression, KtTypeArgumentList::class.java)

        val ktFile = context.file as? KtFile ?: return

        val factory = KtPsiFactory(context.project)

        val invokeNameReference = PsiTreeUtil.findChildOfType(
            callExpression,
            KtNameReferenceExpression::class.java
        ) ?: return

        val returnTypeElement = method.returnTypeElement
        if (returnTypeElement != null) {
            returnTypeElement.type

            // 导入包
            val primitiveType = returnTypeElement.type as? PsiPrimitiveType

            if (primitiveType == null && returnTypeElement.type.canonicalText != "java.lang.String") {
                ktFile.checkAndImportPackage(returnTypeElement.type.canonicalText)
            }
            val shortName = primitiveType?.name?.capitalize() ?: returnTypeElement.type.presentableText

            if (method.hasModifier(JvmModifier.STATIC)) {
                PsiTreeUtil.findChildOfType(callExpression, KtValueArgument::class.java)?.let { valueArgument ->

                    val binaryExpression = PsiTreeUtil.findChildOfType(valueArgument, KtBinaryExpression::class.java)
                    if (binaryExpression != null) {
                        binaryExpression.replace(factory.createExpression("isStatic = true"))
                    } else {
                        val stringTemplateExpression =
                            PsiTreeUtil.findChildOfType(valueArgument, KtStringTemplateExpression::class.java)
                                ?: return@let
                        val comma = factory.createComma()
                        valueArgument.addAfter(factory.createExpression("isStatic = true"), stringTemplateExpression)
                        valueArgument.addAfter(comma, stringTemplateExpression)
                    }
                }
            }

            // 如果没有泛型, 就要添加泛型
            if (generics == null) {
                callExpression.addAfter(factory.createTypeArguments("<${shortName}>"), invokeNameReference)
            } else {
                // 替换泛型
                generics.replace(factory.createTypeArguments("<${shortName}>"))
            }
        }
    }

    private fun PsiClass.findMethods(methods: MutableSet<PsiMethod>): MutableSet<PsiMethod> {
        this.methods.forEach { methods += it }
        this.interfaces.forEach { interfaceClass ->
            interfaceClass.findMethods(methods)
        }
        this.supers.forEach { superClass ->
            superClass.findMethods(methods)
        }

        return methods
    }
}
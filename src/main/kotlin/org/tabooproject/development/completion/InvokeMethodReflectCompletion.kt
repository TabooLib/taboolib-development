package org.tabooproject.development.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class InvokeMethodReflectCompletion: CompletionContributor() {

    init {
        extend(
            null,
            PlatformPatterns.psiElement().inside(KtLiteralStringTemplateEntry::class.java),
            InvokeMethodReflectCompletionProvider()
        )
    }
}

class InvokeMethodReflectCompletionProvider : CompletionProvider<CompletionParameters>() {
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

            val calleeExpression = currentElement.calleeExpression
            val calleeText = calleeExpression?.text

            var testEle: PsiElement = calleeExpression!!
            while (true) {
                testEle = testEle.parent ?: break
                println(testEle::class.java.name)
                println(testEle.text)
            }

            if (calleeText == "invokeMethod") {
                val qualifiedExpression = PsiTreeUtil.findFirstParent(calleeExpression) {
                    it is KtDotQualifiedExpression
                } as? KtDotQualifiedExpression
                val receiverExpression = qualifiedExpression?.receiverExpression
                val bindingContext = receiverExpression?.analyze(BodyResolveMode.PARTIAL)
                val type = bindingContext?.get(BindingContext.EXPRESSION_TYPE_INFO, receiverExpression)?.type
                val classDescriptor = type?.constructor?.declarationDescriptor as? ClassDescriptor
                val fqName = classDescriptor?.fqNameSafe?.asString() ?: return

                val clazz = JavaPsiFacade.getInstance(calleeExpression.project)
                    .findClass(fqName, GlobalSearchScope.allScope(calleeExpression.project)) ?: run {
                    return
                }

                val methods = HashSet<String>()
                clazz.findMethods(methods)
                methods.forEach { method ->
                    result.addElement(LookupElementBuilder.create(method))
                }
            }
        }
    }

    private fun PsiClass.findMethods(methods: MutableSet<String>): MutableSet<String> {
        this.methods.forEach { methods += it.name }
        this.interfaces.forEach { interfaceClass ->
            interfaceClass.findMethods(methods)
        }
        this.supers.forEach { superClass ->
            superClass.findMethods(methods)
        }

        return methods
    }
}
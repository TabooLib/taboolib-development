package org.tabooproject.development

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun createFileWithDirectories(baseDir: String, relativePath: String): Path? {
    val fullPath = Paths.get(baseDir, relativePath)
    val parentDir = fullPath.parent ?: return null

    if (Files.notExists(parentDir)) {
        Files.createDirectories(parentDir)
    }

    if (Files.notExists(fullPath)) {
        return Files.createFile(fullPath)
    }
    return null
}

fun createOkHttpClientWithSystemProxy(block: OkHttpClient.Builder.() -> Unit = {}): OkHttpClient {
    val proxyHost = System.getProperty("http.proxyHost")
    val proxyPort = System.getProperty("http.proxyPort")

    val clientBuilder = OkHttpClient.Builder().apply {
        block(this)
    }

    if (!proxyHost.isNullOrEmpty() && !proxyPort.isNullOrEmpty()) {
        val proxy = Proxy(
            Proxy.Type.HTTP,
            InetSocketAddress(proxyHost, proxyPort.toInt())
        )
        clientBuilder.proxy(proxy)
    }

    return clientBuilder.build()
}

fun getRequest(url: String): Request {
    return Request.Builder().url(url).build()
}

fun PsiElement.findContainingAnnotated(): KtAnnotated? = findParent(resolveReferences = false) { it is KtAnnotated }

private inline fun <reified T : PsiElement> PsiElement.findParent(
    resolveReferences: Boolean,
    stop: (PsiElement) -> Boolean,
): T? {
    var el: PsiElement = this

    while (true) {
        if (resolveReferences && el is PsiReference) {
            el = el.resolve() ?: return null
        }

        if (el is T) {
            return el
        }

        if (el is PsiFile || el is PsiDirectory || stop(el)) {
            return null
        }

        el = el.parent ?: return null
    }
}

fun readFromUrl(url: String): String? {
    return try {
        val client = createOkHttpClientWithSystemProxy {
            connectTimeout(30, TimeUnit.SECONDS)  // 增加超时时间
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
            // 添加重试机制
            retryOnConnectionFailure(true)
        }

        val response = client
            .newCall(getRequest(url))
            .execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message} for URL: $url")
        }

        response.body?.string()
    } catch (e: Exception) {
        // 记录详细错误信息但不抛出异常，让调用方决定如何处理
        println("Network request failed for $url: ${e.message}")
        e.printStackTrace()
        null
    }
}

fun PsiClass.getContainingPackageName(): String? {
    val containingPackage = containingClass
    return containingPackage?.qualifiedName
}

fun isReflectContext(context: PsiElement): Boolean {
    val expression = PsiTreeUtil.findFirstParent(context) {
        it is KtCallExpression
    } as? KtCallExpression ?: return false
    val name = expression.calleeExpression?.text ?: return false
    if (name == "invokeMethod" || name == "getProperty" || name == "setProperty") {
        return true
    }

    return false
}

private fun isPackageInProject(file: PsiFile, packageName: String): Boolean {
    val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return false
    val orderEnumerator = OrderEnumerator.orderEntries(module).recursively().librariesOnly().classes()
    return orderEnumerator.urls.any { it.contains(packageName.replace('.', '/')) }
}


fun KtFile.checkAndImportPackage(path: String) {
    val fqName = FqName(path)

    // 检查和引入info包
    val import =
        PsiTreeUtil.findChildrenOfType(this, KtImportDirective::class.java)
    val hasImport =
        import.any { it.importedFqName == fqName }
    if (!hasImport) {
        val factory = KtPsiFactory(project)
        val importDirective =
            factory.createImportDirective(ImportPath.fromString(path))
        importList?.add(importDirective)
    }
}

/**
 * 获取 KtDotQualifiedExpression 的完全限定名
 * 
 * K2兼容的实现：基于文本分析获取FQ名称
 * 虽然不如语义分析精确，但在K2环境下更稳定
 */
val KtDotQualifiedExpression.fqName: String?
    get() {
        // 尝试从表达式文本中解析出完全限定名
        val fullText = text
        
        // 移除方法调用部分，只保留类型路径
        val cleanText = fullText.substringBefore('(').trim()
        
        // 如果包含点分隔符，可能是完全限定名
        return if (cleanText.contains('.')) {
            cleanText
        } else {
            // 对于简单名称，尝试从导入中推断
            null
        }
    }

fun KtDotQualifiedExpression.getPsiClass(): PsiClass? {
    return JavaPsiFacade.getInstance(project)
        .findClass(fqName ?: return null, GlobalSearchScope.allScope(project)) ?: run {
        return null
    }
}
package org.tabooproject.intellij

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
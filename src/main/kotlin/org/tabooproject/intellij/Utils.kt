package org.tabooproject.intellij

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
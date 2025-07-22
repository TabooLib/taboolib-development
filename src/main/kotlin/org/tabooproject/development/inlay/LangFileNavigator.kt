package org.tabooproject.development.inlay

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.PsiElementProcessor

/**
 * 语言文件导航器
 * 
 * 用于在语言文件中定位并导航到指定的语言键
 * 
 * @since 1.42
 */
object LangFileNavigator {
    
    /**
     * 导航到语言文件中的指定键
     * 
     * @param project 项目
     * @param languageFile 语言文件
     * @param key 要导航到的语言键
     * @return 导航是否成功
     */
    fun navigateToKey(project: Project, languageFile: VirtualFile, key: String): Boolean {
        if (!languageFile.exists() || !languageFile.isValid) {
            return false
        }
        
        val psiFile = PsiManager.getInstance(project).findFile(languageFile) ?: return false
        val content = String(languageFile.contentsToByteArray(), Charsets.UTF_8)
        val lines = content.lines()
        
        // 搜索键的精确位置
        var targetLine = -1
        var targetColumn = -1
        
        // 查找包含完整键的行（处理嵌套键）
        for (i in lines.indices) {
            val line = lines[i]
            
            // 直接匹配完整键
            if (line.contains("$key:")) {
                targetLine = i
                targetColumn = line.indexOf(key)
                break
            }
            
            // 处理嵌套键
            if (key.contains(".")) {
                val parts = key.split(".")
                val firstPart = parts.first()
                
                if (line.trim().startsWith("$firstPart:")) {
                    // 可能是嵌套键的父级，继续检查子级
                    var currentIndent = getIndentation(line)
                    var j = i + 1
                    
                    // 检查后续行是否包含子级键
                    while (j < lines.size) {
                        val nextLine = lines[j]
                        val nextIndent = getIndentation(nextLine)
                        
                        // 如果缩进级别减少，说明已经退出了当前嵌套
                        if (nextIndent <= currentIndent && !nextLine.isBlank()) {
                            break
                        }
                        
                        // 检查是否包含最后一部分键
                        val lastPart = parts.last()
                        if (nextLine.trim().startsWith("$lastPart:")) {
                            targetLine = j
                            targetColumn = nextLine.indexOf(lastPart)
                            break
                        }
                        
                        j++
                    }
                    
                    if (targetLine >= 0) {
                        break
                    }
                }
            }
        }
        
        if (targetLine >= 0) {
            // 打开文件并定位到目标行
            val fileEditorManager = FileEditorManager.getInstance(project)
            val descriptor = OpenFileDescriptor(project, languageFile, targetLine, targetColumn)
            fileEditorManager.openEditor(descriptor, true)
            return true
        }
        
        // 如果没有找到精确位置，只打开文件
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openFile(languageFile, true)
        return false
    }
    
    /**
     * 获取行的缩进级别
     * 
     * @param line 要检查的行
     * @return 缩进的空格数
     */
    private fun getIndentation(line: String): Int {
        var i = 0
        while (i < line.length && line[i].isWhitespace()) {
            i++
        }
        return i
    }
} 
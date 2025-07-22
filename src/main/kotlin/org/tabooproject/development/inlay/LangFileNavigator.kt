package org.tabooproject.development.inlay

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

/**
 * TabooLib 语言文件导航器
 * 
 * 负责实现点击跳转到语言文件中的具体键位置
 * 
 * @since 1.32
 */
object LangFileNavigator {
    
    /**
     * 跳转到语言文件中的指定键
     * 
     * @param project 当前项目
     * @param languageFile 语言文件
     * @param languageKey 要跳转的语言键
     * @return 是否成功跳转
     */
    fun navigateToLanguageKey(
        project: Project,
        languageFile: VirtualFile,
        languageKey: String
    ): Boolean {
        // 打开语言文件
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editors = fileEditorManager.openFile(languageFile, true)
        
        if (editors.isEmpty()) {
            return false
        }
        
        // 获取编辑器
        val editor = fileEditorManager.getSelectedEditor(languageFile)?.let {
            if (it is com.intellij.openapi.fileEditor.TextEditor) {
                it.editor
            } else null
        } ?: return false
        
        // 查找语言键在文件中的位置
        val offset = findLanguageKeyOffset(project, languageFile, languageKey)
        if (offset >= 0) {
            // 跳转到指定位置
            editor.caretModel.moveToOffset(offset)
            editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
            return true
        }
        
        return false
    }
    
    /**
     * 查找语言键在文件中的偏移位置
     * 
     * @param project 当前项目
     * @param languageFile 语言文件
     * @param languageKey 要查找的语言键
     * @return 语言键的偏移位置，未找到返回 -1
     */
    private fun findLanguageKeyOffset(
        project: Project,
        languageFile: VirtualFile,
        languageKey: String
    ): Int {
        val psiManager = PsiManager.getInstance(project)
        val psiFile = psiManager.findFile(languageFile) ?: return -1
        
        val content = psiFile.text
        val lines = content.lines()
        
        // 解析语言键路径
        val keyParts = languageKey.split(".")
        
        return findKeyInLines(lines, keyParts, 0, 0)
    }
    
    /**
     * 在指定行中递归查找语言键
     * 
     * @param lines 文件行列表
     * @param keyParts 语言键部分列表
     * @param currentKeyIndex 当前查找的键部分索引
     * @param startLineIndex 开始查找的行索引
     * @return 找到的偏移位置，未找到返回 -1
     */
    private fun findKeyInLines(
        lines: List<String>,
        keyParts: List<String>,
        currentKeyIndex: Int,
        startLineIndex: Int
    ): Int {
        if (currentKeyIndex >= keyParts.size) {
            return -1
        }
        
        val targetKey = keyParts[currentKeyIndex]
        var currentOffset = 0
        
        // 计算到startLineIndex的偏移量
        for (i in 0 until startLineIndex) {
            if (i < lines.size) {
                currentOffset += lines[i].length + 1 // +1 for newline
            }
        }
        
        for (i in startLineIndex until lines.size) {
            val line = lines[i]
            val trimmedLine = line.trimStart()
            
            // 跳过空行和注释
            if (trimmedLine.isBlank() || trimmedLine.startsWith('#')) {
                currentOffset += line.length + 1
                continue
            }
            
            // 查找键
            val colonIndex = trimmedLine.indexOf(':')
            if (colonIndex != -1) {
                val key = trimmedLine.substring(0, colonIndex).trim()
                
                if (key == targetKey) {
                    // 找到目标键
                    if (currentKeyIndex == keyParts.size - 1) {
                        // 这是最后一个键部分，返回键的位置
                        val keyOffset = currentOffset + line.indexOf(key)
                        return keyOffset
                    } else {
                        // 还有子键，递归查找
                        val value = trimmedLine.substring(colonIndex + 1).trim()
                        if (value.isEmpty()) {
                            // 这是一个嵌套对象，继续在子级中查找
                            val childResult = findKeyInLines(
                                lines, 
                                keyParts, 
                                currentKeyIndex + 1, 
                                i + 1
                            )
                            if (childResult >= 0) {
                                return childResult
                            }
                        }
                    }
                }
            }
            
            currentOffset += line.length + 1
        }
        
        return -1
    }
    
    /**
     * 获取键的缩进级别
     * 
     * @param line 文本行
     * @return 缩进级别（空格数）
     */
    private fun getIndentLevel(line: String): Int {
        val trimmed = line.trimStart()
        return line.length - trimmed.length
    }
} 
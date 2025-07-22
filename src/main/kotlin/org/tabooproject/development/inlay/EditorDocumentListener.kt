package org.tabooproject.development.inlay

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ApplicationManager

/**
 * 编辑器文档监听器
 *
 * 监听编辑器中的文档变更，当语言文件被编辑时实时刷新提示
 *
 * @since 1.42
 */
@Service(Service.Level.PROJECT)
class EditorDocumentListener(private val project: Project) : Disposable {

    /**
     * 防抖时间戳
     */
    @Volatile
    private var lastRefreshTime = 0L
    private val refreshDebounceMs = 200L
    
    /**
     * 文档监听器
     */
    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            val document = event.document
            val file = FileDocumentManager.getInstance().getFile(document)

            if (file != null && isLanguageFile(file)) {
                // 防抖：避免频繁刷新
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRefreshTime < refreshDebounceMs) {
                    return
                }
                lastRefreshTime = currentTime
                
                // 延迟刷新以合并连续的修改
                ApplicationManager.getApplication().invokeLater({
                    if (!project.isDisposed) {
                        refreshEditorsForFile(file)
                    }
                }, project.disposed)
            }
        }
    }

    /**
     * 已注册监听的文档集合
     */
    private val registeredDocuments = HashSet<Document>()

    /**
     * 初始化
     */
    init {
        // 注册当前打开的所有编辑器
        registerCurrentEditors()
    }

    /**
     * 注册当前打开的所有编辑器
     */
    fun registerCurrentEditors() {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles

        for (file in openFiles) {
            if (isLanguageFile(file)) {
                val document = FileDocumentManager.getInstance().getDocument(file)
                if (document != null) {
                    registerDocument(document)
                }
            }
        }
    }

    /**
     * 注册文档监听器
     *
     * @param document 要监听的文档
     */
    fun registerDocument(document: Document) {
        if (!registeredDocuments.contains(document)) {
            document.addDocumentListener(documentListener)
            registeredDocuments.add(document)
        }
    }

    /**
     * 取消注册文档监听器
     *
     * @param document 要取消监听的文档
     */
    fun unregisterDocument(document: Document) {
        if (registeredDocuments.contains(document)) {
            document.removeDocumentListener(documentListener)
            registeredDocuments.remove(document)
        }
    }

    /**
     * 针对特定文件刷新相关编辑器（优化版本）
     */
    private fun refreshEditorsForFile(changedFile: VirtualFile) {
        // 清除该文件的缓存
        LangParser.clearCache(changedFile)

        // 只刷新包含 sendLang 调用的 Kotlin 文件
        val fileEditorManager = FileEditorManager.getInstance(project)
        val psiManager = PsiManager.getInstance(project)
        val daemonCodeAnalyzer = com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project)

        fileEditorManager.allEditors.forEach { editor ->
            if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                val virtualFile = editor.file
                if (virtualFile != null && virtualFile.name.endsWith(".kt")) {
                    val psiFile = psiManager.findFile(virtualFile)
                    if (psiFile != null && containsSendLangCalls(psiFile)) {
                        // 只重新分析包含 sendLang 调用的文件
                        daemonCodeAnalyzer.restart(psiFile)
                    }
                }
            }
        }
    }

    /**
     * 检查文件是否包含 sendLang 调用（简单启发式检查）
     */
    private fun containsSendLangCalls(psiFile: com.intellij.psi.PsiFile): Boolean {
        return try {
            psiFile.text.contains("sendLang") || psiFile.text.contains("asLangText")
        } catch (e: Exception) {
            true // 发生异常时默认刷新
        }
    }

    /**
     * 刷新所有编辑器（保留作为备用方法）
     */
    private fun refreshAllEditors() {
        // 清除所有缓存
        LangParser.clearCache()

        // 使用 ApplicationManager 确保在 EDT 中执行
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                // 重新分析所有打开的编辑器
                val fileEditorManager = FileEditorManager.getInstance(project)
                val psiManager = PsiManager.getInstance(project)
                val daemonCodeAnalyzer = com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project)

                fileEditorManager.allEditors.forEach { editor ->
                    if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                        val virtualFile = editor.file
                        if (virtualFile != null) {
                            val psiFile = psiManager.findFile(virtualFile)
                            if (psiFile != null) {
                                // 强制重新分析整个文件
                                daemonCodeAnalyzer.restart(psiFile)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查文件是否为语言文件
     *
     * @param file 要检查的文件
     * @return 如果是语言文件返回 true
     */
    private fun isLanguageFile(file: VirtualFile): Boolean {
        return LangFiles.isLangFile(file)
    }

    override fun dispose() {
        // 清理所有监听器
        registeredDocuments.forEach { document ->
            document.removeDocumentListener(documentListener)
        }
        registeredDocuments.clear()
    }

    companion object {
        /**
         * 获取编辑器文档监听器实例
         *
         * @param project 项目实例
         * @return 编辑器文档监听器
         */
        fun getInstance(project: Project): EditorDocumentListener {
            return project.service<EditorDocumentListener>()
        }
    }
}

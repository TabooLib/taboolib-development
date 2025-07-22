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

/**
 * 编辑器文档监听器
 * 
 * 监听编辑器中的文档变更，当语言文件被编辑时实时刷新提示
 * 
 * @since 1.32
 */
@Service(Service.Level.PROJECT)
class EditorDocumentListener(private val project: Project) : Disposable {
    
    /**
     * 文档监听器
     */
    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            val document = event.document
            val file = FileDocumentManager.getInstance().getFile(document)
            
            if (file != null && isLanguageFile(file)) {
                println("[TabooLib] 检测到语言文件编辑: ${file.path}")
                
                // 立即刷新所有编辑器
                refreshAllEditors()
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
        println("[TabooLib] 编辑器文档监听器已初始化")
        
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
            println("[TabooLib] 注册文档监听器: ${document.hashCode()}")
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
     * 刷新所有编辑器
     */
    private fun refreshAllEditors() {
        // 清除所有缓存
        project.service<LangFileWatcher>().clearAllCaches()

        // 使用 ApplicationManager 确保在 EDT 中执行
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                // 重新分析所有打开的编辑器
                val fileEditorManager = FileEditorManager.getInstance(project)
                val psiManager = PsiManager.getInstance(project)
                val daemonCodeAnalyzer = com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project)
                val inlayHintsPassFactory = try {
                    // 兼容不同平台版本
                    Class.forName("com.intellij.codeInsight.hints.InlayHintsPassFactory")
                        .getMethod("getInstance")
                        .invoke(null) as? com.intellij.codeInsight.hints.InlayHintsPassFactory
                } catch (_: Throwable) {
                    null
                }

                fileEditorManager.allEditors.forEach { editor ->
                    if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                        val virtualFile = editor.file
                        if (virtualFile != null) {
                            val psiFile = psiManager.findFile(virtualFile)
                            if (psiFile != null) {
                                // 强制重新分析整个文件
                                daemonCodeAnalyzer.restart(psiFile)
                                // 额外触发 Inlay Hints 刷新（如可用）
                                inlayHintsPassFactory?.let {
                                    try {
                                        it.javaClass.getMethod("refreshInlayHints", com.intellij.psi.PsiFile::class.java)
                                            .invoke(it, psiFile)
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    }
                }

                // 延迟再次刷新，确保变更生效
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater({
                    if (!project.isDisposed) {
                        fileEditorManager.allEditors.forEach { editor ->
                            if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                                val virtualFile = editor.file
                                if (virtualFile != null) {
                                    val psiFile = psiManager.findFile(virtualFile)
                                    if (psiFile != null) {
                                        daemonCodeAnalyzer.restart(psiFile)
                                    }
                                }
                            }
                        }
                    }
                }, 100)
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
        if (file.isDirectory || !file.exists()) {
            return false
        }
        
        val fileName = file.name.lowercase()
        val filePath = file.path.lowercase()
        
        // 检查是否是 YAML 文件且路径中包含 lang
        val isYaml = fileName.endsWith(".yml") || fileName.endsWith(".yaml")
        val isInLangDir = filePath.contains("lang")
        
        return isYaml && isInLangDir
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
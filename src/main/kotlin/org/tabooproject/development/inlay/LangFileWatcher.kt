package org.tabooproject.development.inlay

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiManager
import com.intellij.util.messages.MessageBus
import java.util.concurrent.ConcurrentHashMap

/**
 * TabooLib 语言文件监听器
 * 
 * 负责监听语言文件的变更，并及时刷新缓存以确保 Inlay Hints 显示最新内容
 * 
 * @since 1.32
 */
@Service(Service.Level.PROJECT)
class LangFileWatcher(private val project: Project) : Disposable {
    
    /**
     * 监听的语言文件集合
     */
    private val watchedFiles = ConcurrentHashMap.newKeySet<String>()
    
    /**
     * 文件监听器
     */
    private val fileListener = object : VirtualFileListener {
        override fun contentsChanged(event: VirtualFileEvent) {
            handleFileChange(event.file)
        }
        
        override fun fileDeleted(event: VirtualFileEvent) {
            handleFileChange(event.file)
        }
        
        override fun fileMoved(event: VirtualFileMoveEvent) {
            handleFileChange(event.file)
        }
    }
    
    init {
        // 注册文件监听器
        VirtualFileManager.getInstance().addVirtualFileListener(fileListener, this)
    }
    
    /**
     * 开始监听指定的语言文件
     * 
     * @param languageFile 要监听的语言文件
     */
    fun watchLanguageFile(languageFile: VirtualFile) {
        if (isLanguageFile(languageFile)) {
            watchedFiles.add(languageFile.path)
        }
    }
    
    /**
     * 停止监听指定的语言文件
     * 
     * @param languageFile 要停止监听的语言文件
     */
    fun unwatchLanguageFile(languageFile: VirtualFile) {
        watchedFiles.remove(languageFile.path)
    }
    
    /**
     * 处理文件变更事件
     * 
     * @param file 发生变更的文件
     */
    private fun handleFileChange(file: VirtualFile) {
        // 检查是否为语言文件，即使不在监听列表中也处理
        if (isLanguageFile(file) || isWatchedFile(file)) {
            // 立即添加到监听列表
            watchedFiles.add(file.path)
            
            // 清除相关缓存
            clearFileCache(file)
            
            // 立即刷新 Inlay Hints
            refreshInlayHints()
            
            // 延迟100ms后再次刷新，确保变更生效
            ApplicationManager.getApplication().invokeLater {
                Thread.sleep(100)
                refreshInlayHints()
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
        if (file.isDirectory) {
            return false
        }
        
        val fileName = file.name.lowercase()
        val parentPath = file.parent?.path?.lowercase() ?: ""
        
        return (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) &&
               parentPath.contains("lang")
    }
    
    /**
     * 检查文件是否在监听列表中
     * 
     * @param file 要检查的文件
     * @return 如果在监听列表中返回 true
     */
    private fun isWatchedFile(file: VirtualFile): Boolean {
        return watchedFiles.contains(file.path) || isLanguageFile(file)
    }
    
    /**
     * 清除指定文件的缓存
     * 
     * @param file 要清除缓存的文件
     */
    private fun clearFileCache(file: VirtualFile) {
        // 通知 SendLangInlayHintsProvider 清除相关缓存
        project.messageBus.syncPublisher(LangFileCacheListener.TOPIC)
            .onFileCacheInvalidated(file)
    }
    
    /**
     * 刷新所有 Inlay Hints
     */
    private fun refreshInlayHints() {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                // 触发编辑器重新分析
                val psiManager = PsiManager.getInstance(project)
                val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                
                fileEditorManager.allEditors.forEach { editor ->
                    if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                        val virtualFile = editor.file
                        if (virtualFile != null) {
                            val psiFile = psiManager.findFile(virtualFile)
                            if (psiFile != null) {
                                // 触发重新分析
                                com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project)
                                    .restart(psiFile)
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取所有监听的文件
     * 
     * @return 监听的文件路径集合
     */
    fun getWatchedFiles(): Set<String> {
        return watchedFiles.toSet()
    }
    
    /**
     * 清除所有监听的文件
     */
    fun clearAllWatched() {
        watchedFiles.clear()
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCaches() {
        // 通知所有缓存监听器
        watchedFiles.forEach { path ->
            val file = VirtualFileManager.getInstance().findFileByUrl("file://$path")
            if (file != null) {
                project.messageBus.syncPublisher(LangFileCacheListener.TOPIC)
                    .onFileCacheInvalidated(file)
            }
        }
    }
    
    override fun dispose() {
        // 监听器会在 Disposable 销毁时自动移除
        clearAllWatched()
    }
    
    companion object {
        /**
         * 获取项目的语言文件监听器实例
         * 
         * @param project 项目实例
         * @return 语言文件监听器
         */
        fun getInstance(project: Project): LangFileWatcher {
            return project.getService(LangFileWatcher::class.java)
        }
    }
}

/**
 * 语言文件缓存监听器接口
 */
interface LangFileCacheListener {
    
    /**
     * 当文件缓存失效时调用
     * 
     * @param file 失效的文件
     */
    fun onFileCacheInvalidated(file: VirtualFile)
    
    companion object {
        val TOPIC = com.intellij.util.messages.Topic.create(
            "LangFileCacheListener", 
            LangFileCacheListener::class.java
        )
    }
} 
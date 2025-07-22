package org.tabooproject.development.inlay

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.EditorNotifications
import java.util.concurrent.atomic.AtomicLong

/**
 * TabooLib语言文件监听器
 * 
 * 监听语言文件变更，并自动更新通知和提示
 * 
 * @since 1.42
 */
object LangFileListener : BulkFileListener {
    
    private val lastRefreshTime = AtomicLong(0)
    private const val REFRESH_DEBOUNCE_MS = 300L // 防抖间隔

    override fun before(events: List<VFileEvent>) {
        // 文件修改前无需处理
    }

    override fun after(events: List<VFileEvent>) {
        // 检查是否有语言文件发生变更
        val affectedFiles = events.mapNotNull { it.file }.filter { LangFiles.isLangFile(it) }
        if (affectedFiles.isEmpty()) return
        
        // 防抖：避免频繁刷新
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime.get() < REFRESH_DEBOUNCE_MS) {
            return
        }
        lastRefreshTime.set(currentTime)
        
        // 清除受影响文件的缓存
        affectedFiles.forEach { file ->
            LangParser.clearCache(file)
        }
        
        // 更新编辑器通知（轻量级操作）
        EditorNotifications.updateAll()
        
        // 延迟刷新项目，避免阻塞文件操作
        ApplicationManager.getApplication().invokeLater {
            updateAffectedProjects(affectedFiles)
        }
    }

    /**
     * 更新受影响的项目（优化版本）
     */
    private fun updateAffectedProjects(affectedFiles: List<VirtualFile>) {
        val projectManager = com.intellij.openapi.project.ProjectManager.getInstance()
        val openProjects = projectManager.openProjects
        
        // 只刷新实际包含受影响文件的项目
        for (project in openProjects) {
            if (project.isDisposed) continue
            
            val projectBasePath = project.basePath ?: continue
            val hasAffectedFiles = affectedFiles.any { file ->
                file.path.startsWith(projectBasePath)
            }
            
            if (hasAffectedFiles) {
                refreshProject(project)
            }
        }
    }
    
    /**
     * 刷新项目（优化版本）
     */
    private fun refreshProject(project: Project) {
        if (project.isDisposed) return
        
        ApplicationManager.getApplication().invokeLater {
            val psiManager = com.intellij.psi.PsiManager.getInstance(project)
            val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
            val daemonCodeAnalyzer = com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project)
            
            // 只刷新当前打开的 Kotlin 文件
            fileEditorManager.allEditors.forEach { editor ->
                if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                    val virtualFile = editor.file
                    if (virtualFile != null && virtualFile.isValid && virtualFile.name.endsWith(".kt")) {
                        val psiFile = psiManager.findFile(virtualFile)
                        if (psiFile != null) {
                            // 只重新分析包含 sendLang 调用的文件
                            daemonCodeAnalyzer.restart(psiFile)
                        }
                    }
                }
            }
        }
    }
} 
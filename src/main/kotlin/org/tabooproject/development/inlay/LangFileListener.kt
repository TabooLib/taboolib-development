package org.tabooproject.development.inlay

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.EditorNotifications

/**
 * TabooLib语言文件监听器
 * 
 * 监听语言文件变更，并自动更新通知和提示
 * 
 * @since 1.42
 */
object LangFileListener : BulkFileListener {

    override fun before(events: List<VFileEvent>) {
        // 文件修改前无需处理
    }

    override fun after(events: List<VFileEvent>) {
        // 检查是否有语言文件发生变更
        if (events.any { LangFiles.isLangFile(it.file) }) {
            // 清除受影响文件的缓存
            events.forEach { event ->
                event.file?.let { file ->
                    LangParser.clearCache(file)
                }
            }
            
            // 更新所有编辑器通知
            EditorNotifications.updateAll()
            
            // 更新所有项目的代码折叠和行标记
            updateAllProjects()
        }
    }

    /**
     * 更新所有项目
     */
    private fun updateAllProjects() {
        ApplicationManager.getApplication().invokeLater {
            val openProjects = com.intellij.openapi.project.ProjectManager.getInstance().openProjects
            for (project in openProjects) {
                if (!project.isDisposed) {
                    refreshProject(project)
                }
            }
        }
    }
    
    /**
     * 刷新项目
     */
    private fun refreshProject(project: Project) {
        if (project.isDisposed) return
        
        ApplicationManager.getApplication().invokeLater {
            val psiManager = com.intellij.psi.PsiManager.getInstance(project)
            val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
            val daemonCodeAnalyzer = com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project)
            
            // 刷新当前打开的所有编辑器
            fileEditorManager.allEditors.forEach { editor ->
                if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                    val virtualFile = editor.file
                    if (virtualFile != null && virtualFile.isValid) {
                        val psiFile = psiManager.findFile(virtualFile)
                        if (psiFile != null) {
                            // 强制重新分析整个文件
                            daemonCodeAnalyzer.restart(psiFile)
                        }
                    }
                }
            }
            
            // 强制更新折叠区域和行标记
            val updateManager = com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl.getInstance(project)
            updateManager.setUpdateByTimerEnabled(false) // 暂时关闭自动更新
            updateManager.setUpdateByTimerEnabled(true)  // 重新开启自动更新
            
            // 刷新所有编辑器
            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }
} 
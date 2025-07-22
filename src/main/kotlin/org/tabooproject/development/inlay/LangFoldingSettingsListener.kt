package org.tabooproject.development.inlay

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.concurrency.AppExecutorUtil

/**
 * TabooLib语言文件折叠设置变更监听器
 * 
 * 监听设置变更并刷新所有编辑器的折叠状态
 * 
 * @since 1.42
 */
class LangFoldingSettingsListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 设置变更时刷新折叠的实现
        // 这里暂时留空，因为设置页面的变更会自动触发UI刷新
        // 如果需要更精细的控制，可以实现自定义的监听器
    }
    
    companion object {
        /**
         * 刷新所有打开编辑器的折叠状态
         */
        fun refreshAllEditors() {
            ReadAction.nonBlocking {
                val projects = ProjectManager.getInstance().openProjects
                for (project in projects) {
                    if (!project.isDisposed) {
                        refreshProjectEditors(project)
                    }
                }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }
        
        /**
         * 刷新指定项目的所有编辑器折叠状态
         */
        private fun refreshProjectEditors(project: Project) {
            if (project.isDisposed) return
            
            val editors = EditorFactory.getInstance().allEditors
            for (editor in editors) {
                if (editor.project == project && !editor.isDisposed) {
                    // 在 EDT 中执行写操作
                    ApplicationManager.getApplication().invokeLater {
                        if (!editor.isDisposed && !project.isDisposed) {
                            ApplicationManager.getApplication().runWriteAction {
                                try {
                                    val foldingManager = CodeFoldingManager.getInstance(project)
                                    foldingManager.buildInitialFoldings(editor)
                                } catch (e: Exception) {
                                    // 忽略异常
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
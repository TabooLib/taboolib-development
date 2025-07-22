package org.tabooproject.development.inlay

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil

/**
 * TabooLib语言文件折叠刷新监听器
 * 
 * 确保代码折叠在项目加载后正确显示
 * 
 * @since 1.42
 */
class LangFoldingRefreshListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 项目打开后，在后台线程延迟刷新代码折叠
        ReadAction.nonBlocking {
            refreshFoldingInAllEditors(project)
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun refreshFoldingInAllEditors(project: Project) {
        if (project.isDisposed) return
        
        val fileEditorManager = FileEditorManager.getInstance(project)
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        
        // 遍历所有打开的编辑器
        for (editor in EditorFactory.getInstance().allEditors) {
            if (editor.project == project && !editor.isDisposed) {
                refreshFoldingInEditor(editor, project, psiDocumentManager)
            }
        }
    }

    private fun refreshFoldingInEditor(editor: Editor, project: Project, psiDocumentManager: PsiDocumentManager) {
        if (editor.isDisposed || project.isDisposed) return
        
        val document = editor.document
        val psiFile = psiDocumentManager.getPsiFile(document)
        
        // 只处理Kotlin文件
        if (psiFile?.language?.id == "kotlin") {
            val foldingManager = CodeFoldingManager.getInstance(project)
            
            // 在后台线程中执行，避免 EDT 线程访问违规
            ReadAction.nonBlocking {
                // 在 EDT 中执行写操作
                ApplicationManager.getApplication().invokeLater {
                    if (!editor.isDisposed && !project.isDisposed) {
                        ApplicationManager.getApplication().runWriteAction {
                            try {
                                foldingManager.buildInitialFoldings(editor)
                            } catch (e: Exception) {
                                // 忽略异常，避免影响编辑器性能
                            }
                        }
                    }
                }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }
}
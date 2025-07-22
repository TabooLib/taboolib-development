package org.tabooproject.development.inlay

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDocumentManager

/**
 * TabooLib语言文件折叠刷新监听器
 * 
 * 确保代码折叠在项目加载后正确显示
 * 
 * @since 1.42
 */
class LangFoldingRefreshListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 项目打开后，延迟刷新代码折叠
        ApplicationManager.getApplication().invokeLater {
            refreshFoldingInAllEditors(project)
        }
    }

    private fun refreshFoldingInAllEditors(project: Project) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        
        // 遍历所有打开的编辑器
        for (editor in EditorFactory.getInstance().allEditors) {
            if (editor.project == project) {
                refreshFoldingInEditor(editor, project, psiDocumentManager)
            }
        }
    }

    private fun refreshFoldingInEditor(editor: Editor, project: Project, psiDocumentManager: PsiDocumentManager) {
        val document = editor.document
        val psiFile = psiDocumentManager.getPsiFile(document)
        
        // 只处理Kotlin文件
        if (psiFile?.language?.id == "kotlin") {
            val foldingManager = CodeFoldingManager.getInstance(project)
            
            // 刷新代码折叠
            ApplicationManager.getApplication().runWriteAction {
                foldingManager.buildInitialFoldings(editor)
            }
        }
    }
}
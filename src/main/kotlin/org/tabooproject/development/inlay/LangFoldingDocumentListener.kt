package org.tabooproject.development.inlay

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager

/**
 * TabooLib语言文件折叠文档监听器
 * 
 * 监听当前项目的文档变化，确保折叠状态正确更新
 * 
 * @since 1.42
 */
class LangFoldingDocumentListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 获取文档管理器
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val fileDocumentManager = FileDocumentManager.getInstance()
        
        // 创建文档监听器
        val documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val document = event.document
                
                // 检查文档是否属于当前项目
                if (!isDocumentBelongsToProject(document, project, fileDocumentManager)) {
                    return
                }
                
                // 延迟刷新，避免在编辑过程中频繁更新
                ApplicationManager.getApplication().invokeLater({
                    if (!project.isDisposed) {
                        refreshFoldingForDocument(document, project, psiDocumentManager, fileDocumentManager)
                    }
                }, project.disposed)
            }
        }
        
        // 注册项目级别的文档监听器，避免全局监听
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                EditorFactory.getInstance().eventMulticaster.addDocumentListener(documentListener, project)
            }
        }
    }

    /**
     * 检查文档是否属于当前项目
     * 
     * @param document 要检查的文档
     * @param project 当前项目
     * @param fileDocumentManager 文件文档管理器
     * @return 如果文档属于当前项目返回 true
     */
    private fun isDocumentBelongsToProject(
        document: Document, 
        project: Project,
        fileDocumentManager: FileDocumentManager
    ): Boolean {
        try {
            val virtualFile = fileDocumentManager.getFile(document) ?: return false
            
            // 检查是否是 Light File 或临时文件
            if (virtualFile.javaClass.simpleName.contains("Light") || 
                virtualFile.path.contains("Dummy") ||
                !virtualFile.isInLocalFileSystem) {
                return false
            }
            
            // 检查文件是否在项目路径下
            val projectBasePath = project.basePath ?: return false
            if (!virtualFile.path.startsWith(projectBasePath)) {
                return false
            }
            
            return true
        } catch (e: Exception) {
            // 任何异常都视为不属于当前项目
            return false
        }
    }

    /**
     * 为指定文档刷新折叠
     * 
     * @param document 要刷新的文档
     * @param project 当前项目
     * @param psiDocumentManager PSI文档管理器
     * @param fileDocumentManager 文件文档管理器
     */
    private fun refreshFoldingForDocument(
        document: Document, 
        project: Project, 
        psiDocumentManager: PsiDocumentManager,
        fileDocumentManager: FileDocumentManager
    ) {
        try {
            // 再次检查文档是否属于当前项目
            if (!isDocumentBelongsToProject(document, project, fileDocumentManager)) {
                return
            }
            
            val psiFile = psiDocumentManager.getPsiFile(document)
            
            // 只处理Kotlin文件，并确保PSI文件属于当前项目
            if (psiFile?.language?.id != "kotlin" || psiFile.project != project) {
                return
            }
            
            // 获取该文档在当前项目中的编辑器
            val editors = EditorFactory.getInstance().getEditors(document, project)
            
            for (editor in editors) {
                if (editor.isDisposed) continue
                
                val foldingManager = CodeFoldingManager.getInstance(project)
                val foldingModel = editor.foldingModel
                
                // 在写入操作中更新折叠
                ApplicationManager.getApplication().runWriteAction {
                    try {
                        foldingModel.runBatchFoldingOperation {
                            // 重新构建折叠区域
                            foldingManager.updateFoldRegions(editor)
                            
                            // 获取当前光标位置
                            val caretOffset = editor.caretModel.offset
                            
                            // 智能处理TabooLib翻译折叠区域
                            for (foldRegion in foldingModel.allFoldRegions) {
                                val group = foldRegion.group
                                if (group?.toString()?.startsWith("taboolib.translation.") == true) {
                                    if (LangFoldingSettings.instance.shouldFoldTranslations) {
                                        // 检查光标是否在折叠区域内
                                        val isCaretInside = caretOffset >= foldRegion.startOffset && 
                                                          caretOffset <= foldRegion.endOffset
                                        
                                        if (isCaretInside) {
                                            // 如果光标在折叠区域内，展开它
                                            foldRegion.isExpanded = true
                                        } else {
                                            // 如果光标不在折叠区域内，折叠它
                                            foldRegion.isExpanded = false
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 忽略异常，避免影响编辑器性能
                    }
                }
            }
        } catch (e: Exception) {
            // 捕获所有异常，避免影响其他功能
        }
    }
}
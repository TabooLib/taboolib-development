package org.tabooproject.development.inlay

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDocumentManager

/**
 * TabooLib语言文件折叠文档监听器
 * 
 * 监听文档变化，确保折叠状态正确更新
 * 
 * @since 1.42
 */
class LangFoldingDocumentListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 获取文档管理器
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        
        // 创建文档监听器
        val documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val document = event.document
                
                // 延迟刷新，避免在编辑过程中频繁更新
                ApplicationManager.getApplication().invokeLater({
                    if (!project.isDisposed) {
                        refreshFoldingForDocument(document, project, psiDocumentManager)
                    }
                }, project.disposed)
            }
        }
        
        // 注册全局文档监听器
        ApplicationManager.getApplication().invokeLater {
            EditorFactory.getInstance().eventMulticaster.addDocumentListener(documentListener, project)
        }
    }

    private fun refreshFoldingForDocument(document: Document, project: Project, psiDocumentManager: PsiDocumentManager) {
        val psiFile = psiDocumentManager.getPsiFile(document)
        
        // 只处理Kotlin文件
        if (psiFile?.language?.id != "kotlin") return
        
        // 获取该文档的所有编辑器
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
    }
}
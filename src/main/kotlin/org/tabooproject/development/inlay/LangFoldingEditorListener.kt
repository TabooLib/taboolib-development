package org.tabooproject.development.inlay

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDocumentManager

/**
 * TabooLib语言文件折叠编辑器监听器
 * 
 * 监听编辑器事件，确保失焦后自动折叠sendLang调用
 * 
 * @since 1.42
 */
class LangFoldingEditorListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 注册编辑器工厂监听器
        ApplicationManager.getApplication().invokeLater {
            EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor
                    if (editor.project == project) {
                        setupEditorListener(editor, project)
                        
                        // 编辑器创建后立即触发初始折叠检查
                        ApplicationManager.getApplication().invokeLater {
                            if (!editor.isDisposed) {
                                scheduleInitialFolding(editor, project)
                            }
                        }
                    }
                }
            }, project)
        }
    }

    private fun setupEditorListener(editor: Editor, project: Project) {
        // 添加光标监听器
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                // 当光标移动时，检查是否需要刷新折叠
                scheduleRefreshFolding(editor, project)
            }
        })
    }

    private fun scheduleInitialFolding(editor: Editor, project: Project) {
        // 初始折叠需要更长的延迟，确保折叠区域已经创建
        ApplicationManager.getApplication().invokeLater({
            if (!editor.isDisposed && !project.isDisposed) {
                // 等待折叠区域创建完成
                ApplicationManager.getApplication().invokeLater({
                    if (!editor.isDisposed && !project.isDisposed) {
                        performInitialFolding(editor, project)
                    }
                }, project.disposed)
            }
        }, project.disposed)
    }

    private fun scheduleRefreshFolding(editor: Editor, project: Project) {
        // 延迟执行，避免过于频繁的刷新
        ApplicationManager.getApplication().invokeLater({
            if (!editor.isDisposed) {
                refreshFolding(editor, project)
            }
        }, project.disposed)
    }

    private fun performInitialFolding(editor: Editor, project: Project) {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        
        // 只处理Kotlin文件
        if (psiFile?.language?.id == "kotlin") {
            val foldingManager = CodeFoldingManager.getInstance(project)
            
            // 使用更现代的API重建折叠区域
            ApplicationManager.getApplication().runWriteAction {
                try {
                    // 使用更新的折叠管理方式
                    foldingManager.updateFoldRegions(editor)
                    
                    // 延迟应用初始折叠状态
                    ApplicationManager.getApplication().invokeLater({
                        if (!editor.isDisposed && !project.isDisposed) {
                            applyInitialFoldingState(editor, project)
                        }
                    }, project.disposed)
                } catch (e: Exception) {
                    // 忽略异常
                }
            }
        }
    }

    private fun applyInitialFoldingState(editor: Editor, project: Project) {
        val foldingModel = editor.foldingModel
        
        ApplicationManager.getApplication().runWriteAction {
            try {
                foldingModel.runBatchFoldingOperation {
                    // 获取当前光标位置
                    val caretOffset = editor.caretModel.offset
                    
                    // 为所有TabooLib语言折叠区域设置初始状态
                    for (foldRegion in foldingModel.allFoldRegions) {
                        val group = foldRegion.group
                        if (group?.toString()?.startsWith("taboolib.translation.") == true) {
                            if (LangFoldingSettings.instance.shouldFoldTranslations) {
                                // 检查光标是否在折叠区域内
                                val isCaretInside = caretOffset >= foldRegion.startOffset && 
                                                  caretOffset <= foldRegion.endOffset
                                
                                // 根据光标位置设置初始折叠状态：光标不在内部则折叠
                                foldRegion.isExpanded = isCaretInside
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    private fun refreshFolding(editor: Editor, project: Project) {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        
        // 只处理Kotlin文件
        if (psiFile?.language?.id == "kotlin") {
            val foldingModel = editor.foldingModel
            
            // 在写入操作中更新折叠状态，但不重建折叠区域
            ApplicationManager.getApplication().runWriteAction {
                try {
                    foldingModel.runBatchFoldingOperation {
                        // 获取当前光标位置
                        val caretOffset = editor.caretModel.offset
                        
                        // 只更新现有的TabooLib语言折叠区域状态
                        for (foldRegion in foldingModel.allFoldRegions) {
                            val group = foldRegion.group
                            if (group?.toString()?.startsWith("taboolib.translation.") == true) {
                                if (LangFoldingSettings.instance.shouldFoldTranslations) {
                                    // 检查光标是否在折叠区域内
                                    val isCaretInside = caretOffset >= foldRegion.startOffset && 
                                                      caretOffset <= foldRegion.endOffset
                                    
                                    // 根据光标位置智能设置折叠状态
                                    val shouldExpand = isCaretInside
                                    if (foldRegion.isExpanded != shouldExpand) {
                                        foldRegion.isExpanded = shouldExpand
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
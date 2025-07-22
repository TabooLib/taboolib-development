package org.tabooproject.development.inlay

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDocumentManager

/**
 * TabooLib语言文件折叠鼠标监听器
 * 
 * 监听鼠标点击事件，确保点击别的地方时自动折叠sendLang调用
 * 
 * @since 1.42
 */
class LangFoldingMouseListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 注册编辑器工厂监听器，为每个编辑器添加鼠标监听器
        ApplicationManager.getApplication().invokeLater {
            EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor
                    if (editor.project == project) {
                        setupMouseListener(editor, project)
                    }
                }
            }, project)
        }
    }

    private fun setupMouseListener(editor: Editor, project: Project) {
        // 添加鼠标监听器
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                // 当鼠标点击时，延迟刷新折叠状态
                scheduleRefreshFolding(editor, project)
            }
            
            override fun mousePressed(event: EditorMouseEvent) {
                // 鼠标按下时也触发折叠检查
                scheduleRefreshFolding(editor, project)
            }
        })
    }


    private fun scheduleRefreshFolding(editor: Editor, project: Project) {
        // 延迟执行，避免与其他编辑器操作冲突，确保光标位置已更新
        ApplicationManager.getApplication().invokeLater({
            if (!editor.isDisposed && !project.isDisposed) {
                // 二次延迟确保光标位置已经完全更新
                ApplicationManager.getApplication().invokeLater({
                    if (!editor.isDisposed && !project.isDisposed) {
                        refreshFolding(editor, project)
                    }
                }, project.disposed)
            }
        }, project.disposed)
    }

    private fun refreshFolding(editor: Editor, project: Project) {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        
        // 只处理Kotlin文件
        if (psiFile?.language?.id == "kotlin") {
            val foldingModel = editor.foldingModel
            
            // 确保在 EDT 中执行写操作
            ApplicationManager.getApplication().invokeLater {
                if (!editor.isDisposed && !project.isDisposed) {
                    ApplicationManager.getApplication().runWriteAction {
                        try {
                            foldingModel.runBatchFoldingOperation {
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
    }
}
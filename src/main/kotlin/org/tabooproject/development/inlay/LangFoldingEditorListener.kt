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
import java.util.concurrent.ConcurrentHashMap

/**
 * TabooLib语言文件折叠编辑器监听器
 * 
 * 监听编辑器事件，确保失焦后自动折叠sendLang调用
 * 
 * @since 1.42
 */
class LangFoldingEditorListener : ProjectActivity {

    companion object {
        // 使用弱引用集合跟踪已处理的编辑器，避免重复处理
        private val processedEditors = ConcurrentHashMap.newKeySet<Editor>()
    }

    override suspend fun execute(project: Project) {
        // 注册编辑器工厂监听器
        ApplicationManager.getApplication().invokeLater {
            EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor
                    if (editor.project == project && !processedEditors.contains(editor)) {
                        processedEditors.add(editor)
                        setupEditorListener(editor, project)
                        
                        // 延迟初始化折叠，合并多个延迟调用
                        scheduleInitialFolding(editor, project)
                    }
                }
                
                override fun editorReleased(event: EditorFactoryEvent) {
                    // 清理已释放的编辑器引用
                    processedEditors.remove(event.editor)
                }
            }, project)
        }
    }

    private fun setupEditorListener(editor: Editor, project: Project) {
        // 添加光标监听器
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                // 防抖：避免频繁刷新
                scheduleRefreshFolding(editor, project)
            }
        })
    }

    private fun scheduleInitialFolding(editor: Editor, project: Project) {
        // 合并延迟操作，减少EDT负载
        ApplicationManager.getApplication().invokeLater({
            if (!editor.isDisposed && !project.isDisposed) {
                performInitialFolding(editor, project)
            }
        }, project.disposed)
    }

    private fun scheduleRefreshFolding(editor: Editor, project: Project) {
        // 简化延迟逻辑，减少嵌套调用
        ApplicationManager.getApplication().invokeLater({
            if (!editor.isDisposed && !project.isDisposed) {
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
            
            // 使用 ReadAction.nonBlocking 避免 EDT 线程访问违规
            com.intellij.openapi.application.ReadAction.nonBlocking {
                // 在后台线程中准备数据
                if (!editor.isDisposed && !project.isDisposed) {
                    // 切换到 EDT 执行写操作
                    ApplicationManager.getApplication().invokeLater {
                        if (!editor.isDisposed && !project.isDisposed) {
                            ApplicationManager.getApplication().runWriteAction {
                                try {
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
                }
            }.submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService())
        }
    }

    private fun applyInitialFoldingState(editor: Editor, project: Project) {
        val foldingModel = editor.foldingModel
        
        ApplicationManager.getApplication().runWriteAction {
            try {
                foldingModel.runBatchFoldingOperation {
                    // 初始化时折叠所有TabooLib翻译区域
                    for (foldRegion in foldingModel.allFoldRegions) {
                        val group = foldRegion.group
                        if (group?.toString()?.startsWith("taboolib.translation.") == true) {
                            if (LangFoldingSettings.instance.shouldFoldTranslations) {
                                foldRegion.isExpanded = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // 忽略异常，避免影响编辑器性能
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
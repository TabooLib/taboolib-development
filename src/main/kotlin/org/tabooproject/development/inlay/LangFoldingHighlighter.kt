package org.tabooproject.development.inlay

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDocumentManager

/**
 * TabooLib语言文件折叠高亮器
 * 
 * 为折叠的sendLang调用添加颜色高亮效果
 * 
 * @since 1.42
 */
class LangFoldingHighlighter : ProjectActivity {

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor
                    if (editor.project == project) {
                        setupHighlighter(editor, project)
                    }
                }
            }, project)
        }
    }

    private fun setupHighlighter(editor: Editor, project: Project) {
        // 延迟设置高亮，确保折叠区域已经创建
        ApplicationManager.getApplication().invokeLater({
            if (!editor.isDisposed) {
                highlightFoldedRegions(editor, project)
            }
        }, project.disposed)
    }

    private fun highlightFoldedRegions(editor: Editor, project: Project) {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        
        // 只处理Kotlin文件
        if (psiFile?.language?.id != "kotlin") return
        
        val foldingModel = editor.foldingModel
        val markupModel = editor.markupModel
        
        // 清除之前的高亮
        val existingHighlighters = markupModel.allHighlighters.filter { 
            it.getUserData(TABOOLIB_COLOR_KEY) != null 
        }
        existingHighlighters.forEach { markupModel.removeHighlighter(it) }
        
        // 为所有TabooLib翻译折叠区域添加颜色高亮
        ApplicationManager.getApplication().runReadAction {
            for (foldRegion in foldingModel.allFoldRegions) {
                val group = foldRegion.group
                if (group?.toString()?.startsWith("taboolib.translation.") == true && 
                    !foldRegion.isExpanded) {
                    
                    // 提取语言键
                    val langKey = group.toString().removePrefix("taboolib.translation.")
                    val translation = findTranslationInProject(project, langKey)
                    
                    if (translation != null) {
                        val colorAttribute = LangColorAttributes.extractColorAttribute(translation)
                        if (colorAttribute != null) {
                            // 创建高亮器
                            val highlighter = markupModel.addRangeHighlighter(
                                foldRegion.startOffset,
                                foldRegion.endOffset,
                                HighlighterLayer.ADDITIONAL_SYNTAX,
                                colorAttribute.defaultAttributes,
                                HighlighterTargetArea.EXACT_RANGE
                            )
                            
                            // 标记这是我们的高亮器
                            highlighter.putUserData(TABOOLIB_COLOR_KEY, langKey)
                        }
                    }
                }
            }
        }
    }

    /**
     * 在项目中查找翻译文本
     */
    private fun findTranslationInProject(project: Project, langKey: String): String? {
        // 使用与LangFoldingBuilder相同的测试数据
        val testTranslations = mapOf(
            "test-key" to "这是测试文本",
            "player.join" to "玩家 {0} 加入了游戏", 
            "player.quit" to "玩家 {0} 离开了游戏",
            "test-color" to "&4红色&a绿色&b蓝色文本",
            "red-text" to "&c这是红色文本",
            "green-text" to "&a这是绿色文本",
            "blue-text" to "&9这是蓝色文本",
            "yellow-text" to "&e这是黄色文本",
            "rainbow-text" to "&4红&6橙&e黄&a绿&3青&2蓝&5紫",
            "hex-color" to "&#FF5733这是十六进制颜色",
            "rgb-color" to "&{#00FF00}这是RGB颜色"
        )
        
        return testTranslations[langKey]
    }

    companion object {
        private val TABOOLIB_COLOR_KEY = com.intellij.openapi.util.Key.create<String>("TABOOLIB_LANG_COLOR")
    }
}
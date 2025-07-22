package org.tabooproject.development.inlay

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.util.function.Function
import javax.swing.JComponent

/**
 * TabooLib语言文件编辑器通知提供器
 * 
 * 显示缺失的翻译条目，并提供添加缺失条目的功能
 * 
 * @since 1.42
 */
class LangEditorNotificationProvider : EditorNotificationProvider {

    private var show: Boolean = true

    /**
     * 收集通知数据
     */
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (!show || !LangFiles.isLangFile(file) || LangFiles.isDefaultLocale(file)) {
            return null
        }

        val missingTranslations = findMissingTranslations(project, file)
        if (missingTranslations.isEmpty()) {
            return null
        }

        return Function { editor ->
            createNotificationPanel(missingTranslations, file, project)
        }
    }

    /**
     * 创建通知面板
     */
    private fun createNotificationPanel(
        missingTranslations: List<Lang>,
        file: VirtualFile,
        project: Project
    ): InfoPanel {
        val panel = InfoPanel()
        val missingCount = missingTranslations.size
        val defaultLocale = LangConstants.DEFAULT_LOCALE
        
        panel.text = "语言文件缺少 $missingCount 个翻译条目（相比默认语言文件 $defaultLocale）"
        
        panel.createActionLabel("添加缺失的条目") {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@createActionLabel
            
            // 应用写操作
            ApplicationManager.getApplication().runWriteAction {
                // 读取当前文件内容
                val currentContent = String(file.contentsToByteArray(), Charsets.UTF_8)
                val contentBuilder = StringBuilder(currentContent)
                
                // 如果文件不以换行结束，添加换行
                if (!currentContent.endsWith("\n")) {
                    contentBuilder.append("\n")
                }
                
                // 添加缺失的翻译条目
                contentBuilder.append("\n# 自动添加的缺失翻译条目\n")
                missingTranslations.forEach { lang ->
                    // 处理嵌套键
                    if (lang.key.contains(".")) {
                        contentBuilder.append(getIndentedEntry(lang.key, lang.value))
                    } else {
                        contentBuilder.append("${lang.key}: \"${lang.value}\"\n")
                    }
                }
                
                // 写入文件
                file.setBinaryContent(contentBuilder.toString().toByteArray(Charsets.UTF_8))
                
                // 清除文件缓存
                LangParser.clearCache(file)
                
                // 更新编辑器通知
                EditorNotifications.updateAll()
            }
            
            // 询问是否排序
            val sort = MessageDialogBuilder.yesNo("排序翻译", "是否要对所有翻译条目进行排序？")
                .ask(project)
            
            if (sort) {
                // 这里可以添加排序逻辑
                // 暂时留空，后续可以实现
            }
        }
        
        panel.createActionLabel("隐藏通知") {
            panel.isVisible = false
            show = false
        }
        
        return panel
    }

    /**
     * 获取嵌套键的缩进条目
     */
    private fun getIndentedEntry(key: String, value: String): String {
        val parts = key.split(".")
        val builder = StringBuilder()
        
        // 处理多级嵌套
        if (parts.size > 2) {
            // 复杂嵌套情况，暂时简单处理
            return "${parts.first()}: \n  ${parts.last()}: \"$value\"\n"
        } else if (parts.size == 2) {
            // 两级嵌套
            return "${parts[0]}: \n  ${parts[1]}: \"$value\"\n"
        }
        
        return "$key: \"$value\"\n"
    }

    /**
     * 查找缺失的翻译
     */
    private fun findMissingTranslations(project: Project, file: VirtualFile): List<Lang> {
        // 获取默认语言文件的所有翻译
        val defaultTranslations = LangIndex.getProjectDefaultLangs(project)
        
        // 获取当前文件的所有翻译
        val currentTranslations = LangIndex.getLangs(project, file)
        
        // 查找当前文件缺少的翻译
        val currentKeys = currentTranslations.map { it.key }.toSet()
        
        return defaultTranslations.filter { it.key !in currentKeys }
    }

    /**
     * 自定义信息面板
     */
    class InfoPanel : EditorNotificationPanel() {
        override fun getBackground(): Color {
            val color = EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.NOTIFICATION_BACKGROUND)
            return color ?: UIUtil.getPanelBackground()
        }
    }
} 
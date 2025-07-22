package org.tabooproject.development.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import java.util.concurrent.ConcurrentHashMap

/**
 * TabooLib sendLang 方法 Inlay Hints 提供器
 * 
 * 为 sendLang 方法调用提供 i18n 翻译内容的可视化显示
 * 
 * @since 1.32
 */
@Suppress("UnstableApiUsage")
class SendLangInlayHintsProvider : InlayHintsProvider<NoSettings>, Disposable {
    
    override val key: SettingsKey<NoSettings> = SettingsKey("taboolib.sendlang.hints")
    override val name: String = "TabooLib sendLang i18n hints"
    override val previewText: String = """
        |player.sendLang("message.welcome")
        |// Shows: Welcome to the server!
    """.trimMargin()
    
    /**
     * 语言文件缓存
     */
    private val languageCache = ConcurrentHashMap<String, Map<String, String>>()
    
    /**
     * 缓存监听器，用于处理文件变更事件
     */
    private val cacheListener = object : LangFileCacheListener {
        override fun onFileCacheInvalidated(file: VirtualFile) {
            invalidateFileCache(file)
        }
    }
    
    override fun createSettings(): NoSettings = NoSettings()
    
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): javax.swing.JComponent {
                return javax.swing.JLabel("TabooLib sendLang i18n hints")
            }
        }
    }
    
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        if (file !is KtFile) return null
        
        // 注册缓存监听器
        val project = file.project
        project.messageBus.connect(this).subscribe(LangFileCacheListener.TOPIC, cacheListener)
        
        // 注册编辑器文档监听器
        val documentListener = EditorDocumentListener.getInstance(project)
        documentListener.registerCurrentEditors()
        
        // 注册当前编辑器的文档
        val document = editor.document
        documentListener.registerDocument(document)
        
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (element is KtCallExpression) {
                    processSendLangCall(element, sink, factory)
                }
                return true
            }
        }
    }
    
    /**
     * 处理 sendLang 方法调用
     */
    private fun processSendLangCall(
        callExpression: KtCallExpression,
        sink: InlayHintsSink,
        factory: PresentationFactory
    ) {
        // 检查是否是 sendLang 方法调用
        if (!isSendLangCall(callExpression)) {
            return
        }
        
        // 获取第一个参数（语言键）
        val languageKey = extractLanguageKey(callExpression) ?: return
        
        // 查找并解析语言文件
        val languageFile = LangFileFinder.getPreferredLanguageFile(
            callExpression.project, 
            callExpression
        ) ?: return
        
        // 注册文件监听
        LangFileWatcher.getInstance(callExpression.project).watchLanguageFile(languageFile)
        
        // 获取翻译内容
        val translatedText = getTranslatedText(languageFile, languageKey) ?: return
        
        // 创建并添加 inlay hint
        val presentation = createInlayPresentation(
            translatedText, 
            languageFile, 
            languageKey, 
            factory,
            callExpression.project
        )
        
        sink.addInlineElement(
            callExpression.textRange.endOffset,
            true,
            presentation,
            false
        )
    }
    
    /**
     * 检查是否是 sendLang 方法调用
     */
    private fun isSendLangCall(callExpression: KtCallExpression): Boolean {
        val callee = callExpression.calleeExpression
        
        // 检查方法名
        if (callee?.text != "sendLang") {
            return false
        }
        
        // 检查是否是扩展函数调用 (receiver.sendLang)
        val parent = callExpression.parent
        if (parent is KtDotQualifiedExpression) {
            return true
        }
        
        // 检查是否是直接调用并且有正确的 import
        val ktFile = callExpression.containingFile as? KtFile ?: return false
        val imports = ktFile.importDirectives
        
        return imports.any { import ->
            val importPath = import.importedFqName?.asString()
            importPath == "taboolib.platform.util.sendLang" ||
            importPath?.endsWith(".sendLang") == true
        }
    }
    
    /**
     * 提取语言键参数
     */
    private fun extractLanguageKey(callExpression: KtCallExpression): String? {
        val arguments = callExpression.valueArguments
        if (arguments.isEmpty()) {
            return null
        }
        
        val firstArg = arguments[0].getArgumentExpression()
        
        // 处理字符串字面量
        if (firstArg is KtStringTemplateExpression) {
            return extractStringLiteral(firstArg)
        }
        
        // TODO: 处理变量引用等复杂情况
        return null
    }
    
    /**
     * 提取字符串字面量内容
     */
    private fun extractStringLiteral(stringTemplate: KtStringTemplateExpression): String? {
        if (stringTemplate.entries.size == 1) {
            val entry = stringTemplate.entries[0]
            if (entry is KtLiteralStringTemplateEntry) {
                return entry.text
            }
        }
        return null
    }
    
    /**
     * 获取翻译文本
     */
    private fun getTranslatedText(languageFile: VirtualFile, key: String): String? {
        // 每次都重新解析语言文件，确保获取最新内容
        val langMap = LangFileParser.parseLanguageFile(languageFile)
        val text = LangFileParser.getLanguageText(langMap, key)
        
        // 如果找不到对应的键，返回空字符串而不是null，这样会显示"未知节点"
        return text ?: ""
    }
    
    /**
     * 创建 Inlay Presentation
     */
    private fun createInlayPresentation(
        translatedText: String,
        languageFile: VirtualFile,
        languageKey: String,
        factory: PresentationFactory,
        project: Project
    ): InlayPresentation {
        // 移除所有颜色代码，只保留纯文本
        val cleanText = MinecraftColorProcessor.processColorCodes(translatedText, ColorProcessMode.STRIP)
        
        // 限制显示长度，避免过长的文本
        val maxLength = 50
        val displayText = if (cleanText.length > maxLength) {
            cleanText.take(maxLength - 3) + "..."
        } else {
            cleanText
        }
        
        // 创建前缀和文本
        val prefixText = if (cleanText.isEmpty()) {
            " → [未知节点]"
        } else {
            " → "
        }
        val prefixPresentation = factory.smallText(prefixText)
        val textPresentation = factory.smallText(displayText)
        
        // 组合前缀和文本
        val basePresentation = factory.seq(prefixPresentation, textPresentation)
        
        // 添加点击处理器，跳转到语言文件
        return factory.referenceOnHover(basePresentation) { mouseEvent, point ->
            LangFileNavigator.navigateToLanguageKey(
                project,
                languageFile,
                languageKey
            )
        }
    }
    
    /**
     * 使指定文件的缓存失效
     * 
     * @param file 要使缓存失效的文件
     */
    private fun invalidateFileCache(file: VirtualFile) {
        val keysToRemove = languageCache.keys.filter { key ->
            key.startsWith(file.path)
        }
        keysToRemove.forEach { key ->
            languageCache.remove(key)
        }
    }
    
    override fun dispose() {
        // 清理资源
        languageCache.clear()
    }
} 
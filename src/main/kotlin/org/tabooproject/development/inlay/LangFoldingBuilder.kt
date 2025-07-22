package org.tabooproject.development.inlay

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.tabooproject.development.isSendLangCall

/**
 * TabooLib 语言文件代码折叠构建器
 *
 * 使用代码折叠方式显示翻译文本，更符合IntelliJ平台习惯
 * 支持Minecraft颜色代码显示
 *
 * @since 1.42
 */
class LangFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val project = root.project

        // 使用标准的PSI递归访问器，按照官方文档的做法
        root.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                // 检查是否是sendLang方法调用
                if (element is KtCallExpression && isSendLangCall(element)) {
                    processSendLangCall(element, descriptors, project)
                }
            }
        })

        return descriptors.toTypedArray()
    }

    /**
     * 处理sendLang方法调用
     */
    private fun processSendLangCall(
        callExpression: KtCallExpression,
        descriptors: MutableList<FoldingDescriptor>,
        project: Project
    ) {
        val arguments = callExpression.valueArgumentList ?: return
        if (arguments.arguments.isEmpty()) return

        val firstArg = arguments.arguments[0].getArgumentExpression()
        if (firstArg !is KtStringTemplateExpression) return

        // 提取语言键
        val langKey = extractStringLiteral(firstArg) ?: return

        // 查找翻译文本
        val translation = findTranslationInProject(project, langKey) ?: return

        // 计算折叠范围 - 只折叠第一个参数（语言键）
        val range = firstArg.textRange

        // 创建折叠描述符
        val node = firstArg.node
        val group = FoldingGroup.newGroup("taboolib.translation.$langKey")

        // 创建带颜色信息的折叠描述符
        val coloredDescriptor = createColoredFoldingDescriptor(
            node, range, group, translation, langKey
        )

        descriptors.add(coloredDescriptor)
    }

    /**
     * 创建带颜色信息的折叠描述符
     */
    private fun createColoredFoldingDescriptor(
        node: ASTNode,
        range: com.intellij.openapi.util.TextRange,
        group: FoldingGroup,
        translation: String,
        langKey: String
    ): FoldingDescriptor {
        // 处理颜色代码并创建显示文本
        val displayText = createColoredDisplayText(translation)

        // 创建标准的折叠描述符
        return FoldingDescriptor(node, range, group, displayText)
    }

    /**
     * 创建带颜色的显示文本
     */
    private fun createColoredDisplayText(translation: String): String {
        // 根据用户配置决定是否显示颜色代码
        val displayText = if (LangFoldingSettings.instance.showColorCodes) {
            // 保留颜色代码
            translation
        } else {
            // 移除所有颜色代码，只显示纯文本
            stripColorCodes(translation)
        }

        // 限制显示长度
        val maxLength = 50
        val finalText = if (displayText.length > maxLength) {
            displayText.take(maxLength - 3) + "..."
        } else {
            displayText
        }

        return "\"$finalText\""
    }

    /**
     * 提取字符串字面量
     */
    private fun extractStringLiteral(stringTemplate: KtStringTemplateExpression): String? {
        // 处理完整的字符串内容
        val fullText = stringTemplate.text

        // 处理带引号的情况 (如 "key")
        if ((fullText.startsWith("\"") && fullText.endsWith("\"")) ||
            (fullText.startsWith("'") && fullText.endsWith("'"))
        ) {
            return fullText.substring(1, fullText.length - 1)
        }

        // 使用entries提取内容
        if (stringTemplate.entries.size == 1) {
            val entry = stringTemplate.entries[0]
            if (entry is org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry) {
                return entry.text
            }
        }

        return null
    }

    /**
     * 检查是否包含颜色代码
     */
    private fun hasColorCodes(text: String): Boolean {
        // 检查标准颜色代码 §x 和 &x
        val standardPattern = Regex("[§&][0-9a-fk-or]")
        if (standardPattern.containsMatchIn(text)) return true

        // 检查RGB颜色代码 §{#rrggbb} 和 &{#rrggbb}
        val rgbPattern = Regex("[§&]\\{#[0-9a-fA-F]{6}}")
        if (rgbPattern.containsMatchIn(text)) return true

        // 检查十六进制颜色代码 &#rrggbb
        val hexPattern = Regex("&#[0-9a-fA-F]{6}")
        return hexPattern.containsMatchIn(text)
    }

    /**
     * 移除字符串中的颜色代码
     */
    private fun stripColorCodes(text: String): String {
        // 移除标准颜色代码 §x 和 &x
        var result = text.replace(Regex("[§&][0-9a-fk-or]"), "")

        // 移除RGB颜色代码 §{#rrggbb} 和 &{#rrggbb}
        result = result.replace(Regex("[§&]\\{#[0-9a-fA-F]{6}}"), "")

        // 移除十六进制颜色代码 &#rrggbb
        result = result.replace(Regex("&#[0-9a-fA-F]{6}"), "")

        return result
    }

    /**
     * 在项目中查找翻译文本
     */
    private fun findTranslationInProject(project: Project, langKey: String): String? {
        // 添加一些测试数据，确保基础功能工作
        val testTranslations = mapOf(
            "test-key" to "这是测试文本",
            "player.join" to "玩家 {0} 加入了游戏",
            "player.quit" to "玩家 {0} 离开了游戏",

            // 单颜色示例
            "red-text" to "&c这是红色文本",
            "green-text" to "&a这是绿色文本",
            "blue-text" to "&9这是蓝色文本",
            "yellow-text" to "&e这是黄色文本",

            // 多颜色示例
            "test-color" to "&4红色&a绿色&b蓝色文本",
            "rainbow-text" to "&4红&6橙&e黄&a绿&3青&2蓝&5紫",
            "two-color" to "&c红色&9蓝色混合",
            "three-color" to "&e黄色&a绿色&5紫色文字",
            "complex-color" to "&4警告：&e这是一条&a重要&c的消息",

            // 自定义颜色示例
            "hex-color" to "&#FF5733这是十六进制颜色",
            "rgb-color" to "&{#00FF00}这是RGB颜色",
            "mixed-color" to "&c红色&#FFD700金色&9蓝色",

            // 超多颜色示例（测试彩虹显示）
            "super-rainbow" to "&4红&c亮红&6橙&e黄&2深绿&a绿&b青&3深青&1深蓝&9蓝&5深紫&d紫",

            // 格式代码混合
            "formatted-text" to "&l&4粗体红色&r&o&a斜体绿色"
        )

        testTranslations[langKey]?.let { return it }

        // 获取项目根目录
        val baseDir = project.baseDir ?: return null

        // 查找语言文件目录（通常在src/main/resources目录下）
        val langDirs = listOf(
            "src/main/resources/lang",
            "src/main/resources/language",
            "src/main/resources/languages",
            "resources/lang",
            "lang"
        )

        val langFiles = mutableListOf<com.intellij.openapi.vfs.VirtualFile>()

        // 搜索语言文件
        for (dirPath in langDirs) {
            val dir = baseDir.findFileByRelativePath(dirPath)
            if (dir != null && dir.isDirectory) {
                dir.children.forEach { file ->
                    if (file.extension in listOf("yml", "yaml")) {
                        langFiles.add(file)
                    }
                }
            }
        }

        // 如果没有找到语言文件，进行更广泛的搜索
        if (langFiles.isEmpty()) {
            searchForYamlFiles(baseDir, langFiles)
        }

        // 解析语言文件并查找翻译
        for (langFile in langFiles) {
            try {
                val content = String(langFile.contentsToByteArray(), langFile.charset)
                val translation = parseYamlForKey(content, langKey)
                if (translation != null) {
                    return translation
                }
            } catch (e: Exception) {
                // 忽略解析错误，继续下一个文件
            }
        }

        return null
    }

    /**
     * 递归搜索YAML文件
     */
    private fun searchForYamlFiles(dir: com.intellij.openapi.vfs.VirtualFile, result: MutableList<com.intellij.openapi.vfs.VirtualFile>) {
        if (!dir.isDirectory) return

        dir.children.forEach { child ->
            if (child.isDirectory) {
                // 避免搜索太深或搜索不相关的目录
                if (!child.name.startsWith(".") &&
                    child.name != "target" &&
                    child.name != "build" &&
                    child.name != "node_modules"
                ) {
                    searchForYamlFiles(child, result)
                }
                // 判断是否为yml，父文件夹是否为 lang
            } else if (child.extension in listOf("yml", "yaml") && child.parent.name == "lang") {
                result.add(child)
            }
        }
    }

    /**
     * 解析YAML内容并查找指定键
     */
    private fun parseYamlForKey(content: String, key: String): String? {
        // 简单的YAML解析，处理键值对格式
        val lines = content.lines()

        // 支持嵌套键（如 "player.join"）
        val keyParts = key.split(".")

        if (keyParts.size == 1) {
            // 简单键查找
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("$key:")) {
                    val value = trimmed.substring(key.length + 1).trim()
                    return unquoteYamlString(value)
                }
            }
        } else {
            // 嵌套键查找（简化版本）
            var currentLevel = 0
            var targetLevel = -1
            val targetKey = keyParts.last()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                val indentLevel = line.length - line.trimStart().length

                // 检查是否匹配嵌套路径
                if (indentLevel == 0 && trimmed.startsWith(keyParts[0] + ":")) {
                    targetLevel = 0
                    currentLevel = 0
                } else if (targetLevel >= 0 && indentLevel > currentLevel) {
                    currentLevel = indentLevel
                    targetLevel++

                    if (targetLevel < keyParts.size - 1) {
                        if (!trimmed.startsWith(keyParts[targetLevel] + ":")) {
                            targetLevel = -1 // 路径不匹配
                        }
                    } else if (targetLevel == keyParts.size - 1) {
                        if (trimmed.startsWith("$targetKey:")) {
                            val value = trimmed.substring(targetKey.length + 1).trim()
                            return unquoteYamlString(value)
                        }
                    }
                } else if (indentLevel <= currentLevel) {
                    targetLevel = -1 // 退出当前嵌套
                }
            }
        }

        return null
    }

    /**
     * 移除YAML字符串的引号
     */
    private fun unquoteYamlString(value: String): String {
        var result = value
        if ((result.startsWith("\"") && result.endsWith("\"")) ||
            (result.startsWith("'") && result.endsWith("'"))
        ) {
            result = result.substring(1, result.length - 1)
        }
        return result
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = LangFoldingSettings.instance.shouldFoldTranslations
}

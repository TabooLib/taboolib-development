package org.tabooproject.development.step

import ai.grazie.utils.capitalize
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.tabooproject.development.component.CheckModulePanel
import org.tabooproject.development.settings.TabooLibProjectSettings
import org.tabooproject.development.util.ResourceLoader
import org.tabooproject.development.util.ResourceLoader.loadModules
import java.awt.Dimension
import java.lang.reflect.Method
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.ScrollPaneConstants

//private fun fetchAndParseModules(
//    url: String = "https://raw.githubusercontent.com/TabooLib/taboolib-gradle-plugin/master/src/main/kotlin/io/izzel/taboolib/gradle/Standards.kt",
//): List<String>? {
//    val client = createOkHttpClientWithSystemProxy {
//        connectTimeout(5, TimeUnit.SECONDS)
//        readTimeout(5, TimeUnit.SECONDS)
//    }
//    val request = getRequest(url)
//
//    return try {
//        val response = client.newCall(request).execute()
//        response.body?.string()?.let { responseBody ->
//            parseModules(responseBody)
//        }
//    } catch (e: IOException) {
//        e.printStackTrace()
//        null
//    }
//}

//fun parseModules(content: String): List<String> {
//    val pattern = """val (\w+) =""".toRegex()
//    return pattern.findAll(content)
//        .mapNotNull { matchResult ->
//            val id = matchResult.groupValues[1]
//            id.ifBlank { null }
//        }
//        .toList()
//}

data class Module(
    val name: String,
    val desc: String?,
    val id: String
)


val TEMPLATE_DOWNLOAD_MIRROR = linkedMapOf(
    "tabooproject.org" to "https://template.tabooproject.org",
    "github.com" to "https://github.com/TabooLib/taboolib-sdk/archive/refs/heads/idea-template.zip"
)

data class ConfigurationProperty(
    var name: String? = null,
    var mainClass: String = "org.example.untitled.Untitled",
    var version: String = "1.0.0-SNAPSHOT",
    var mirrorIndex: String = "github.com",
    val modules: MutableList<Module> = mutableListOf() // 不给默认模块了
) {
    init {

    }
}

class ConfigurationPropertiesStep(val context: WizardContext) : ModuleWizardStep() {

    private val checkModulePanel = CheckModulePanel()
    private var mainClassTextField: JTextField? = null
    private var inited = false
    private val settings = TabooLibProjectSettings.getInstance()

    init {
        // 注册 checkModulePanel 到 wizard context 的 disposable
        Disposer.register(context.disposable, checkModulePanel)

        // 设置ResourceLoader使用保存的模块镜像
        val savedModulesMirror = settings.getDefaultModulesMirror()
        if (ResourceLoader.getAvailableMirrors().containsKey(savedModulesMirror)) {
            ResourceLoader.setMirror(savedModulesMirror)
        }

        // 加载用户的默认设置
        loadDefaultSettings()
    }

    companion object {

        var property = ConfigurationProperty()
            private set

        fun refreshTemporaryData() {
            property = ConfigurationProperty()
        }
    }

    /**
     * 加载用户的默认设置
     */
    private fun loadDefaultSettings() {
        // 只设置回调，不立即应用设置（等到updateStep时再应用）
        checkModulePanel.onModuleSelectionChanged = { modules: List<Module> ->
            // 实时保存模块选择到配置中
            property.modules.clear()
            property.modules.addAll(modules)
        }
    }

    override fun getComponent(): JComponent {
        val mainPanel = panel {
            indent {
                // 添加向导步骤指示器
                row {
                    text("<h3>第 2 步，共3 步：插件配置</h3>" +
                         "<p>配置您的插件详细信息并选择 TabooLib 模块</p>")
                        .apply {
                            component.border = JBUI.Borders.empty(0, 0, 20, 0)
                        }
                }

                // 插件基础配置
                group("⚙️ 插件详情", indent = false) {
                    row("插件名称:") {
                        textField()
                            .focused()
                            .validationOnInput { textField ->
                                when {
                                    textField.text.isBlank() -> error("插件名称不能为空")
                                    textField.text.length < 3 -> error("插件名称至少需要 3 个字符")
                                    else -> null
                                }
                            }
                            .apply {
                                component.text = property.name
                                component.columns = 40
                                component.toolTipText = "插件的显示名称\n示例：我的强大插件"
                            }.onChanged {
                                autoChangeMainClass(it.text)
                                property.name = it.text
                            }
                    }.rowComment("<i>在插件列表和日志中显示的名称</i>")

                    row("主类:") {
                        textField()
                            .validationOnInput { textField ->
                                when {
                                    textField.text.isBlank() -> error("主类不能为空")
                                    !textField.text.matches(Regex("[a-zA-Z][a-zA-Z0-9_.]*[a-zA-Z0-9]")) -> 
                                        error("类名格式不正确")
                                    !textField.text.contains(".") -> 
                                        warning("建议使用包名（例如：com.example.MyPlugin）")
                                    else -> null
                                }
                            }
                            .apply {
                                component.text = property.mainClass
                                component.columns = 40
                                component.toolTipText = "包含包名的完整类名\n" +
                                                       "示例：com.example.myplugin.MyPlugin"
                                mainClassTextField = this.component
                            }.onChanged { property.mainClass = it.text }
                    }.rowComment("<i>继承 TabooLib 插件的主类（根据插件名称自动生成）</i>")

                    row("版本:") {
                        textField()
                            .validationOnInput { textField ->
                                when {
                                    textField.text.isBlank() -> error("版本不能为空")
                                    !textField.text.matches(Regex("\\d+\\.\\d+.*")) -> 
                                        warning("建议使用语义化版本号（例如：1.0.0）")
                                    else -> null
                                }
                            }
                            .apply {
                                component.text = property.version
                                component.columns = 20
                                component.toolTipText = "插件的语义化版本\n" +
                                                       "示例：1.0.0, 2.1.3-SNAPSHOT"
                            }.onChanged { property.version = it.text }
                    }.rowComment("<i>遵循语义化版本号：主版本.次版本.修订版本</i>")
                }

                // 改进的模块选择区域
                group("📦 TabooLib 模块", indent = false) {
                    row {
                        text("<div>" +
                             "<b>选择您的插件需要的模块：</b><br/>" +
                             "<small>" +
                             "• 只选择您实际需要的模块以保持插件轻量化<br/>" +
                             "• 您可以随时通过编辑 build.gradle.kts 添加更多模块<br/>" +
                             "• 根据常用模式预选了热门模块" +
                             "</small></div>")
                    }
                    
                    row {
                        cell(checkModulePanel)
                            .align(AlignX.FILL)
                            .apply {
                                // 移除边框，保持干净的外观
                                component.border = JBUI.Borders.empty(10)
                            }
                    }
                }
            }
        }

        // 设置更合理的尺寸，确保可以滚动
        mainPanel.preferredSize = Dimension(900, 450)
        mainPanel.maximumSize = Dimension(Int.MAX_VALUE, 450)

        // 包装在滚动面板中
        val scrollPane = JBScrollPane(mainPanel).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = JBUI.Borders.empty()
            preferredSize = Dimension(900, 600)
        }

        return scrollPane
    }

    private val doPreviousActionMethod: Method by lazy {
        AbstractWizard::class.java.getDeclaredMethod("doPreviousAction").apply {
            isAccessible = true
        }
    }

    override fun _init() {
        if (inited) return

        // 在后台线程加载模块数据
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable {
                try {
                    // 确保模块数据加载完成
                    loadModules()
                    
                    // 在EDT中更新UI
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val modules = ResourceLoader.getModules()
                            
                            // 检查模块数据是否为空
                            if (modules.isEmpty()) {
                                println("警告：模块数据为空，将使用本地文件")
                                // 强制重新加载本地文件
                                ResourceLoader.cacheJson = ResourceLoader.loadLocalModulesToJson()
                                val localModules = ResourceLoader.getModules()
                                checkModulePanel.setModules(localModules)
                            } else {
                                checkModulePanel.setModules(modules)
                            }
                            
                            applyFavoriteModulesFromSettings()
                            inited = true
                            
                            println("模块数据加载完成，共 ${modules.size} 个分类")
                        } catch (e: Exception) {
                            println("UI更新失败: ${e.message}")
                            e.printStackTrace()
                            // 尝试使用本地文件作为备用
                            try {
                                val localModules = ResourceLoader.parseModules(ResourceLoader.loadLocalModulesToJson())
                                checkModulePanel.setModules(localModules)
                                inited = true
                                println("使用本地文件加载模块数据成功")
                            } catch (localError: Exception) {
                                println("本地文件加载也失败: ${localError.message}")
                                localError.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("加载模块列表时出现问题: ${e.message}")
                    e.printStackTrace()
                    
                    // 在EDT中尝试使用本地文件
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val localModules = ResourceLoader.parseModules(ResourceLoader.loadLocalModulesToJson())
                            checkModulePanel.setModules(localModules)
                            inited = true
                            println("使用本地文件作为备用方案成功")
                        } catch (localError: Exception) {
                            println("本地文件备用方案也失败: ${localError.message}")
                            localError.printStackTrace()
                        }
                    }
                }
            },
            "Loading modules list", false, context.project
        )
    }





    override fun updateStep() {
        refreshTemporaryData()

        property.name = context.projectName

        // 在项目名称确定后，重新加载和应用默认设置
        applyDefaultSettings()
    }

    override fun updateDataModel() {
        // 不在这里保存设置，由ProjectBuilder.cleanup()统一处理
    }

    /**
     * 应用默认设置到当前配置
     */
    private fun applyDefaultSettings() {
        // 应用默认包名前缀
        if (settings.getDefaultPackagePrefix().isNotEmpty()) {
            val packagePrefix = settings.getDefaultPackagePrefix()
            val projectName = property.name ?: "Untitled"
            property.mainClass = "$packagePrefix.${projectName.lowercase()}.${projectName.capitalize()}"

            // 更新UI中的文本字段
            mainClassTextField?.text = property.mainClass
        }

        // 应用默认模板镜像
        if (settings.getDefaultTemplateMirror().isNotEmpty()) {
            property.mirrorIndex = settings.getDefaultTemplateMirror()
        }

        // 应用常用模块设置（需要在模块数据加载后）
        applyFavoriteModulesWhenReady()
    }

    /**
     * 在模块数据准备就绪时应用常用模块设置
     */
    private fun applyFavoriteModulesWhenReady() {
        if (inited) {
            // 如果模块数据已经加载，立即应用
            applyFavoriteModulesFromSettings()
        }
        // 否则等待_init()完成后再应用
    }

    /**
     * 从设置中应用常用模块
     */
    private fun applyFavoriteModulesFromSettings() {
        // 不再自动应用常用模块，让右侧默认为空
        // val favoriteModuleIds = settings.getFavoriteModules()
        // if (favoriteModuleIds.isNotEmpty()) {
        //     println("apply fav modules ${favoriteModuleIds}")
        //     // 直接设置选中的模块
        //     checkModulePanel.setSelectedModules(favoriteModuleIds)
        // }
    }

    /**
     * 自动更改主类名。
     * 此函数检查当前的主类名是否符合特定的插件命名模式，并根据匹配情况自动更新主类名。
     * 如果输入的文本与插件名匹配，且现有插件名以该文本为前缀，则将插件名中的该文本替换为新文本。
     *
     * @param text 要替换到主类名中的新文本。
     */
    private fun autoChangeMainClass(text: String) {
        // 如果 mainClassTextField 未初始化, 则直接返回
        if (mainClassTextField == null) return

        // 提取重复的字符串操作, 减少代码重复并提高性能
        var baseClass = property.mainClass.substringBeforeLast(".")
        val currentLastPart = property.mainClass.substringAfterLast(".")

        val newLastPart = when {
            currentLastPart == "Plugin" -> text
            currentLastPart.isEmpty() -> text.capitalize()
            currentLastPart == property.name?.lowercase() -> text.capitalize()
            currentLastPart.removeSuffix("Plugin").lowercase() == property.name?.lowercase() -> text.capitalize()
            else -> currentLastPart
        }

        val lastGroup = baseClass.substringAfterLast(".").let {
            if (it.lowercase() == property.name?.lowercase()) {
                return@let text.lowercase()
            } else {
                it
            }
        }

        baseClass = baseClass.substringBeforeLast(".")

        // 更新 mainClassTextField 的文本
        mainClassTextField!!.text = "$baseClass.$lastGroup.$newLastPart"
    }
}

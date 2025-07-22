package org.tabooproject.development.step

import ai.grazie.utils.capitalize
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ex.MultiLineLabel
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.JBColor
import com.intellij.util.xml.ui.MultiLineTextPanel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.tabooproject.development.component.CheckModulePanel
import org.tabooproject.development.util.ResourceLoader
import org.tabooproject.development.util.ResourceLoader.loadModules
import org.tabooproject.development.settings.TabooLibProjectSettings
import java.awt.Dimension
import java.lang.reflect.Method
import javax.swing.JComponent
import javax.swing.JTextField

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
    var version: String = "1.0-SNAPSHOT",
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
                // 基本配置区域
                group("Project Configuration", indent = true) {
                    row("Plugin name:") {
                        textField()
                            .apply {
                                component.text = property.name
                                component.columns = 35
                                component.toolTipText = "Enter your plugin name (e.g., MyAwesome Plugin)"
                            }.onChanged {
                                autoChangeMainClass(it.text)
                                property.name = it.text
                            }
                    }.rowComment("The display name of your plugin")

                    row("Plugin main class:") {
                        textField()
                            .apply {
                                component.text = property.mainClass
                                component.columns = 35
                                component.toolTipText = "Full class name including package (e.g., com.example.myplugin.MyPlugin)"
                                mainClassTextField = this.component
                            }.onChanged { property.mainClass = it.text }
                    }.rowComment("The main class that extends TabooLib plugin")

                    row("Plugin version:") {
                        textField()
                            .apply {
                                component.text = property.version
                                component.columns = 35
                                component.toolTipText = "Semantic version (e.g., 1.0.0, 2.1.3-SNAPSHOT)"
                            }.onChanged { property.version = it.text }
                    }.rowComment("Initial version of your plugin")
                }

                // 添加分隔空间
                separator()

                // 模块选择区域 - 单独成组，更突出
                group("Module Selection", indent = false) {
                    row {
                        text("Choose the TabooLib modules your plugin will use. Selected modules will be included in your project dependencies.")
                            .apply {
                                component.foreground = JBColor.GRAY
                            }
                    }
                    row {
                        cell(checkModulePanel)
                            .align(com.intellij.ui.dsl.builder.AlignX.FILL)
                    }
                }

            }
        }

        // 设置主面板的最大尺寸以防止对话框过高
        mainPanel.maximumSize = Dimension(Int.MAX_VALUE, 750)

        return mainPanel
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

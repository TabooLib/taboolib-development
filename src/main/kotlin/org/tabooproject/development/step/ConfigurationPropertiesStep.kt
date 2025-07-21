package org.tabooproject.development.step

import ai.grazie.utils.capitalize
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.ex.MultiLineLabel
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xml.ui.MultiLineTextPanel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.tabooproject.development.component.CheckModulePanel
import org.tabooproject.development.util.ResourceLoader
import org.tabooproject.development.util.ResourceLoader.loadModules
import org.tabooproject.development.settings.TabooLibProjectSettings
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


val TEMPLATE_DOWNLOAD_MIRROR = mapOf(
    "github.com" to "https://github.com/TabooLib/taboolib-sdk/archive/refs/heads/idea-template.zip",
    "tabooproject.org" to "https://template.tabooproject.org",
)

data class ConfigurationProperty(
    var name: String? = null,
    var mainClass: String = "org.example.untitled.UntitledPlugin",
    var version: String = "1.0-SNAPSHOT",
    var mirrorIndex: String = "github.com",
    val modules: MutableList<Module> = mutableListOf() // 不给默认模块了
)

class ConfigurationPropertiesStep(val context: WizardContext) : ModuleWizardStep() {

    private val checkModulePanel = CheckModulePanel()
    private var mainClassTextField: JTextField? = null
    private var inited = false
    private val settings = TabooLibProjectSettings.getInstance()

    init {
        // 注册 checkModulePanel 到 wizard context 的 disposable
        Disposer.register(context.disposable, checkModulePanel)
        
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
        // 设置默认包名前缀（如果尚未设置）
        if (property.name == null && settings.getDefaultPackagePrefix().isNotEmpty()) {
            val packagePrefix = settings.getDefaultPackagePrefix()
            // 从包名前缀推导项目名
            val projectName = context.projectName ?: "UntitledPlugin"
            property.mainClass = "$packagePrefix.${projectName.lowercase()}.${projectName.capitalize()}Plugin"
        }
        
        // 设置默认模板镜像
        if (settings.getDefaultTemplateMirror().isNotEmpty()) {
            property.mirrorIndex = settings.getDefaultTemplateMirror()
        }
        
        // 预选常用模块（在模块数据加载后处理）
        checkModulePanel.onModuleSelectionChanged = { modules: List<Module> ->
            // 保存当前选择到设置中，作为下次的默认值
            settings.setFavoriteModules(modules.map { it.id })
        }
    }

    override fun getComponent(): JComponent {
        return panel {
            indent {
                group("Configuration Properties", indent = true) {
                    row("Plugin name:") {
                        textField()
                            .apply {
                                property.mainClass = "org.example.${property.name?.lowercase()}.${property.name?.capitalize()}Plugin"
                                component.text = property.name
                                component.columns = 30
                            }.onChanged {
                                autoChangeMainClass(it.text)
                                property.name = it.text
                            }
                    }
                    row("Plugin main class:") {
                        textField()
                            .apply {
                                component.text = property.mainClass
                                component.columns = 30
                                mainClassTextField = this.component
                            }.onChanged { property.mainClass = it.text }
                    }
                    row("Plugin version:") {
                        textField()
                            .apply {
                                component.text = property.version
                                component.columns = 30
                            }.onChanged { property.version = it.text }
                    }
                    row {
                        cell(checkModulePanel)
                    }
                    row { text("") }
                    row("Select template download mirror:") {
                        comboBox(TEMPLATE_DOWNLOAD_MIRROR.keys)
                            .apply {
                                component.selectedIndex = 0
                                component.columns(20)
                            }.onChanged {
                                property.mirrorIndex = it.selectedItem as String
                            }
                    }
                }
            }
        }
    }

    private val doPreviousActionMethod: Method by lazy {
        AbstractWizard::class.java.getDeclaredMethod("doPreviousAction").apply {
            isAccessible = true
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun _init() {
        if (inited) return

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable {
                try {
                    loadModules()
                } catch (e: Exception) {
                    val wizard = context.getUserData(AbstractWizard.KEY)
                    doPreviousActionMethod.invoke(wizard)

                    ApplicationManager.getApplication().invokeLater {
                        DialogBuilder().apply {
                            setTitle("Download module list failed")
                            setCenterPanel(MultiLineLabel(e.message).apply {

                            })
                            addCancelAction().setText("Cancel")
                        }.show()
                    }
                    throw e
                }
            },
            "Downloading modules list", false, context.project
        )

        GlobalScope.launch {
            coroutineScope {
                val modules = ResourceLoader.getModules()
                checkModulePanel.setModules(modules)
                
                // 应用用户的常用模块设置
                ApplicationManager.getApplication().invokeLater {
                    applyFavoriteModules(modules)
                }
                
                inited = true
            }
        }
    }



    /**
     * 应用用户保存的常用模块设置
     */
    private fun applyFavoriteModules(modules: Map<String, List<Module>>) {
        val favoriteModuleIds = settings.getFavoriteModules()
        if (favoriteModuleIds.isNotEmpty()) {
            // 从所有模块中找到匹配的模块并自动选中
            val allModules = modules.values.flatten()
            val favoriteModules = allModules.filter { it.id in favoriteModuleIds }
            
            // 将常用模块添加到当前配置中
            property.modules.clear()
            property.modules.addAll(favoriteModules)
            
            // 通知UI更新（如果需要的话，这里可以触发复选框的更新）
        }
    }

    override fun updateStep() {
        if (property.name == null) {
            property.name = context.projectName
        }
    }

    override fun updateDataModel() {
        // 自动保存当前配置为默认设置，供下次使用
        autoSaveAsDefaults()
    }

    /**
     * 自动保存当前配置为默认设置
     */
    private fun autoSaveAsDefaults() {
        // 提取包名前缀
        val packagePrefix = if (property.mainClass.contains(".")) {
            property.mainClass.substringBeforeLast(".")
                .substringBeforeLast(".") // 获取包名前缀，去掉最后两级
        } else {
            "org.example" // 默认值
        }
        
        settings.saveAsDefaults(
            packagePrefix = packagePrefix,
            author = OptionalPropertiesStep.property.authors.firstOrNull() ?: "",
            selectedModules = checkModulePanel.getSelectedModules(),
            templateMirror = property.mirrorIndex
        )
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
            currentLastPart == "Plugin" -> text + "Plugin"
            currentLastPart.isEmpty() -> text.capitalize()
            currentLastPart == property.name?.lowercase() -> text.capitalize()
            currentLastPart.removeSuffix("Plugin").lowercase() == property.name?.lowercase() -> text.capitalize() + "Plugin"
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
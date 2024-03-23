package org.tabooproject.intellij.step

import ai.grazie.utils.capitalize
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.tabooproject.intellij.component.CheckModulePanel
import org.tabooproject.intellij.util.ResourceLoader
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
    "tabooproject.org" to "https://template.tabooproject.org"
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

    companion object {

        var property = ConfigurationProperty()
            private set

        fun refreshTemporaryData() {
            property = ConfigurationProperty()
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

    @OptIn(DelicateCoroutinesApi::class)
    override fun _init() {
        if (inited) return
        GlobalScope.launch {
            coroutineScope {
                checkModulePanel.setModules(ResourceLoader.getModules())
                inited = true
            }
        }
    }

    override fun updateStep() {
        if (property.name == null){
            property.name = context.projectName
        }
    }

    override fun updateDataModel() {
        // 针对控件数据 (AddDeleteListPanel) 无法直接绑定到数据模型的问题，手动导出数据
//        property.modules.apply {
//            clear()
//            addAll(checkModulePanel.export().map { it.id })
//        }
    }

    /**
     * 自动更改主类名。
     * 此函数检查当前的主类名是否符合特定的插件命名模式，并根据匹配情况自动更新主类名。
     * 如果输入的文本与插件名匹配，且现有插件名以该文本为前缀，则将插件名中的该文本替换为新文本。
     *
     * @param text 要替换到主类名中的新文本。
     */
    private fun autoChangeMainClass(text:String) {
        // 如果 mainClassTextField 未初始化，则直接返回
        if (mainClassTextField == null) return

        // 提取重复的字符串操作，减少代码重复并提高性能
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
            if (it.lowercase() == property.name?.lowercase()){
                return@let text.lowercase()
            }else{
                it
            }
        }

        baseClass = baseClass.substringBeforeLast(".")

        // 更新 mainClassTextField 的文本
        mainClassTextField!!.text = "$baseClass.$lastGroup.$newLastPart"

    }
}
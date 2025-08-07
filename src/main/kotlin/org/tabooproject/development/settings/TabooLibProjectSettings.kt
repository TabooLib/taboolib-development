package org.tabooproject.development.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import org.tabooproject.development.step.Module

/**
 * TabooLib 项目设置状态
 * 
 * 持久化保存用户的项目创建偏好，包括包名前缀、作者信息和常用模块
 * 
 * @since 1.31
 */
@State(
    name = "TabooLibProjectSettings",
    storages = [Storage("taboolib-project-settings.xml")]
)
@Service(Service.Level.APP)
class TabooLibProjectSettings : PersistentStateComponent<TabooLibProjectSettings.State> {

    private var state = State()

    /**
     * 获取当前状态
     */
    override fun getState(): State = state

    /**
     * 加载状态
     */
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    /**
     * 获取默认包名前缀
     */
    fun getDefaultPackagePrefix(): String = state.defaultPackagePrefix

    /**
     * 设置默认包名前缀
     */
    fun setDefaultPackagePrefix(prefix: String) {
        state.defaultPackagePrefix = prefix
    }

    /**
     * 获取默认作者
     */
    fun getDefaultAuthor(): String = state.defaultAuthor

    /**
     * 设置默认作者
     */
    fun setDefaultAuthor(author: String) {
        state.defaultAuthor = author
    }

    /**
     * 获取常用模块列表
     */
    fun getFavoriteModules(): List<String> = state.favoriteModules.toList()

    /**
     * 设置常用模块列表
     */
    fun setFavoriteModules(modules: List<String>) {
        state.favoriteModules.clear()
        state.favoriteModules.addAll(modules)
    }

    /**
     * 添加常用模块
     */
    fun addFavoriteModule(moduleId: String) {
        if (!state.favoriteModules.contains(moduleId)) {
            state.favoriteModules.add(moduleId)
        }
    }

    /**
     * 移除常用模块
     */
    fun removeFavoriteModule(moduleId: String) {
        state.favoriteModules.remove(moduleId)
    }

    /**
     * 获取默认模板镜像
     */
    fun getDefaultTemplateMirror(): String = state.defaultTemplateMirror

    /**
     * 设置默认模板镜像
     */
    fun setDefaultTemplateMirror(mirror: String) {
        state.defaultTemplateMirror = mirror
    }
    
    /**
     * 获取默认模块镜像
     */
    fun getDefaultModulesMirror(): String = state.defaultModulesMirror

    /**
     * 设置默认模块镜像
     */
    fun setDefaultModulesMirror(mirror: String) {
        state.defaultModulesMirror = mirror
    }

    /**
     * 保存当前项目配置为默认值
     */
    fun saveAsDefaults(
        packagePrefix: String,
        author: String,
        selectedModules: List<Module>,
        templateMirror: String,
        modulesMirror: String? = null
    ) {
        state.defaultPackagePrefix = packagePrefix
        state.defaultAuthor = author
        state.favoriteModules.clear()
        state.favoriteModules.addAll(selectedModules.map { it.id })
        state.defaultTemplateMirror = templateMirror
        modulesMirror?.let { state.defaultModulesMirror = it }
    }

    /**
     * 设置状态类
     */
    data class State(
        var defaultPackagePrefix: String = "org.example",
        var defaultAuthor: String = System.getProperty("user.name", ""),
        var favoriteModules: MutableList<String> = mutableListOf(),
        var defaultTemplateMirror: String = "github.com",
        var defaultModulesMirror: String = "github.com"
    )

    companion object {
        /**
         * 获取应用级设置实例
         */
        fun getInstance(): TabooLibProjectSettings {
            return service<TabooLibProjectSettings>()
        }
    }
} 
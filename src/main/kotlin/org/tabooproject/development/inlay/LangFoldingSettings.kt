package org.tabooproject.development.inlay

import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.BeanConfigurable

/**
 * TabooLib语言文件折叠设置提供器
 */
class LangFoldingOptionsProvider :
    BeanConfigurable<LangFoldingSettings>(LangFoldingSettings.instance), CodeFoldingOptionsProvider {
    
    init {
        title = "TabooLib"
        checkBox(
            "Language Translation Strings (sendLang)",
            LangFoldingSettings.instance::shouldFoldTranslations,
        ) {
            LangFoldingSettings.instance.shouldFoldTranslations = it
            // 设置变更后刷新折叠
            LangFoldingSettingsListener.refreshAllEditors()
        }
        checkBox(
            "Show color codes in folded text",
            LangFoldingSettings.instance::showColorCodes,
        ) {
            LangFoldingSettings.instance.showColorCodes = it
            // 设置变更后刷新折叠
            LangFoldingSettingsListener.refreshAllEditors()
        }
        checkBox(
            "Highlight valid language keys",
            LangFoldingSettings.instance::showValidLangKeyHighlight,
        ) {
            LangFoldingSettings.instance.showValidLangKeyHighlight = it
        }
    }
}

/**
 * TabooLib语言文件折叠设置
 */
@State(name = "TabooLibLangFoldingSettings", storages = [(Storage("taboolib_dev.xml"))])
class LangFoldingSettings : PersistentStateComponent<LangFoldingSettings.State> {

    data class State(
        var shouldFoldTranslations: Boolean = true,
        var showColorCodes: Boolean = false,
        var showValidLangKeyHighlight: Boolean = false
    )

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    // State mappings
    var shouldFoldTranslations: Boolean
        get() = state.shouldFoldTranslations
        set(value) {
            state.shouldFoldTranslations = value
        }

    var showColorCodes: Boolean
        get() = state.showColorCodes
        set(value) {
            state.showColorCodes = value
        }

    var showValidLangKeyHighlight: Boolean
        get() = state.showValidLangKeyHighlight
        set(value) {
            state.showValidLangKeyHighlight = value
        }

    companion object {
        val instance: LangFoldingSettings
            get() = ApplicationManager.getApplication().getService(LangFoldingSettings::class.java)
    }
}
package org.tabooproject.intellij.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.dsl.builder.*
import org.tabooproject.intellij.component.AddDeleteStringListPanel
import javax.swing.JComponent

data class OptionalProperty(
    var description: String = "",
    val authors: MutableList<String> = mutableListOf(),
    var website: String = "",
    val depends: MutableList<String> = mutableListOf(),
    val softDepends: MutableList<String> = mutableListOf(),
)

class OptionalPropertiesStep : ModuleWizardStep() {

    private val authorsPanel = AddDeleteStringListPanel("Authors", property.authors, "Author", "Add Author")
    private val dependsPanel = AddDeleteStringListPanel("Depends", property.depends, "Depend", "Add Depend")
    private val softDependsPanel = AddDeleteStringListPanel("Soft depends", property.softDepends, "Soft Depend", "Add Soft Depend")

    companion object {

        var property = OptionalProperty()
            private set

        fun refreshTemporaryData() {
            property = OptionalProperty()
        }
    }

    override fun getComponent(): JComponent {
        return panel {
            indent {
                group("Optional Properties", indent = true) {
                    row("Description:") {
                        textField()
                            .bindText(property::description)
                    }
                    row("Author:") {
                        cell(authorsPanel)
                    }
                    row("Website:") {
                        textField()
                            .bindText(property::website)
                    }
                    row("Depends:") {
                        cell(dependsPanel)
                    }
                    row("Soft Depends:") {
                        cell(softDependsPanel)
                    }
                }
            }
        }
    }

    override fun updateDataModel() {
        // 针对控件数据 (AddDeleteListPanel) 无法直接绑定到数据模型的问题，手动导出数据
        property.authors.apply {
            clear()
            addAll(authorsPanel.export())
        }
        property.depends.apply {
            clear()
            addAll(dependsPanel.export())
        }
        property.softDepends.apply {
            clear()
            addAll(softDependsPanel.export())
        }
    }
}
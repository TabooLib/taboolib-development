package org.tabooproject.development.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.profile.codeInspection.ui.addScrollPaneIfNecessary
import com.intellij.ui.dsl.builder.*
import org.tabooproject.development.component.AddDeleteStringListPanel
import javax.swing.JComponent

data class OptionalProperty(
    var description: String = "",
    val authors: MutableList<String> = mutableListOf(),
    var website: String = "",
    val depends: MutableList<String> = mutableListOf(),
    val softDepends: MutableList<String> = mutableListOf(),
)

class OptionalPropertiesStep : ModuleWizardStep() {

    private val authorsPanel = AddDeleteStringListPanel("Authors", property.authors, "Author", "Add Author", 10)
    private val dependsPanel = AddDeleteStringListPanel("Depends", property.depends, "Depend", "Add Depend", 10)
    private val softDependsPanel = AddDeleteStringListPanel("Soft depends", property.softDepends, "Soft Depend", "Add Soft Depend", 10)

    companion object {

        var property = OptionalProperty()
            private set

        fun refreshTemporaryData() {
            property = OptionalProperty()
        }
    }

    override fun getComponent(): JComponent {
        val panel = panel {
            indent {
                group("Optional Properties", indent = true) {
                    row("Description:") {
                        textField()
                            .apply {
                                component.text = property.description
                                component.columns = 30
                            }.onChanged { property.description = it.text }
                    }
                    row("Author:") {
                        cell(authorsPanel)
                    }
                    row("Website:") {
                        textField()
                            .apply {
                                component.text = property.website
                                component.columns = 30
                            }.onChanged { property.website = it.text }
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
        return addScrollPaneIfNecessary(panel)
    }

    override fun updateDataModel() {
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
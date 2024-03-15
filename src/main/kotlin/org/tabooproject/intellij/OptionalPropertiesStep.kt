package org.tabooproject.intellij

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AddDeleteListPanel
import com.intellij.ui.AddEditDeleteListPanel
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import org.tabooproject.intellij.util.Assets
import javax.swing.DefaultListModel

class OptionalPropertiesStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    val pluginDescriptionProperty = propertyGraph.property("")
    val authorProperty = propertyGraph.property(mutableListOf<String>())
    val websiteProperty = propertyGraph.property("")
    val dependsProperty = propertyGraph.property(mutableListOf<String>())
    val softDependsProperty = propertyGraph.property(mutableListOf<String>())

    override fun setupUI(builder: Panel) {
        builder.group("Optional Plugin Properties") {
            row("Description") {
                textField()
                    .bindText(pluginDescriptionProperty)
                    .columns(COLUMNS_MEDIUM)
            }
            row("Author") {
                val authorsPanel = object : AddDeleteListPanel<String>("Authors", authorProperty.get()) {
                    override fun findItemToAdd(): String? {
                        return Messages.showInputDialog(
                            "Enter author name:",
                            "Add Author",
                            Assets.TABOO_16x16,
                            "",
                            null
                        )
                    }
                }
                cell(authorsPanel)
            }
            row("Website") {
                textField()
                    .bindText(websiteProperty)
                    .columns(COLUMNS_MEDIUM)
            }
            row("Depends") {
                val dependsPanel = object : AddDeleteListPanel<String>("Depends", dependsProperty.get()) {
                    override fun findItemToAdd(): String? {
                        return Messages.showInputDialog(
                            "Enter plugin name:",
                            "Add Depends",
                            Assets.TABOO_16x16,
                            "",
                            null
                        )
                    }
                }
                cell(dependsPanel)
            }
            row("Soft Depends") {
                val softDependsPanel = object : AddDeleteListPanel<String>("Soft depends", softDependsProperty.get()) {
                    override fun findItemToAdd(): String? {
                        return Messages.showInputDialog(
                            "Enter plugin name:",
                            "Add Soft Depends",
                            Assets.TABOO_16x16,
                            "",
                            null
                        )
                    }
                }
                cell(softDependsPanel)
            }
        }
    }
}
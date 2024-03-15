package org.tabooproject.intellij.step

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class BuildProperty(
    var groupId: String = "org.example",
    var artifactId: String = "untitled",
    var version: String = "1.0.0",
)

class BuildSystemPropertiesStep : ModuleWizardStep()  {

    companion object {

        var property = BuildProperty()
            private set

        fun refreshTemporaryData() {
            property = BuildProperty()
        }
    }

    override fun getComponent(): JComponent {
        return panel {
            indent {
                group("Build System Properties", indent = true) {
                    row("Group ID:") {
                        textField()
                            .bindText(property::groupId)
                    }
                    row("Artifact ID:") {
                        textField()
                            .bindText(property::artifactId)
                    }
                    row("Version:") {
                        textField()
                            .bindText(property::version)
                    }
                }
            }
        }
    }

    override fun updateDataModel() = Unit
}
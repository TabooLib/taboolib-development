package org.tabooproject.intellij

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TempStep : ModuleWizardStep() {

    override fun getComponent(): JComponent {
        return panel {
            indent {
                group("Temp", indent = true) {

                }
            }
        }
    }

    override fun updateDataModel() = Unit
}
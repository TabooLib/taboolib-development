package org.tabooproject.intellij

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import javax.swing.DefaultComboBoxModel

class SelectJdkStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    private val jdkProperty = propertyGraph.property<Sdk?>(null)

    override fun setupUI(builder: Panel) {
        val availableJdks = ProjectJdkTable.getInstance().allJdks
        val jdkNames = availableJdks.map { it.versionString }

        builder.row("Select JDK:") {
            comboBox(DefaultComboBoxModel(jdkNames.toTypedArray()))
                .bindItem(
                    getter = { jdkProperty.get()?.versionString },
                    setter = { value -> jdkProperty.set(availableJdks.find { it.name == value }) }
                )
        }
    }
}

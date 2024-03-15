package org.tabooproject.intellij

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class BuildSystemPropertiesStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    val groupIdProperty = propertyGraph.property("org.example")

    val artifactIdProperty = propertyGraph.lazyProperty { parent.baseData!!.name }

    val versionProperty = propertyGraph.property("1.0-SNAPSHOT")

    var groupId by groupIdProperty
    var artifactId by artifactIdProperty
    var version by versionProperty

    init {
        artifactIdProperty.dependsOn(parent.baseData!!.nameProperty, ::artifactId)
    }

    override fun setupUI(builder: Panel) {
        builder.group("Build System Properties") {
            row("Group ID:") {
                textField()
                    .bindText(groupIdProperty)
                    .columns(COLUMNS_MEDIUM)
            }
            row("Artifact ID:") {
                textField()
                    .bindText(artifactIdProperty)
                    .columns(COLUMNS_MEDIUM)
            }
            row("Version:") {
                textField()
                    .bindText(versionProperty)
                    .columns(COLUMNS_MEDIUM)
            }
        }
    }
}
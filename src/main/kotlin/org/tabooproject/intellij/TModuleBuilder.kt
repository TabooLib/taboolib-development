package org.tabooproject.intellij

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import org.tabooproject.intellij.util.Assets
import org.tabooproject.intellij.util.NewProjectWizardChainStep.Companion.nextStep
import javax.swing.Icon


class TModuleBuilder : AbstractNewProjectWizardBuilder() {

//    override fun createStep(context: WizardContext) =
//        RootNewProjectWizardStep(context)
//            .nextStep { NewProjectWizardBaseStep(it) }
//            .nextStep { GitNewProjectWizardStep(it) }
//            .nextStep { ConfigurationPropertiesStep(it) }
//            .nextStep { OptionalPropertiesStep(it) }
//            .nextStep { BuildSystemPropertiesStep(it) }
//            .nextStep { SelectJdkStep(it) }

    override fun getDescription(): String {
        return Assets.description
    }

    override fun getNodeIcon(): Icon {
        return Assets.TABOO_16x16
    }

    override fun getPresentableName(): String {
        return "Taboo Integration"
    }

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        return RootNewProjectWizardStep(context)
            .nextStep { NewProjectWizardBaseStep(it) }
            .nextStep { GitNewProjectWizardStep(it) }
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        ConfigurationPropertiesStep.refreshTemporaryData()
        return arrayOf(ConfigurationPropertiesStep())
    }
}
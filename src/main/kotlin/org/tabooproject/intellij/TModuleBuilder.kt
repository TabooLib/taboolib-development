package org.tabooproject.intellij

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import org.tabooproject.intellij.util.Assets
import org.tabooproject.intellij.util.NewProjectWizardChainStep.Companion.nextStep
import javax.swing.Icon


class TModuleBuilder : AbstractNewProjectWizardBuilder() {

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
            // 确定项目的基本信息, 在这两步做完紧跟着的是 createWizardSteps (确认 taboolib 和 gradle 的相关配置)
            .nextStep { NewProjectWizardBaseStep(it) }
            .nextStep { GitNewProjectWizardStep(it) }
            // 项目的创建回调函数
            .nextStep { object : AbstractNewProjectWizardStep(it) {
                override fun setupProject(project: Project) {
                    println("setupProject")
                }
            } }
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        ConfigurationPropertiesStep.refreshTemporaryData()
        OptionalPropertiesStep.refreshTemporaryData()
        BuildSystemPropertiesStep.refreshTemporaryData()
        return arrayOf(ConfigurationPropertiesStep(), OptionalPropertiesStep(), BuildSystemPropertiesStep(), TempStep())
    }

    override fun commitModule(project: Project, model: ModifiableModuleModel?): Module? {
        println("commitModule, sure!")
        return super.commitModule(project, model)
    }
}
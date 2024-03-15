package org.tabooproject.intellij

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.starters.local.StandardAssetsProvider
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.util.io.FileUtil
import org.tabooproject.intellij.step.BuildSystemPropertiesStep
import org.tabooproject.intellij.step.ConfigurationPropertiesStep
import org.tabooproject.intellij.step.OptionalPropertiesStep
import org.tabooproject.intellij.util.Assets
import org.tabooproject.intellij.util.NewProjectWizardChainStep.Companion.nextStep
import java.io.File
import javax.swing.Icon


class ModuleBuilder : AbstractNewProjectWizardBuilder() {

    override fun getDescription(): String {
        return Assets.description
    }

    override fun getNodeIcon(): Icon {
        return Assets.TABOO_16x16
    }

    override fun getPresentableName(): String {
        return "Taboo Integration"
    }

    @Suppress("UnstableApiUsage")
    override fun createStep(context: WizardContext): NewProjectWizardStep {
        return RootNewProjectWizardStep(context)
            // 确定项目的基本信息, 在这两步做完紧跟着的是 createWizardSteps (确认 taboolib 和 gradle 的相关配置)
            .nextStep { NewProjectWizardBaseStep(it) }
            .nextStep { GitNewProjectWizardStep(it) }
            // 资源填充
            .nextStep {
                object : AssetsNewProjectWizardStep(it) {

                    override fun setupAssets(project: Project) {
                        addAssets(StandardAssetsProvider().getGradlewAssets())
                    }
                }
            }
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        ConfigurationPropertiesStep.refreshTemporaryData()
        OptionalPropertiesStep.refreshTemporaryData()
        BuildSystemPropertiesStep.refreshTemporaryData()
        return arrayOf(ConfigurationPropertiesStep(), OptionalPropertiesStep(), BuildSystemPropertiesStep())
    }
}
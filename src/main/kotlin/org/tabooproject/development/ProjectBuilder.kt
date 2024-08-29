package org.tabooproject.development

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.starters.local.StandardAssetsProvider
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.util.ThrowableComputable
import org.tabooproject.development.step.ConfigurationPropertiesStep
import org.tabooproject.development.step.OptionalPropertiesStep
import org.tabooproject.development.util.Assets
import org.tabooproject.development.util.NewProjectWizardChainStep.Companion.nextStep
import javax.swing.Icon


class ProjectBuilder : AbstractNewProjectWizardBuilder() {

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
            // 确定项目的基本信息, 在这两步做完紧跟着的是 createWizardSteps (确认 taboolib 的相关配置)
            .nextStep { NewProjectWizardBaseStep(it) }
            .nextStep { GitNewProjectWizardStep(it) }
            // 资源填充
            .nextStep {
                object : AssetsNewProjectWizardStep(it) {
                    override fun setupAssets(project: Project) {
                        addAssets(StandardAssetsProvider().getGradlewAssets())

                        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                            ThrowableComputable {
                                val directory = project.basePath!!
                                Template.downloadAndUnzipFile(directory)
                            },
                            "Downloading template files", false, project
                        )
                    }
                }
            }
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        return arrayOf(ConfigurationPropertiesStep(wizardContext), OptionalPropertiesStep())
    }

    override fun cleanup() {
        super.cleanup()
        ConfigurationPropertiesStep.refreshTemporaryData()
        OptionalPropertiesStep.refreshTemporaryData()
    }
}
package org.tabooproject.intellij.util

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel

class NewProjectWizardChainStep<S : NewProjectWizardStep> : AbstractNewProjectWizardStep {

    private val step: S
    private val steps: List<NewProjectWizardStep>

    constructor(step: S) : this(step, emptyList())

    private constructor(step: S, descendantSteps: List<NewProjectWizardStep>) : super(step) {
        this.step = step
        this.steps = descendantSteps + step
    }

    fun <NS : NewProjectWizardStep> nextStep(create: (S) -> NS): NewProjectWizardChainStep<NS> {
        return NewProjectWizardChainStep(create(step), steps)
    }

    override fun setupUI(builder: Panel) {
        for (step in steps) {
            step.setupUI(builder)
        }
    }

    override fun setupProject(project: Project) {
        for (step in steps) {
            step.setupProject(project)
        }
    }

    companion object {

        fun <S : NewProjectWizardStep, NS : NewProjectWizardStep> S.nextStep(
            create: (S) -> NS
        ): NewProjectWizardChainStep<NS> {
            return NewProjectWizardChainStep(this).nextStep(create)
        }
    }
}
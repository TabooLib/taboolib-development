package org.tabooproject.development.component

import com.intellij.packageDependencies.ui.TreeModel
import com.intellij.ui.*
import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import org.tabooproject.development.step.ConfigurationPropertiesStep
import org.tabooproject.development.step.Module
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * @author 大阔
 * @since 2024/3/22 04:31
 */
class CheckModuleList(private val displayModuleList: DisplayModuleList) : JScrollPane() {

    val root = CheckedTreeNode("Root").apply {
        isFocusable = false
    }

    private val treeNode = TreeModel(root)

    private val checkBoxList = CheckboxTreeBase(object : CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree?,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            if (value is CheckedTreeNode) {
                if (value.userObject is Module) {
                    val module = value.userObject as Module

                    textRenderer.append(
                        ColoredText.singleFragment(
                            module.name, SimpleTextAttributes(
                                SimpleTextAttributes.STYLE_BOLD,
                                JBColor.BLACK
                            )
                        )
                    )

                    textRenderer.append("   ")

                    textRenderer.append(
                        ColoredText.singleFragment(
                            module.name, SimpleTextAttributes(
                                SimpleTextAttributes.STYLE_PLAIN,
                                JBColor.GRAY
                            )
                        )
                    )
                }
            } else if (value is DefaultMutableTreeNode) {
                textRenderer.append((value as DefaultMutableTreeNode).userObject.toString())
            }
        }
    }, root)

    init {
        checkBoxList.model = treeNode
        checkBoxList.isFocusable = false
        checkBoxList.addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                if (node.userObject !is Module) return
                if (node.isChecked) {
                    ConfigurationPropertiesStep.property.modules.add(node.userObject as Module)
                    displayModuleList.addModule((node.userObject as Module).name)
                } else {
                    ConfigurationPropertiesStep.property.modules.remove(node.userObject as Module)
                    displayModuleList.removeModule((node.userObject as Module).name)
                }
            }
        })
        setFocusable(false)
        autoscrolls = true
        setViewportView(checkBoxList)
    }

    override fun updateUI() {
        treeNode?.reload()
        super.updateUI()
    }
}
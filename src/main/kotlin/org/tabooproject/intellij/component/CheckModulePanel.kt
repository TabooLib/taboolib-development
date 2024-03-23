package org.tabooproject.intellij.component

import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import org.tabooproject.intellij.step.Module
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.tree.DefaultMutableTreeNode

/**
 * @author 大阔
 * @since 2024/3/22 23:28
 */
class CheckModulePanel : JBPanel<CheckModulePanel>() {

    private val displayModuleList = DisplayModuleList()
    private val moduleCheck = CheckModuleList(displayModuleList)

    init {
        layout = GridBagLayout()

        val gbc = GridBagConstraints()


        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 0.1
        gbc.weighty = 0.1
        gbc.gridwidth = 1
        gbc.gridheight = 1
        gbc.gridx = 0
        gbc.gridy = 0
        add(JBLabel("Select Modules"), gbc)
        gbc.gridx = 1
        add(JBLabel("Selected Modules"), gbc)
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.9
        gbc.weighty = 0.9
        add(moduleCheck, gbc)

        gbc.gridx = 1
        add(displayModuleList, gbc)
        preferredSize = Dimension(600, 350)
        minimumSize = Dimension(0, 0)
    }

    // 回调
    fun setModules(modules: Map<String, List<Module>>) {
        modules.map {
            DefaultMutableTreeNode(it.key).apply {
                it.value.forEach {
                    add(CheckedTreeNode(it).apply {
//                        if (ConfigurationPropertiesStep.property.modules.contains(it.name)){
//                            isChecked = true
//                        }
                        isChecked = false
                        isFocusable = false
                    })
                }
                isFocusable = false
            }
        }.forEach {
            moduleCheck.root.add(it)
        }
        moduleCheck.updateUI()
    }


}
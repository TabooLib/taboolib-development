package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.packageDependencies.ui.TreeModel
import com.intellij.ui.*
import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import org.tabooproject.development.step.ConfigurationPropertiesStep
import org.tabooproject.development.step.Module
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * 模块选择复选框树列表
 * 
 * @since 1.31
 */
class CheckModuleList : JScrollPane(), Disposable {

    val root = CheckedTreeNode("Root").apply {
        isFocusable = false
    }

    private val treeNode: TreeModel? = TreeModel(root)
    var onModuleSelectionChanged: ((List<Module>) -> Unit)? = null

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
                            module.desc ?: module.name, SimpleTextAttributes(
                                SimpleTextAttributes.STYLE_PLAIN,
                                JBColor.GRAY
                            )
                        )
                    )
                }
            } else if (value is DefaultMutableTreeNode) {
                textRenderer.append(value.userObject.toString())
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
                } else {
                    ConfigurationPropertiesStep.property.modules.remove(node.userObject as Module)
                }
                // 通知模块选择变更
                onModuleSelectionChanged?.invoke(export())
            }
        })
        setFocusable(false)
        autoscrolls = true
        setViewportView(checkBoxList)

        updateUI()
    }

    /**
     * 设置模块数据
     */
    fun setModules(modules: Map<String, List<Module>>) {
        root.removeAllChildren()
        modules.map {
            DefaultMutableTreeNode(it.key).apply {
                it.value.forEach {
                    add(CheckedTreeNode(it).apply {
                        isChecked = false
                        isFocusable = false
                    })
                }
                isFocusable = false
            }
        }.forEach {
            root.add(it)
        }

        updateUI()
    }

    /**
     * 批量设置模块选中状态
     */
    fun setSelectedModules(moduleIds: List<String>) {
        // 先清除所有选择
        clearAllSelections(root)
        
        // 然后设置指定的模块为选中
        moduleIds.forEach { moduleId ->
            findAndSetModuleSelection(root, moduleId, true)
        }
        treeNode?.reload()
        
        // 通知选择变更
        onModuleSelectionChanged?.invoke(export())
    }

    /**
     * 递归查找并设置模块选中状态
     */
    private fun findAndSetModuleSelection(node: CheckedTreeNode, moduleId: String, selected: Boolean) {
        // 检查当前节点
        if (node.userObject is Module && (node.userObject as Module).id == moduleId) {
            node.isChecked = selected
            return
        }
        
        // 递归检查子节点
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
            for (j in 0 until child.childCount) {
                val treeNode = child.getChildAt(j) as? CheckedTreeNode ?: continue
                if ((treeNode.userObject as? Module)?.id == moduleId) {
                    treeNode.isChecked = selected
                    if (selected) {
                        ConfigurationPropertiesStep.property.modules.add(treeNode.userObject as Module)
                    } else {
                        ConfigurationPropertiesStep.property.modules.remove(treeNode.userObject as Module)
                    }
                }
            }
        }
    }

    /**
     * 清除所有选择
     */
    private fun clearAllSelections(node: CheckedTreeNode) {
        node.isChecked = false
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode ?: continue
            clearAllSelections(child)
        }
    }

    /**
     * 导出当前选中的模块
     */
    fun export(): List<Module> {
        return ConfigurationPropertiesStep.property.modules.toList()
    }

    /**
     * 更新UI显示
     * 
     * 添加null检查以防止初始化期间的NPE
     */
    override fun updateUI() {
        super.updateUI()
        treeNode?.reload()
    }

    override fun dispose() {
        root.removeAllChildren()
        onModuleSelectionChanged = null
    }
}
package org.tabooproject.development.component

import com.intellij.openapi.Disposable
import com.intellij.packageDependencies.ui.TreeModel
import com.intellij.ui.*
import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import com.intellij.util.ui.JBUI
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

                    // 模块名称 - 使用更突出的颜色和字体
                    textRenderer.append(
                        ColoredText.singleFragment(
                            module.name, SimpleTextAttributes(
                                SimpleTextAttributes.STYLE_BOLD,
                                if (selected) JBColor.WHITE else JBColor(0x2D5AA0, 0x4A9EFF) // 蓝色主题
                            )
                        )
                    )

                    // 添加合适的间距
                    textRenderer.append("  ")

                    // 模块描述 - 使用更柔和的灰色
                    val description = module.desc?.let { 
                        if (it.length > 50) "${it.take(50)}..." else it 
                    } ?: "No description"
                    
                    textRenderer.append(
                        ColoredText.singleFragment(
                            description, SimpleTextAttributes(
                                SimpleTextAttributes.STYLE_ITALIC,
                                if (selected) JBColor.LIGHT_GRAY else JBColor.GRAY
                            )
                        )
                    )
                }
            } else if (value is DefaultMutableTreeNode) {
                // 分类标题 - 使用更突出的样式
                textRenderer.append(
                    ColoredText.singleFragment(
                        value.userObject.toString(), SimpleTextAttributes(
                            SimpleTextAttributes.STYLE_BOLD,
                            if (selected) JBColor.WHITE else JBColor(0x1A472A, 0x5FB865) // 绿色分类标题
                        )
                    )
                )
            }
        }
    }, root)

    init {
        checkBoxList.model = treeNode
        checkBoxList.isFocusable = false
        
        // 设置树的行高和样式
        checkBoxList.rowHeight = 24  // 增加行高让内容更易读
        checkBoxList.isRootVisible = false  // 隐藏根节点
        checkBoxList.showsRootHandles = true  // 显示展开/折叠句柄
        
        // 默认展开所有分类节点
        javax.swing.SwingUtilities.invokeLater {
            for (i in 0 until checkBoxList.rowCount) {
                checkBoxList.expandRow(i)
            }
        }
        
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
        
        // 添加双击监听器用于展开/收起分类节点
        checkBoxList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                if (e?.clickCount == 2) {
                    val path = checkBoxList.getPathForLocation(e.x, e.y)
                    if (path != null) {
                        val node = path.lastPathComponent
                        if (node is DefaultMutableTreeNode && node.userObject !is Module) {
                            // 这是一个分类节点，切换展开/收起状态
                            if (checkBoxList.isExpanded(path)) {
                                checkBoxList.collapsePath(path)
                            } else {
                                checkBoxList.expandPath(path)
                            }
                        }
                    }
                }
            }
        })
        
        setFocusable(false)
        autoscrolls = true
        setViewportView(checkBoxList)
        
        // 设置滚动条样式
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED
        border = JBUI.Borders.empty()

        updateUI()
    }

    /**
     * 设置模块数据
     */
    fun setModules(modules: Map<String, List<Module>>) {
        println("CheckModuleList.setModules 被调用，收到 ${modules.size} 个分类")
        modules.forEach { (category, moduleList) ->
            println("  分类 '$category': ${moduleList.size} 个模块")
        }
        
        root.removeAllChildren()
        
        // 按分类名称排序，确保一致的显示顺序
        val sortedModules = modules.toSortedMap()
        
        sortedModules.forEach { (categoryName, moduleList) ->
            val categoryNode = DefaultMutableTreeNode(categoryName).apply {
                isFocusable = false
            }
            
            // 按模块名称排序
            moduleList.sortedBy { it.name }.forEach { module ->
                val moduleNode = CheckedTreeNode(module).apply {
                    isChecked = false
                    isFocusable = false
                }
                categoryNode.add(moduleNode)
            }
            
            root.add(categoryNode)
        }

        // 强制重新构建树模型
        treeNode?.reload()
        updateUI()
        
        // 在EDT中展开所有分类节点以提供更好的用户体验
        javax.swing.SwingUtilities.invokeLater {
            for (i in 0 until checkBoxList.rowCount) {
                checkBoxList.expandRow(i)
            }
            println("CheckModuleList 展开了 ${checkBoxList.rowCount} 行")
        }
    }

    /**
     * 批量设置模块选中状态
     */
    fun setSelectedModules(moduleIds: List<String>) {
        println("CheckModuleList.setSelectedModules: 设置选中模块 ${moduleIds}")
        
        // 先清空ConfigurationPropertiesStep中的模块列表
        ConfigurationPropertiesStep.property.modules.clear()
        
        // 清除所有选择
        clearAllSelections(root)
        
        // 然后设置指定的模块为选中
        moduleIds.forEach { moduleId ->
            findAndSetModuleSelection(root, moduleId, true)
        }
        
        // 强制刷新UI
        treeNode?.reload()
        checkBoxList.repaint()
        checkBoxList.updateUI()
        
        println("CheckModuleList.setSelectedModules: 完成后实际选中模块数量 ${export().size}")
        
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
        // 递归处理所有子节点
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
            // 如果是分类节点，继续递归处理其模块节点
            for (j in 0 until child.childCount) {
                val moduleNode = child.getChildAt(j) as? CheckedTreeNode ?: continue
                moduleNode.isChecked = false
                // 从配置中移除该模块
                if (moduleNode.userObject is Module) {
                    ConfigurationPropertiesStep.property.modules.remove(moduleNode.userObject as Module)
                }
            }
        }
    }

    /**
     * 取消选中单个模块
     */
    fun unselectModule(moduleId: String) {
        println("CheckModuleList.unselectModule: 取消选中模块 ${moduleId}")
        
        // 查找并取消选中指定模块
        findAndSetModuleSelection(root, moduleId, false)
        
        // 刷新UI
        treeNode?.reload()
        checkBoxList.repaint()
        
        println("CheckModuleList.unselectModule: 完成后实际选中模块数量 ${export().size}")
        
        // 通知选择变更
        onModuleSelectionChanged?.invoke(export())
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
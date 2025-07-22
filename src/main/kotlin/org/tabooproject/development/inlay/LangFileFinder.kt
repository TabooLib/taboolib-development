package org.tabooproject.development.inlay

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * TabooLib 语言文件查找器
 * 
 * 负责在多模块项目中查找语言文件，优先查找 zh_cn.yml
 * 
 * @since 1.32
 */
object LangFileFinder {
    
    /**
     * 查找语言文件的可能路径
     */
    private val LANGUAGE_PATHS = listOf(
        "resources/lang/zh_cn.yml",
        "src/main/resources/lang/zh_cn.yml",
        "main/resources/lang/zh_cn.yml",
        "resources/lang/en_us.yml",
        "src/main/resources/lang/en_us.yml",
        "main/resources/lang/en_us.yml"
    )
    
    /**
     * 在项目中查找语言文件
     * 
     * @param project 当前项目
     * @param contextElement 上下文元素，用于确定当前模块
     * @return 找到的语言文件列表，按优先级排序
     */
    fun findLanguageFiles(project: Project, contextElement: PsiElement): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        
        // 1. 首先从当前元素所在模块查找
        val currentModule = findModuleForElement(contextElement)
        if (currentModule != null) {
            result.addAll(findLanguageFilesInModule(currentModule))
        }
        
        // 2. 然后从项目的所有其他模块查找
        val allModules = ModuleManager.getInstance(project).modules
        for (module in allModules) {
            if (module != currentModule) {
                result.addAll(findLanguageFilesInModule(module))
            }
        }
        
        return result.distinctBy { it.path }
    }
    
    /**
     * 在指定模块中查找语言文件
     * 
     * @param module 目标模块
     * @return 找到的语言文件列表
     */
    private fun findLanguageFilesInModule(module: Module): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val moduleRootManager = ModuleRootManager.getInstance(module)
        
        // 遍历模块的所有内容根目录
        for (contentRoot in moduleRootManager.contentRoots) {
            for (langPath in LANGUAGE_PATHS) {
                val langFile = contentRoot.findFileByRelativePath(langPath)
                if (langFile != null && langFile.exists() && !langFile.isDirectory) {
                    result.add(langFile)
                }
            }
        }
        
        return result
    }
    
    /**
     * 获取首选的语言文件
     * 
     * @param project 当前项目
     * @param contextElement 上下文元素
     * @return 首选的语言文件，如果未找到返回 null
     */
    fun getPreferredLanguageFile(project: Project, contextElement: PsiElement): VirtualFile? {
        val files = findLanguageFiles(project, contextElement)
        
        // 优先返回 zh_cn.yml
        return files.firstOrNull { it.name == "zh_cn.yml" }
            ?: files.firstOrNull() // 如果没有找到 zh_cn.yml，返回第一个找到的
    }
    
    /**
     * 查找元素所在的模块
     * 
     * @param element PSI 元素
     * @return 元素所在的模块，如果未找到返回 null
     */
    private fun findModuleForElement(element: PsiElement): Module? {
        val virtualFile = element.containingFile?.virtualFile ?: return null
        val project = element.project
        
        val moduleManager = ModuleManager.getInstance(project)
        return moduleManager.modules.firstOrNull { module ->
            val moduleRootManager = ModuleRootManager.getInstance(module)
            moduleRootManager.fileIndex.isInContent(virtualFile)
        }
    }
    
    /**
     * 获取语言文件的缓存键
     * 
     * @param file 语言文件
     * @return 缓存键
     */
    fun getCacheKey(file: VirtualFile): String {
        return "${file.path}:${file.modificationStamp}"
    }
} 
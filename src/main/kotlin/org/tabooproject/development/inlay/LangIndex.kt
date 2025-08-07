package org.tabooproject.development.inlay

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.File

/**
 * TabooLib语言索引
 * 
 * 用于查找和索引语言文件
 * 
 * @since 1.42
 */
object LangIndex {

    /**
     * 查找当前项目中的所有语言文件
     *
     * @param project 项目实例
     * @return 语言文件列表
     */
    fun findProjectLangFiles(project: Project): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        
        // 在多个可能的路径下查找语言文件
        for (pathPattern in LangConstants.LANG_PATHS) {
            val path = pathPattern.replace('/', File.separatorChar)
            val files = FilenameIndex.getAllFilesByExt(project, "yml", GlobalSearchScope.projectScope(project))
            
            files.forEach { file ->
                if (file.path.contains(path) && LangFiles.isLangFile(file)) {
                    result.add(file)
                }
            }
            
            // 也支持yaml扩展名
            val yamlFiles = FilenameIndex.getAllFilesByExt(project, "yaml", GlobalSearchScope.projectScope(project))
            yamlFiles.forEach { file ->
                if (file.path.contains(path) && LangFiles.isLangFile(file)) {
                    result.add(file)
                }
            }
        }
        
        return result
    }
    
    /**
     * 查找与给定元素相关的语言文件
     *
     * @param element PSI元素
     * @return 语言文件列表
     */
    fun findLangFiles(element: PsiElement): List<VirtualFile> {
        val project = element.project
        return findProjectLangFiles(project)
    }
    
    /**
     * 获取项目的默认语言文件
     *
     * @param project 项目实例
     * @return 默认语言文件，找不到则返回null
     */
    fun getProjectDefaultLangFile(project: Project): VirtualFile? {
        val langFiles = findProjectLangFiles(project)
        return langFiles.find { LangFiles.isDefaultLocale(it) }
    }
    
    /**
     * 获取项目默认语言文件的所有翻译
     *
     * @param project 项目实例
     * @return 翻译列表
     */
    fun getProjectDefaultLangs(project: Project): List<Lang> {
        val defaultLangFile = getProjectDefaultLangFile(project) ?: return emptyList()
        return getLangs(project, defaultLangFile)
    }
    
    /**
     * 从给定文件获取所有翻译
     *
     * @param project 项目实例
     * @param file 语言文件
     * @return 翻译列表
     */
    fun getLangs(project: Project, file: VirtualFile): List<Lang> {
        return LangParser.parseLangFile(file)
    }
    
    /**
     * 根据键获取翻译
     *
     * @param project 项目实例
     * @param langKey 翻译键
     * @return 翻译值，找不到则返回null
     */
    fun getTranslation(project: Project, langKey: String): String? {
        val defaultLangFile = getProjectDefaultLangFile(project) ?: return null
        val langs = getLangs(project, defaultLangFile)
        return langs.find { it.key == langKey }?.value
    }
} 
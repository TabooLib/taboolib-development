package org.tabooproject.development.inlay

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.tabooproject.development.isSendLangCall
import java.util.concurrent.ConcurrentHashMap

/**
 * TabooLib 语言使用情况分析器
 * 
 * 分析项目中语言键的使用情况，提供未使用语言键检测和使用位置查找功能
 * 
 * @since 1.42
 */
object LangUsageAnalyzer {
    
    // 缓存使用情况分析结果
    private val usageCache = ConcurrentHashMap<String, LangUsageResult>()
    
    /**
     * 语言使用结果
     */
    data class LangUsageResult(
        val usedKeys: Set<String>,
        val keyUsages: Map<String, List<LangUsageLocation>>
    )
    
    /**
     * 语言使用位置
     */
    data class LangUsageLocation(
        val file: VirtualFile,
        val offset: Int,
        val length: Int,
        val contextText: String
    )
    
    /**
     * 分析项目中的语言键使用情况
     */
    fun analyzeLangUsage(project: Project): LangUsageResult {
        val projectPath = project.basePath ?: return LangUsageResult(emptySet(), emptyMap())
        
        // 检查缓存
        usageCache[projectPath]?.let { return it }
        
        val usedKeys = mutableSetOf<String>()
        val keyUsages = mutableMapOf<String, MutableList<LangUsageLocation>>()
        
        // 搜索所有Kotlin文件中的sendLang调用
        val kotlinFiles = findKotlinFiles(project)
        
        kotlinFiles.forEach { file ->
            val psiFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return@forEach
            
            // 查找所有sendLang调用
            val callExpressions = PsiTreeUtil.findChildrenOfType(psiFile, KtCallExpression::class.java)
            
            callExpressions.forEach { callExpression ->
                if (isSendLangCall(callExpression)) {
                    val langKey = extractLangKey(callExpression)
                    if (langKey != null) {
                        usedKeys.add(langKey)
                        
                        val usageLocation = LangUsageLocation(
                            file = file,
                            offset = callExpression.textOffset,
                            length = callExpression.textLength,
                            contextText = getContextText(callExpression)
                        )
                        
                        keyUsages.computeIfAbsent(langKey) { mutableListOf() }.add(usageLocation)
                    }
                }
            }
        }
        
        val result = LangUsageResult(usedKeys, keyUsages)
        usageCache[projectPath] = result
        
        return result
    }
    
    /**
     * 获取指定语言键的所有使用位置
     */
    fun findUsages(project: Project, langKey: String): List<LangUsageLocation> {
        val usageResult = analyzeLangUsage(project)
        return usageResult.keyUsages[langKey] ?: emptyList()
    }
    
    /**
     * 检查语言键是否被使用
     */
    fun isLangKeyUsed(project: Project, langKey: String): Boolean {
        val usageResult = analyzeLangUsage(project)
        return langKey in usageResult.usedKeys
    }
    
    /**
     * 获取所有未使用的语言键
     */
    fun getUnusedLangKeys(project: Project): List<String> {
        val usageResult = analyzeLangUsage(project)
        val allKeys = LangIndex.getProjectDefaultLangs(project).map { it.key }.toSet()
        return (allKeys - usageResult.usedKeys).toList()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache(project: Project? = null) {
        if (project != null) {
            val projectPath = project.basePath
            if (projectPath != null) {
                usageCache.remove(projectPath)
            }
        } else {
            usageCache.clear()
        }
    }
    
    /**
     * 查找项目中的所有Kotlin文件
     */
    private fun findKotlinFiles(project: Project): List<VirtualFile> {
        val kotlinFiles = mutableListOf<VirtualFile>()
        val scope = GlobalSearchScope.projectScope(project)
        
        // 使用FilenameIndex搜索所有.kt文件
        try {
            val ktFiles = com.intellij.psi.search.FilenameIndex.getAllFilesByExt(project, "kt", scope)
            kotlinFiles.addAll(ktFiles)
        } catch (e: Exception) {
            // 如果搜索失败，返回空列表
        }
        
        return kotlinFiles
    }
    
    /**
     * 从sendLang调用中提取语言键
     */
    private fun extractLangKey(callExpression: KtCallExpression): String? {
        val arguments = callExpression.valueArgumentList?.arguments
        if (arguments.isNullOrEmpty()) return null
        
        val firstArg = arguments[0].getArgumentExpression()
        if (firstArg !is KtStringTemplateExpression) return null
        
        return extractStringLiteral(firstArg)
    }
    
    /**
     * 提取字符串字面量
     */
    private fun extractStringLiteral(stringTemplate: KtStringTemplateExpression): String? {
        val fullText = stringTemplate.text
        
        // 处理带引号的情况
        if ((fullText.startsWith("\"") && fullText.endsWith("\"")) || 
            (fullText.startsWith("'") && fullText.endsWith("'"))) {
            return fullText.substring(1, fullText.length - 1)
        }
        
        // 使用entries提取内容
        if (stringTemplate.entries.size == 1) {
            val entry = stringTemplate.entries[0]
            if (entry is org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry) {
                return entry.text
            }
        }
        
        return null
    }
    
    /**
     * 获取上下文文本
     */
    private fun getContextText(callExpression: KtCallExpression): String {
        val line = callExpression.containingFile.text
            .substring(0, callExpression.textOffset)
            .count { it == '\n' } + 1
        
        return "第 $line 行: ${callExpression.text}"
    }
}
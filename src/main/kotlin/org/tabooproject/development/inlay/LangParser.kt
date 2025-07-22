package org.tabooproject.development.inlay

import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/**
 * TabooLib语言文件解析器
 * 
 * 负责解析YAML格式的语言文件
 * 
 * @since 1.42
 */
object LangParser {
    
    /**
     * 缓存已解析的语言文件
     * 使用 LinkedHashMap 实现 LRU 缓存
     */
    private val langCache = object : LinkedHashMap<String, Map<String, String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, String>>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    
    /**
     * 最大缓存条目数，防止内存泄露
     */
    private const val MAX_CACHE_SIZE = 100
    
    /**
     * 解析语言文件
     * 
     * @param file 要解析的语言文件
     * @param forceReload 是否强制重新加载
     * @return 语言条目列表
     */
    @Synchronized
    fun parseLangFile(file: VirtualFile, forceReload: Boolean = false): List<Lang> {
        val cacheKey = getCacheKey(file)
        
        // 如果需要强制重新加载或者缓存中没有，则解析文件
        val langMap = if (forceReload || !langCache.containsKey(cacheKey)) {
            parseFile(file).also { 
                if (it.isNotEmpty()) { // 只缓存有效数据
                    langCache[cacheKey] = it 
                }
            }
        } else {
            langCache[cacheKey] ?: emptyMap()
        }
        
        return langMap.map { (key, value) -> Lang(key, value) }
    }
    
    /**
     * 获取缓存键
     * 
     * @param file 语言文件
     * @return 缓存键
     */
    private fun getCacheKey(file: VirtualFile): String {
        return "${file.path}:${file.modificationStamp}"
    }
    
    /**
     * 解析文件内容
     * 
     * @param file 要解析的文件
     * @return 解析后的键值对映射
     */
    private fun parseFile(file: VirtualFile): Map<String, String> {
        if (!file.exists() || file.isDirectory) {
            return emptyMap()
        }
        
        return try {
            val content = String(file.contentsToByteArray(), Charsets.UTF_8)
            parseYamlContent(content)
        } catch (e: IOException) {
            emptyMap()
        }
    }
    
    /**
     * 解析YAML内容
     * 
     * @param content YAML内容
     * @return 扁平化的键值对映射
     */
    private fun parseYamlContent(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = content.lines()
        
        parseYamlLines(lines, result, emptyList())
        return result
    }
    
    /**
     * 递归解析YAML行
     * 
     * @param lines 行列表
     * @param result 结果映射
     * @param parentKeys 父级键路径
     */
    private fun parseYamlLines(
        lines: List<String>,
        result: MutableMap<String, String>,
        parentKeys: List<String>
    ) {
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i].trimEnd()
            
            // 跳过空行和注释
            if (line.isBlank() || line.trimStart().startsWith('#')) {
                i++
                continue
            }
            
            val trimmedLine = line.trimStart()
            val currentIndent = line.length - trimmedLine.length
            
            // 解析键值对
            val colonIndex = trimmedLine.indexOf(':')
            if (colonIndex == -1) {
                i++
                continue
            }
            
            val key = trimmedLine.substring(0, colonIndex).trim()
            val value = trimmedLine.substring(colonIndex + 1).trim()
            
            val currentKeys = parentKeys + key
            val fullKey = currentKeys.joinToString(".")
            
            when {
                // 处理引号包围的值
                value.startsWith('"') || value.startsWith('\'') -> {
                    val quote = value.first()
                    val cleanValue = if (value.length >= 2 && value.endsWith(quote)) {
                        value.substring(1, value.length - 1)
                    } else {
                        value.substring(1)
                    }
                    result[fullKey] = cleanValue
                    i++
                }
                
                // 处理简单值（非空，非嵌套）
                value.isNotEmpty() -> {
                    result[fullKey] = value
                    i++
                }
                
                // 处理嵌套对象
                else -> {
                    // 收集所有子行
                    val childLines = mutableListOf<String>()
                    i++
                    
                    while (i < lines.size) {
                        val nextLine = lines[i]
                        val nextTrimmed = nextLine.trimStart()
                        val nextIndent = nextLine.length - nextTrimmed.length
                        
                        // 空行或注释行也添加到子行中
                        if (nextLine.isBlank() || nextTrimmed.startsWith('#')) {
                            childLines.add(nextLine)
                            i++
                            continue
                        }
                        
                        // 如果缩进级别小于等于当前级别，说明退出了当前嵌套
                        if (nextIndent <= currentIndent) {
                            break
                        }
                        
                        childLines.add(nextLine)
                        i++
                    }
                    
                    // 递归解析子行
                    parseYamlLines(childLines, result, currentKeys)
                }
            }
        }
    }
    
    /**
     * 清除文件缓存
     * 
     * @param file 要清除缓存的文件，如果为null则清除所有缓存
     */
    @Synchronized
    fun clearCache(file: VirtualFile? = null) {
        if (file == null) {
            langCache.clear()
        } else {
            // 移除该文件的所有版本缓存
            val filePath = file.path
            val keysToRemove = langCache.keys.filter { key ->
                key.startsWith("$filePath:")
            }
            keysToRemove.forEach { key ->
                langCache.remove(key)
            }
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): String {
        return "LangParser Cache: ${langCache.size}/$MAX_CACHE_SIZE entries"
    }
} 
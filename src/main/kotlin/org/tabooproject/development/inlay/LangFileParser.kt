package org.tabooproject.development.inlay

import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/**
 * TabooLib 语言文件解析器
 * 
 * 负责解析 YAML 格式的语言文件，支持嵌套键值对访问
 * 
 * @since 1.32
 */
object LangFileParser {
    
    /**
     * 解析语言文件内容
     * 
     * @param file 语言文件
     * @return 解析后的键值对映射
     */
    fun parseLanguageFile(file: VirtualFile): Map<String, String> {
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
     * 解析 YAML 内容为扁平化键值对
     * 
     * @param content YAML 文件内容
     * @return 扁平化的键值对映射
     */
    private fun parseYamlContent(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = content.lines()
        
        parseYamlLines(lines, result, emptyList())
        return result
    }
    
    /**
     * 递归解析 YAML 行
     * 
     * @param lines 待解析的行列表
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
     * 根据键获取语言文本
     * 
     * @param langMap 语言映射
     * @param key 语言键
     * @return 对应的语言文本，如果未找到返回 null
     */
    fun getLanguageText(langMap: Map<String, String>, key: String): String? {
        return langMap[key]
    }
} 
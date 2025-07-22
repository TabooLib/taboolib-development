package org.tabooproject.development.inlay

import com.intellij.openapi.vfs.VirtualFile

/**
 * TabooLib语言条目
 * 
 * 表示一个翻译键值对
 * 
 * @property key 翻译键
 * @property value 翻译值
 * @since 1.42
 */
data class Lang(val key: String, val value: String) {
    val trimmedKey = key.trim()
}

/**
 * TabooLib语言文件常量
 * 
 * @since 1.42
 */
object LangConstants {
    /** 默认语言文件名 */
    const val DEFAULT_LOCALE = "zh_cn"
    
    /** 英文语言文件名 */
    const val EN_LOCALE = "en_us"
    
    /** 支持的语言文件扩展名 */
    val SUPPORTED_EXTENSIONS = setOf("yml", "yaml")
    
    /** 可能的语言文件路径 */
    val LANG_PATHS = listOf(
        "resources/lang",
        "src/main/resources/lang",
        "main/resources/lang"
    )
}

/**
 * TabooLib语言文件工具类
 * 
 * @since 1.42
 */
object LangFiles {
    
    /**
     * 判断文件是否为语言文件
     * 
     * @param file 要检查的文件
     * @return 如果是语言文件返回true
     */
    fun isLangFile(file: VirtualFile?): Boolean {
        if (file == null || file.isDirectory) {
            return false
        }
        
        val fileName = file.name.lowercase()
        val extension = file.extension?.lowercase() ?: ""
        val parentPath = file.parent?.path?.lowercase() ?: ""
        
        return extension in LangConstants.SUPPORTED_EXTENSIONS && 
               parentPath.contains("lang")
    }
    
    /**
     * 获取文件的语言区域
     * 
     * @param file 语言文件
     * @return 语言区域代码，例如zh_cn
     */
    fun getLocale(file: VirtualFile?): String? {
        return file?.nameWithoutExtension?.lowercase()
    }
    
    /**
     * 判断是否为默认语言文件
     * 
     * @param file 要检查的文件
     * @return 如果是默认语言文件返回true
     */
    fun isDefaultLocale(file: VirtualFile?): Boolean {
        return getLocale(file) == LangConstants.DEFAULT_LOCALE
    }
} 
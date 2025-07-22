package org.tabooproject.development.util

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import groovy.util.logging.Slf4j
import kotlinx.serialization.json.*
import org.jetbrains.annotations.NonNls
import org.tabooproject.development.readFromUrl
import org.tabooproject.development.step.Module
import java.io.IOException
import java.security.MessageDigest

/**
 * @author 大阔
 * @since 2024/3/23 00:10
 */
@Slf4j
object ResourceLoader {

    val logger = getLogger<ResourceLoader>()

    /**
     * 可用的模块数据镜像列表
     * 按照访问速度和稳定性排序：Gitee(稍微落后也许) > GitHub
     */
    val MODULES_MIRRORS = linkedMapOf(
        "gitee.com" to "https://gitee.com/Ray_Hughes/taboolib-development/raw/master/src/main/resources/Resources/",
        "github.com" to "https://raw.githubusercontent.com/TabooLib/taboolib-sdk/idea-template/Resources/"
    )

    /**
     * 当前使用的镜像URL
     */
    var currentMirrorUrl = MODULES_MIRRORS.values.first() // 使用第一个镜像（gitee.com）

    /**
     * 缓存的模块数据
     */
    @Volatile
    var cacheJson: JsonObject? = null
        set(value) {
            field = value
            // 更新 PropertiesComponent 缓存
            value?.let { 
                PropertiesComponent.getInstance().setValue("modules_json", it.toString())
            }
        }

    /**
     * 上次网络检查时间
     */
    @Volatile
    private var lastNetworkCheckTime = 0L
    private const val NETWORK_CHECK_INTERVAL_MS = 30_000L // 30秒内不重复网络检查

    init {
        // 尝试从设置中加载默认模块镜像
        try {
            val settings = org.tabooproject.development.settings.TabooLibProjectSettings.getInstance()
            val savedMirror = settings.getDefaultModulesMirror()
            if (MODULES_MIRRORS.containsKey(savedMirror)) {
                currentMirrorUrl = MODULES_MIRRORS[savedMirror]!!
                logger.info { "从设置加载模块镜像: $savedMirror -> $currentMirrorUrl" }
            }
        } catch (e: Exception) {
            logger.info { "无法加载保存的模块镜像设置，使用默认镜像: ${e.message}" }
        }
        
        // 初始化时从缓存加载
        loadFromCache()
    }

    /**
     * 从 PropertiesComponent 加载缓存
     */
    private fun loadFromCache() {
        try {
            PropertiesComponent.getInstance().getValue("modules_json")?.let { cached ->
                cacheJson = Json.parseToJsonElement(cached).jsonObject
                logger.info { "从本地缓存加载模块数据成功" }
            }
        } catch (e: Exception) {
            logger.info { "加载本地缓存失败: ${e.message}" }
        }
    }

    /**
     * 设置使用的镜像
     */
    fun setMirror(mirrorKey: String) {
        currentMirrorUrl = MODULES_MIRRORS[mirrorKey] ?: MODULES_MIRRORS.values.first()
        logger.info { "切换到镜像: $mirrorKey -> $currentMirrorUrl" }
    }

    /**
     * 获取所有可用镜像
     */
    fun getAvailableMirrors(): Map<String, String> = MODULES_MIRRORS

    /**
     * 尝试从多个镜像下载文件
     */
    private fun tryDownloadFromMirrors(fileName: String): String? {
        // 首先尝试当前镜像
        logger.info { "尝试从当前镜像下载: $currentMirrorUrl$fileName" }
        var result = readFromUrl(currentMirrorUrl + fileName)
        if (result != null) return result

        // 如果当前镜像失败，尝试其他镜像
        MODULES_MIRRORS.forEach { (mirrorName, mirrorUrl) ->
            if (mirrorUrl != currentMirrorUrl) {
                logger.info { "尝试从备用镜像 $mirrorName 下载: $mirrorUrl$fileName" }
                result = readFromUrl(mirrorUrl + fileName)
                if (result != null) {
                    logger.info { "成功从镜像 $mirrorName 下载，切换当前镜像" }
                    currentMirrorUrl = mirrorUrl
                    return result
                }
            }
        }

        logger.info { "所有镜像均无法访问文件: $fileName" }
        return null
    }

    fun loadModules() {
        val loadLocalModulesToJson = loadLocalModulesToJson()
        
        // 防抖：避免频繁网络检查
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNetworkCheckTime < NETWORK_CHECK_INTERVAL_MS) {
            logger.info { "距离上次网络检查时间过短，跳过" }
            return
        }
        lastNetworkCheckTime = currentTime

        try {
            val remoteSha1 = tryDownloadFromMirrors("Modules.json.sha1")

            if (remoteSha1 == null) {
                logger.info { "网络失败,使用本地文件和缓存" }
                fallbackToLocal(loadLocalModulesToJson)
                return
            }

            if (cacheJson != null) {
                val cacheJsonSha1 = sha1(cacheJson.toString())
                if (remoteSha1.trim() == cacheJsonSha1.trim()) {
                    logger.info { "缓存数据正确,不进行更新" }
                    return
                }
            }

            sha1(loadLocalModulesToJson.toString()).also {
                if (remoteSha1.trim() == it.trim()) {
                    logger.info { "校验通过,本地文件与远程文件一致,无需更新" }
                    if (cacheJson == null) {
                        logger.info { "缓存为空,将本地文件写入缓存" }
                        cacheJson = loadLocalModulesToJson
                    }
                    return
                }
            }
            
            logger.info { "校验失败,本地文件与远程文件不一致,开始检查缓存" }
            if (cacheJson == null) {
                logger.info { "缓存为空,开始下载远程文件并更新缓存" }
                try {
                    cacheJson = downloadRemoteJson()
                } catch (e: Exception) {
                    logger.info { "下载远程文件失败: ${e.message}, 使用本地文件" }
                    fallbackToLocal(loadLocalModulesToJson)
                }
            } else {
                logger.info { "开始检查缓存" }
                val cacheSha1 = sha1(cacheJson.toString())
                if (cacheSha1.trim() == remoteSha1.trim()) {
                    logger.info { "缓存与远程文件一致,无需更新" }
                } else {
                    logger.info { "缓存与远程文件不一致,开始下载远程文件并更新缓存" }
                    try {
                        cacheJson = downloadRemoteJson()
                    } catch (e: Exception) {
                        logger.info { "下载远程文件失败: ${e.message}, 继续使用现有缓存" }
                    }
                }
            }
        } catch (e: Exception) {
            logger.info { "网络请求失败: ${e.message}, 使用本地文件和缓存" }
            fallbackToLocal(loadLocalModulesToJson)
        }
    }

    /**
     * 网络失败时的备用方案
     */
    private fun fallbackToLocal(localJson: JsonObject) {
        if (cacheJson == null) {
            logger.info { "使用本地文件作为缓存" }
            cacheJson = localJson
        } else {
            logger.info { "使用现有缓存" }
        }
    }

    fun downloadRemoteJson(): JsonObject {
        val jsonString = tryDownloadFromMirrors("Modules.json") ?: throw IOException("Failed to get modules from all mirrors")
        return Json.parseToJsonElement(jsonString).jsonObject
    }

    fun getModules(): Map<String, List<Module>> {
        // 如果缓存为空，尝试使用本地文件
        if (cacheJson == null) {
            logger.info { "缓存为空，直接使用本地文件" }
            try {
                cacheJson = loadLocalModulesToJson()
                logger.info { "成功加载本地模块文件" }
            } catch (e: Exception) {
                logger.info { "加载本地模块文件失败: ${e.message}" }
                e.printStackTrace()
                throw e
            }
        }

        val jsonToUse = cacheJson ?: error("无法加载模块数据")
        logger.info { "准备解析模块数据，JSON数据长度: ${jsonToUse.toString().length}" }

        val result = parseModules(jsonToUse)
        logger.info { "解析模块完成，共 ${result.size} 个分类，${result.values.sumOf { it.size }} 个模块" }

        // 输出每个分类的详细信息
        result.forEach { (category, modules) ->
            logger.info { "分类 '$category': ${modules.size} 个模块" }
        }

        return result
    }

    fun parseModules(json: JsonObject): Map<String, List<Module>> {

        val modules = json["modules"]!!.jsonObject
        return modules.keys.associateWith { key ->
            modules[key]!!.jsonArray.map {
                it.jsonObject.let {
                    Module(
                        // 不知道为什么有时候会带引号
                        it["name"]!!.jsonPrimitive.toString().replace("\"", ""),
                        it["desc"]!!.jsonPrimitive.toString().replace("\"", ""),
                        it["id"]!!.jsonPrimitive.toString().replace("\"", "")
                    )
                }
            }
        }

    }


    fun loadLocalModulesToJson(): JsonObject {
        return this.javaClass.getResourceAsStream("/Resources/Modules.json").use {
            it.bufferedReader().use {
                Json.parseToJsonElement(it.readText()).jsonObject
            }
        }
    }

    fun sha1(str: String): String {
        // 不知道怎么格式化 手动格式化
        val formatString = str.replace("\\\"", "")
            .replace(" ", "")
            .replace("\n", "")
        var sha: MessageDigest? = null
        try {
            sha = MessageDigest.getInstance("SHA")
        } catch (e: Exception) {
            println(e.toString())
            e.printStackTrace()
            return ""
        }

        val byteArray: ByteArray = formatString.toByteArray(Charsets.UTF_8)
        val md5Bytes = sha.digest(byteArray)
        val hexValue = StringBuffer()
        for (i in md5Bytes.indices) {
            val `val` = (md5Bytes[i].toInt()) and 0xff
            if (`val` < 16) {
                hexValue.append("0")
            }
            hexValue.append(Integer.toHexString(`val`))
        }
        return hexValue.toString()
    }

}

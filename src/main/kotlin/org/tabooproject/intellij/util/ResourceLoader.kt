package org.tabooproject.intellij.util

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import groovy.util.logging.Slf4j
import kotlinx.serialization.json.*
import org.jetbrains.annotations.NonNls
import org.tabooproject.intellij.step.Module
import java.net.URL
import java.security.MessageDigest

/**
 * @author 大阔
 * @since 2024/3/23 00:10
 */
@Slf4j
object ResourceLoader {

    val logger = getLogger<ResourceLoader>()

    val url = "https://raw.githubusercontent.com/TabooLib/taboolib-sdk/idea-template/Resources/"

    var cacheJson = PropertiesComponent.getInstance().getValue("modules_json")?.let<@NonNls String, JsonObject> {
        Json.parseToJsonElement(it).jsonObject
    }

    fun loadModules() {
        val moduleSha1URL = URL(url.plus("Modules.json.sha1"))

        val loadLocalModulesToJson = loadLocalModulesToJson()

        if (cacheJson != null) {
            val cacheJsonSha1 = sha1(cacheJson.toString())
            val remoteSha1 = moduleSha1URL.openStream().use { it.bufferedReader().use { it.readText() } }
            if (remoteSha1.trim() == cacheJsonSha1.trim()) {
                logger.info { "缓存数据正确,不进行更新" }
                return
            }
        }


        sha1(loadLocalModulesToJson.toString()).also {
            val remoteSha1 = moduleSha1URL.openStream().use { it.bufferedReader().use { it.readText() } }
            if (remoteSha1.trim() == it.trim()) {
                logger.info { "校验通过,本地文件与远程文件一致,无需更新" }
                if (cacheJson == null) {
                    logger.info { "缓存为空,将本地文件写入缓存" }
                    PropertiesComponent.getInstance().setValue("modules_json", loadLocalModulesToJson.toString())
                    cacheJson = loadLocalModulesToJson
                }
            } else {
                logger.info { "校验失败,本地文件与远程文件不一致,开始检查缓存" }
                if (cacheJson == null) {
                    logger.info { "缓存为空,开始下载远程文件并更新缓存" }
                    cacheJson = downloadRemoteJson()
                    PropertiesComponent.getInstance().setValue("modules_json", cacheJson.toString())
                } else {
                    logger.info { "开始检查缓存" }
                    val cacheSha1 = sha1(cacheJson.toString())
                    if (cacheSha1.trim() == remoteSha1.trim()) {
                        logger.info { "缓存与远程文件一致,无需更新" }
                    } else {
                        logger.info { "缓存与远程文件不一致,开始下载远程文件并更新缓存" }
                        cacheJson = downloadRemoteJson()
                        PropertiesComponent.getInstance().setValue("modules_json", downloadRemoteJson().toString())
                    }
                }
            }
        }
    }

    fun downloadRemoteJson(): JsonObject {
        val moduleURL = URL(url.plus("Modules.json"))
        return moduleURL.openStream().use {
            it.bufferedReader().use { it.readText() }.let { Json.parseToJsonElement(it).jsonObject }
        }
    }

    fun getModules(): Map<String, List<Module>> {
        loadModules()
        return parseModules(cacheJson ?: error("加载失败"))
    }

    fun parseModules(json: JsonObject): Map<String, List<Module>> {

        val modules = json.get("modules")!!.jsonObject
        return modules.keys.map { key ->
            key to modules.get(key)!!.jsonArray.map {
                it.jsonObject.let {
                    Module(
                        // 不知道为什么有时候会带引号
                        it.get("name")!!.jsonPrimitive.toString().replace("\"", ""),
                        it.get("desc")!!.jsonPrimitive.toString().replace("\"", ""),
                        it.get("id")!!.jsonPrimitive.toString().replace("\"", "")
                    )
                }
            }
        }.toMap()

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
package org.tabooproject.development

import org.tabooproject.development.step.ConfigurationPropertiesStep
import org.tabooproject.development.step.OptionalPropertiesStep

object FunctionTemplate {

    fun getBuildProperty(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            val configProperty = ConfigurationPropertiesStep.property
            // 插件名
            put("name", configProperty.name!!)
            // 主类
            put("group", configProperty.mainClass
                // 截去插件名
                .substringBeforeLast(".")
            )
            // 版本
            put("version", configProperty.version)
            // 模块列表
            put("modules", configProperty.modules.map { it.id })
            // 从模块构建额外 imports
            put("extraPackages", configProperty.modules.map { "import io.izzel.taboolib.gradle.${it.id}" })
            val optionalProperty = OptionalPropertiesStep.property
            // 插件描述
            put("description", optionalProperty.description)
            // 作者列表
            if (optionalProperty.authors.isNotEmpty()) {
                put("authors", optionalProperty.authors)
            }
            // 网站
            if (optionalProperty.website.isNotEmpty()) {
                put("website", optionalProperty.website)
            }
            // 依赖列表
            if (optionalProperty.depends.isNotEmpty()) {
                put("dependencies", optionalProperty.depends)
            }
            // 软依赖列表
            if (optionalProperty.softDepends.isNotEmpty()) {
                put("softDependencies", optionalProperty.softDepends)
            }
        }
    }
}
package org.tabooproject.intellij.util

import com.intellij.openapi.util.IconLoader

object Assets {

    val TABOO_16x16 by lazy(LazyThreadSafetyMode.NONE) {
        IconLoader.getIcon("/assets/icons/taboo.png", Assets::class.java)
    }

    val description by lazy(LazyThreadSafetyMode.NONE) {
        "测试组件, 我操你妈"
    }
}
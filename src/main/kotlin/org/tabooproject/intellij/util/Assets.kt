package org.tabooproject.intellij.util

import com.intellij.openapi.util.IconLoader

object Assets {

    val TABOO_16x16 by lazy(LazyThreadSafetyMode.NONE) {
        IconLoader.getIcon("/assets/icons/taboo16x16.png", Assets::class.java)
    }

    val TABOO_32x32 by lazy(LazyThreadSafetyMode.NONE) {
        IconLoader.getIcon("/assets/icons/taboo32x32.png", Assets::class.java)
    }

    val description by lazy(LazyThreadSafetyMode.NONE) {
        "The IntelliJ IDEA plugin for TabooLib."
    }
}
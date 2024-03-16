plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.17.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

fun properties(key: String) = project.findProperty(key).toString()

group = "org.tabooproject.intellij"
version = properties("version")

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.freemarker:freemarker:2.3.32")
}


intellij {
    version.set("2023.1.5")

    plugins.addAll(
        "java",
        "gradle",
        "Kotlin"
    )

    pluginName = "Taboo Integration"
    updateSinceUntilBuild = true
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}


tasks {
    shadowJar {
        archiveClassifier.set("all")
        relocate("org.freemarker", "org.tabooproject.intellij.freemarker")
        relocate("okhttp3", "org.tabooproject.intellij.okhttp3")
    }

    patchPluginXml {
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

tasks.runIde {
    maxHeapSize = "4G"

    System.getProperty("debug")?.let {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.debug.mode", "true")
    }
}
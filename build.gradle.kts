plugins {
    id("java")
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.intellij") version "1.17.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

fun properties(key: String) = project.findProperty(key).toString()

group = "org.tabooproject.development"
version = properties("version")

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.freemarker)
}

intellij {
    version.set("2024.2.1")

    plugins.addAll(
        "java",
        "gradle",
        "Kotlin"
    )

    pluginName.set("Taboo Development")
    updateSinceUntilBuild.set(false)
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}


tasks {

    build {
        dependsOn(shadowJar)
    }

    patchPluginXml {
        sinceBuild.set(properties("pluginSinceBuild"))
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

val writeVersionToFile by tasks.registering {
    val versionProp = project.objects.property(String::class.java).apply {
        set(project.version.toString())
    }

    outputs.file("build/version.txt")

    doLast {
        val versionFile = file("build/version.txt")
        versionFile.writeText(versionProp.get())
    }
}

tasks.runIde {
    maxHeapSize = "4G"

    System.getProperty("debug")?.let {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.debug.mode", "true")
    }
}
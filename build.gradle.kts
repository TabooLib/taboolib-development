plugins {
    id("java")
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

fun properties(key: String) = project.findProperty(key).toString()

group = "org.tabooproject.development"
version = properties("version")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.freemarker)
    
    intellijPlatform {
        intellijIdeaCommunity("2024.3.1")
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

kotlin {
    jvmToolchain(21)
    
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn",
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()
        name = "Taboo Development"
        
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
    
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
    
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn",
            )
        }
    }

    runIde {
        maxHeapSize = "4G"
        
        systemProperty("idea.kotlin.plugin.use.k2", "true")

        System.getProperty("debug")?.let {
            systemProperty("idea.ProcessCanceledException", "disabled")
            systemProperty("idea.debug.mode", "true")
        }
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
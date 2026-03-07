plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val usosConsumerKey: String = providers
    .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText.orNull
    ?.lines()
    ?.firstOrNull { it.startsWith("usos.consumer.key=") }
    ?.removePrefix("usos.consumer.key=")
    ?.trim() ?: ""

val usosConsumerSecret: String = providers
    .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText.orNull
    ?.lines()
    ?.firstOrNull { it.startsWith("usos.consumer.secret=") }
    ?.removePrefix("usos.consumer.secret=")
    ?.trim() ?: ""

kotlin {
    android {
        namespace = "dev.akinom.isod"
        compileSdk = 36
        minSdk = 24

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }

    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)

            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.jetbrains.components.ui.tooling.preview)

            implementation(libs.koin.compose)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.koin)
        }
    }
}

compose.resources {
    packageOfResClass = "dev.akinom.isod"
}

dependencies {
    "androidRuntimeClasspath"(libs.androidx.compose.ui.tooling)
}

val generateSecrets by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/secrets/commonMain/kotlin")
    outputs.dir(outDir)

    val key    = usosConsumerKey
    val secret = usosConsumerSecret

    inputs.property("key", key)
    inputs.property("secret", secret)

    doLast {
        val k = inputs.properties["key"] as String
        val s = inputs.properties["secret"] as String
        val dir = outDir.get().asFile
        dir.mkdirs()
        dir.resolve("Secrets.kt").writeText("""
            package dev.akinom.isod

            internal object Secrets {
                const val USOS_CONSUMER_KEY    = "$k"
                const val USOS_CONSUMER_SECRET = "$s"
            }
        """.trimIndent())
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(tasks.named("generateSecrets"))
}
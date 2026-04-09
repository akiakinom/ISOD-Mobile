plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
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
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "dev.akinom.isod.shared"
        compileSdk = 36
        minSdk = 24
        
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "ISOD Mobile Shared Module"
        homepage = "https://github.com/akiakinom/isod"
        version = "0.2.0"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.koin.core)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)

            api(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)

            implementation(libs.kotlinx.crypto.sha2)
            implementation(libs.kotlinx.crypto.hmac)

            implementation(libs.components.resources)
            implementation(libs.runtime)
            implementation(libs.compose.icons)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.multiplatform.settings)
            implementation(libs.androidx.webkit)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

compose.resources {
    packageOfResClass = "dev.akinom.isod.shared"
}

sqldelight {
    databases {
        create("ISODMobileDatabase") {
            packageName.set("dev.akinom.isod")
        }
    }
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

            object Secrets {
                const val USOS_CONSUMER_KEY    = "$k"
                const val USOS_CONSUMER_SECRET = "$s"
            }
        """.trimIndent())
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(tasks.named("generateSecrets"))
}

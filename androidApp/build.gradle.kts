plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "dev.akinom.isod.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.akinom.isod"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}

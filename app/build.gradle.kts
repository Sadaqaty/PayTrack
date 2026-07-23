import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) load(versionPropsFile.inputStream())
}

android {
    namespace = "studio.fixare.paytrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "studio.fixare.paytrack"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: versionProps.getProperty("VERSION_CODE", "1")).toInt()
        versionName = System.getenv("VERSION_NAME") ?: versionProps.getProperty("VERSION_NAME", "1.0.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("FIXARE_RELEASE_STORE_FILE") ?: "keystore.jks")
            storePassword = System.getenv("FIXARE_RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("FIXARE_RELEASE_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("FIXARE_RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.google.gson)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Task to clean up invalid resource files that break the build
tasks.register("cleanupInvalidResources") {
    doLast {
        val projectDir = project.projectDir
        val invalidFiles = listOf(
            "src/main/res/drawable/PayTrack.png",
            "src/main/res/drawable/PayTrack.svg"
        )
        
        invalidFiles.forEach { path ->
            val file = File(projectDir, path)
            if (file.exists()) {
                println("Automatically deleting invalid resource file: ${file.path}")
                try {
                    if (file.delete()) {
                        println("Successfully deleted ${file.name}")
                    } else {
                        println("Failed to delete ${file.name}")
                    }
                } catch (e: Exception) {
                    println("Error deleting ${file.name}: ${e.message}")
                }
            }
        }
    }
}

// Hook into the build process to run cleanup before preBuild
tasks.named("preBuild") {
    dependsOn("cleanupInvalidResources")
}
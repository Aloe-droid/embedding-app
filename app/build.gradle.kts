import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.aloe.embedding"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aloe.embedding"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

val localProps = Properties()
localProps.load(rootProject.file("local.properties").inputStream())
val sdkDir: String? = localProps.getProperty("sdk.dir")
val ndkDir = File(sdkDir, "ndk").listFiles()?.firstOrNull()?.absolutePath
    ?: "$sdkDir/ndk-bundle"

val targets = mapOf(
    "aarch64-linux-android" to "arm64-v8a",
    "armv7-linux-androideabi" to "armeabi-v7a",
    "i686-linux-android" to "x86",
    "x86_64-linux-android" to "x86_64"
)

val cargoPath = if (System.getProperty("os.name").lowercase().contains("windows")) {
    "C:\\Users\\csjun\\.cargo\\bin\\cargo.exe"
} else {
    "${System.getProperty("user.home")}/.cargo/bin/cargo"
}

targets.forEach { (rustTarget, androidAbi) ->
    tasks.register<Exec>("cargoBuild_${androidAbi}") {
        workingDir("$rootDir/rust")
        environment("ANDROID_NDK_HOME", ndkDir)
        commandLine(
            cargoPath,
            "ndk",
            "-t", rustTarget,
            "build", "--release"
        )
    }
}

tasks.register("cargoBuildAll") {
    targets.keys.forEach { dependsOn("cargoBuild_${targets[it]}") }
    doLast {
        targets.forEach { (rustTarget, androidAbi) ->
            val soFile = file("$rootDir/rust/target/$rustTarget/release/libmyrust.so")
            val destDir = file("$projectDir/src/main/jniLibs/$androidAbi")
            destDir.mkdirs()
            soFile.copyTo(file("$destDir/libmyrust.so"), overwrite = true)
        }
    }
}

tasks.named("preBuild") {
    dependsOn("cargoBuildAll")
}

dependencies {
    // Source: https://mvnrepository.com/artifact/com.microsoft.onnxruntime/onnxruntime-android
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.24.2")

    // Source: https://mvnrepository.com/artifact/com.google.ai.edge.localagents/localagents-rag
    implementation("com.google.ai.edge.localagents:localagents-rag:0.3.0")
    // Source: https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-guava
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")
    implementation("com.google.protobuf:protobuf-javalite:4.33.5")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
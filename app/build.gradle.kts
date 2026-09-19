plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zhizhu.controlconverter"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.zhizhu.controlconverter"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        ndk {
            abiFilters += "arm64-v8a"
        }
        versionName = "0.5"
    }

    signingConfigs {
        create("release") {
            // 签名配置：优先读取 CI 环境变量（GitHub Secret 注入），否则回退到本地 release.keystore。
            val ksPath = System.getenv("KEYSTORE_PATH")
            val ksPass = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPass = System.getenv("KEY_PASSWORD")
            storeFile = if (!ksPath.isNullOrBlank()) file(ksPath) else rootProject.file("release.keystore")
            storePassword = ksPass ?: "zhizhu0001"
            this.keyAlias = keyAlias ?: "zhizhu0001"
            keyPassword = keyPass ?: "zhizhu0001"
        }
    }

    buildTypes {
        debug {
            // 与正式包区分，允许同一台机器上并存安装：
            // debug 包名 com.zhizhu.controlconverter.debug（清单里无 FileProvider/authorities，改包名无冲突）
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        jniLibs {
            // 让 native 库（libcc.so）在 APK 内以 deflate 压缩存放（对应 manifest extractNativeLibs=true）。
            // 未压缩时 libcc.so 以 Stored 形式占 4.33MB，是 release 包体积的绝大部分；压缩后 APK 显著减小。
            // 代价：安装后系统会解压一份副本到应用数据目录，设备占用变大、安装时多一步解压。
            useLegacyPackaging = true
        }
        resources {
            // 仅剔除与运行期无关的许可证/元数据条目（约 19KB）。
            // 注意：不动 kotlin/** 的 *.kotlin_builtins（反射可能依赖），也不动 META-INF/services/*（ServiceLoader 需要）。
            excludes += setOf(
                "META-INF/androidx/**",
                "META-INF/version-control-info.textproto",
                "META-INF/com/android/build/gradle/app-metadata.properties",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.3")
    // 官方背景模糊库（导航栏毛玻璃）。Android 上依赖 RuntimeShader，仅 API 33+ 生效，
    // 低于该版本时由 isRuntimeShaderSupported() 门控回退为半透明底色。
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.3")
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("io.github.kyant0:backdrop:2.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

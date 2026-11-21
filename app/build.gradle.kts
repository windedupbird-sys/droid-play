import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

val admobPropertiesFile = rootProject.file("admob.properties")
val admobProperties = Properties()
if (admobPropertiesFile.exists()) {
    admobProperties.load(admobPropertiesFile.inputStream())
}

val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobInterstitialId = "ca-app-pub-3940256099942544/1033173712"

fun admobId(key: String, envName: String, testId: String): String {
    return System.getenv(envName)
        ?.takeIf { it.isNotBlank() }
        ?: admobProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: testId
}

android {
    namespace = "com.colortapz.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.colortapz.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.3.0"

        resValue(
            "string",
            "admob_app_id",
            admobId("admob.app_id", "ADMOB_APP_ID", testAdmobAppId)
        )
        resValue(
            "string",
            "admob_interstitial_id",
            admobId("admob.interstitial_id", "ADMOB_INTERSTITIAL_ID", testAdmobInterstitialId)
        )
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
                ?: System.getenv("KEYSTORE_FILE")
            val storePasswordValue = keystoreProperties.getProperty("storePassword")
                ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAliasValue = keystoreProperties.getProperty("keyAlias")
                ?: System.getenv("KEY_ALIAS")
            val keyPasswordValue = keystoreProperties.getProperty("keyPassword")
                ?: System.getenv("KEY_PASSWORD")

            if (storeFilePath != null && storePasswordValue != null && keyAliasValue != null && keyPasswordValue != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    testOptions {
        unitTests {
            // Lets JVM tests call into Android stubs without a Robolectric runtime.
            isReturnDefaultValues = true
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        // Versions are pinned to the newest set that still compiles against
        // compileSdk 36; the "newer version available" nags are noise here.
        disable += setOf("GradleDependency", "NewerVersionAvailable")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("com.google.android.gms:play-services-ads:24.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
  id("com.android.application")
  id("com.google.android.gms.oss-licenses-plugin")
  id("com.ncorti.ktfmt.gradle")
  id("kotlin-android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

try {
  keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} catch (_: FileNotFoundException) {
  logger.warn("keystore.properties not found")
}

android {
  namespace = "com.github.shingyx.boomswitch"
  signingConfigs {
    create("release") {
      storeFile = rootProject.file(keystoreProperties.getProperty("storeFile") ?: "default.jks")
      storePassword = keystoreProperties.getProperty("storePassword")
      keyAlias = keystoreProperties.getProperty("keyAlias")
      keyPassword = keystoreProperties.getProperty("keyPassword")
    }
  }
  compileSdk = 36
  defaultConfig {
    applicationId = "com.github.shingyx.boomswitch"
    minSdk = 23
    targetSdk = 36
    versionCode = 13
    versionName = "1.9"
  }
  buildTypes {
    getByName("debug") {
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-dev"
    }
    getByName("release") {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
      signingConfig = signingConfigs["release"]
    }
  }
  buildFeatures {
    buildConfig = true
    viewBinding = true
  }
  kotlin { jvmToolchain(21) }
}

ktfmt { googleStyle() }

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
  implementation("androidx.appcompat:appcompat:1.7.1")
  implementation("androidx.constraintlayout:constraintlayout:2.2.1")
  implementation("com.google.android.gms:play-services-oss-licenses:17.2.2")
  implementation("com.google.android.material:material:1.12.0")
  implementation("com.jakewharton.timber:timber:5.0.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}

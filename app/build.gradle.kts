import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.oss.licenses.plugin)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.kotlin.android)
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
    versionCode = 14
    versionName = "1.10"
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
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.material)
  implementation(libs.oss.licenses)
  implementation(libs.timber)
}

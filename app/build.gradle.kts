import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties
import java.util.Base64

val envFile = project.rootProject.file(".env")
val envProperties = Properties()
if (envFile.exists()) {
    envFile.inputStream().use { envProperties.load(it) }
}

fun getSecret(key: String, defaultValue: String = ""): String {
    return project.providers.gradleProperty(key).orNull
        ?: System.getenv(key)
        ?: envProperties.getProperty(key)
        ?: defaultValue
}

val dhanApiKeyVal = getSecret("DHAN_API_KEY")
val dhanClientIdVal = getSecret("DHAN_CLIENT_ID")
val dhanClientSecretVal = getSecret("DHAN_CLIENT_SECRET")
val dhanRedirectUriVal = getSecret("DHAN_REDIRECT_URI", "kingkhan://oauth/callback")
val angelApiKeyVal = getSecret("ANGEL_ONE_API_KEY")
val angelRedirectUriVal = getSecret("ANGEL_REDIRECT_URI", "kingkhan://oauth/callback")
val githubTokenVal = getSecret("GITHUB_TOKEN", getSecret("GH_TOKEN", ""))
val upstoxApiKeyVal = getSecret("UPSTOX_API_KEY")
val upstoxApiSecretVal = getSecret("UPSTOX_API_SECRET")
val upstoxRedirectUriVal = getSecret("UPSTOX_REDIRECT_URI", "https://application-beige-psi.vercel.app/oauth")
val fyersAppIdVal = getSecret("FYERS_APP_ID")
val fyersSecretIdVal = getSecret("FYERS_SECRET_ID")
val fyersRedirectUriVal = getSecret("FYERS_REDIRECT_URI", "https://application-beige-psi.vercel.app/oauth")

if (dhanClientIdVal.isNotBlank() || dhanApiKeyVal.isNotBlank() || angelApiKeyVal.isNotBlank() || upstoxApiKeyVal.isNotBlank() || fyersAppIdVal.isNotBlank()) {
    envFile.writeText("""
        DHAN_API_KEY=$dhanApiKeyVal
        DHAN_CLIENT_ID=$dhanClientIdVal
        DHAN_CLIENT_SECRET=$dhanClientSecretVal
        DHAN_REDIRECT_URI=$dhanRedirectUriVal
        ANGEL_ONE_API_KEY=$angelApiKeyVal
        ANGEL_REDIRECT_URI=$angelRedirectUriVal
        UPSTOX_API_KEY=$upstoxApiKeyVal
        UPSTOX_API_SECRET=$upstoxApiSecretVal
        UPSTOX_REDIRECT_URI=$upstoxRedirectUriVal
        FYERS_APP_ID=$fyersAppIdVal
        FYERS_SECRET_ID=$fyersSecretIdVal
        FYERS_REDIRECT_URI=$fyersRedirectUriVal
    """.trimIndent())
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.aistudio.kingkhanaitradepro.vtrqpx"
    minSdk = 24
    targetSdk = 34
    val vCode = (project.findProperty("versionCode") as? String)?.toIntOrNull() ?: 48
    val vName = (project.findProperty("versionName") as? String) ?: "1.0.48"
    versionCode = vCode
    versionName = vName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "DHAN_API_KEY", "\"${dhanApiKeyVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "DHAN_CLIENT_ID", "\"${dhanClientIdVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "DHAN_REDIRECT_URI", "\"${dhanRedirectUriVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ANGEL_ONE_API_KEY", "\"${angelApiKeyVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ANGEL_REDIRECT_URI", "\"${angelRedirectUriVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "UPSTOX_API_KEY", "\"${upstoxApiKeyVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "UPSTOX_REDIRECT_URI", "\"${upstoxRedirectUriVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "FYERS_APP_ID", "\"${fyersAppIdVal.replace("\"", "\\\"")}\"")
    buildConfigField("String", "FYERS_REDIRECT_URI", "\"${fyersRedirectUriVal.replace("\"", "\\\"")}\"")
  }

  signingConfigs {
    create("release") {
      val envKsPath = System.getenv("KEYSTORE_PATH") ?: System.getenv("RELEASE_KEYSTORE_PATH")
      fun resolveKsFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val f = file(path)
        if (f.exists()) return f
        val rootF = file("${rootDir}/$path")
        if (rootF.exists()) return rootF
        return null
      }

      val keystoreFile = resolveKsFile(envKsPath)
        ?: resolveKsFile("my-upload-key.jks")
        ?: resolveKsFile("release.keystore")

      if (keystoreFile == null) {
          val isRelease = gradle.startParameter.taskRequests.toString().contains("Release", ignoreCase = true)
          if (isRelease) {
              throw GradleException("Production signing keystore is missing. Refusing to generate a new signing key because this would break app updates.")
          }
      } else {
          storeFile = keystoreFile
          storePassword = System.getenv("STORE_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD") ?: throw GradleException("STORE_PASSWORD missing")
          keyAlias = System.getenv("KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS") ?: throw GradleException("KEY_ALIAS missing")
          keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD") ?: throw GradleException("KEY_PASSWORD missing")
      }

      enableV1Signing = true
      enableV2Signing = true
    }
    create("debugConfig") {
      fun resolveKsFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val f = file(path)
        if (f.exists()) return f
        val rootF = file("${rootDir}/$path")
        if (rootF.exists()) return rootF
        return null
      }

      val debugKs = resolveKsFile("debug.keystore")
        ?: run {
          val b64File = file("${rootDir}/debug.keystore.base64")
          if (b64File.exists()) {
            val ksFile = file("${rootDir}/debug.keystore")
            ksFile.writeBytes(Base64.getDecoder().decode(b64File.readText().trim()))
            ksFile
          } else null
        }

      if (debugKs != null) {
        storeFile = debugKs
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("DHAN_API_KEY")
  ignoreList.add("DHAN_CLIENT_ID")
  ignoreList.add("DHAN_CLIENT_SECRET")
  ignoreList.add("DHAN_REDIRECT_URI")
  ignoreList.add("ANGEL_ONE_API_KEY")
  ignoreList.add("ANGEL_REDIRECT_URI")
  ignoreList.add("UPSTOX_API_KEY")
  ignoreList.add("UPSTOX_API_SECRET")
  ignoreList.add("UPSTOX_REDIRECT_URI")
  ignoreList.add("FYERS_APP_ID")
  ignoreList.add("FYERS_SECRET_ID")
  ignoreList.add("FYERS_REDIRECT_URI")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation("androidx.browser:browser:1.8.0")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation("com.google.protobuf:protobuf-java:3.24.4")
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

gradle.projectsEvaluated {
    val envFile = project.rootProject.file(".env")
    val envProperties = Properties()
    if (envFile.exists()) {
        envFile.inputStream().use { envProperties.load(it) }
    }

    val angelKey = project.providers.gradleProperty("ANGEL_ONE_API_KEY").orNull ?: System.getenv("ANGEL_ONE_API_KEY") ?: envProperties.getProperty("ANGEL_ONE_API_KEY")
    val dhanId = project.providers.gradleProperty("DHAN_CLIENT_ID").orNull ?: System.getenv("DHAN_CLIENT_ID") ?: envProperties.getProperty("DHAN_CLIENT_ID")
    val dhanSecret = project.providers.gradleProperty("DHAN_CLIENT_SECRET").orNull ?: System.getenv("DHAN_CLIENT_SECRET") ?: envProperties.getProperty("DHAN_CLIENT_SECRET")
    val dhanApiKey = project.providers.gradleProperty("DHAN_API_KEY").orNull ?: System.getenv("DHAN_API_KEY") ?: envProperties.getProperty("DHAN_API_KEY")

    if (angelKey.isNullOrBlank() || angelKey == "ANGEL_ONE_API_KEY_DEFAULT_VALUE") {
        logger.warn("ANGEL_ONE_API_KEY secret is not configured. Real Angel One API calls will require credentials.")
    }
    if (dhanId.isNullOrBlank() || dhanId == "DHAN_CLIENT_ID_DEFAULT_VALUE") {
        logger.warn("DHAN_CLIENT_ID secret is not configured. Real Dhan API calls will require credentials.")
    }
    
    val hasDhanSecret = !dhanSecret.isNullOrBlank() && dhanSecret != "DHAN_CLIENT_SECRET_DEFAULT_VALUE"
    val hasDhanApiKey = !dhanApiKey.isNullOrBlank() && dhanApiKey != "DHAN_API_KEY_DEFAULT_VALUE"
    if (!hasDhanSecret && !hasDhanApiKey) {
        logger.warn("DHAN_CLIENT_SECRET or DHAN_API_KEY secret is not configured. Real Dhan API calls will require credentials.")
    }
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}

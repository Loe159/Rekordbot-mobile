import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use { input -> load(input) }
    }
}

fun signingValue(propertyName: String, environmentName: String): String? =
    keystoreProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentName)?.takeIf(String::isNotBlank)

val releaseStoreFile = signingValue("storeFile", "REKORDBOT_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "REKORDBOT_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "REKORDBOT_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "REKORDBOT_KEY_PASSWORD")
val releaseSigningValues = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val isReleaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }
check(releaseSigningValues.none { !it.isNullOrBlank() } || isReleaseSigningConfigured) {
    "Release signing is incomplete: configure all four REKORDBOT signing values."
}

val publicConfigPropertiesFile = rootProject.file("secrets.properties")
val publicConfigProperties = Properties().apply {
    if (publicConfigPropertiesFile.isFile) {
        publicConfigPropertiesFile.inputStream().use { input -> load(input) }
    }
}

fun publicConfigValue(propertyName: String, environmentName: String): String =
    publicConfigProperties.getProperty(propertyName)?.trim()?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentName)?.trim()?.takeIf(String::isNotBlank)
        ?: ""

fun quotedBuildConfigValue(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val spotifyClientId = publicConfigValue("spotifyClientId", "REKORDBOT_SPOTIFY_CLIENT_ID")
val airtableClientId = publicConfigValue("airtableClientId", "REKORDBOT_AIRTABLE_CLIENT_ID")
val publicApiBaseUrl = publicConfigValue("publicApiBaseUrl", "REKORDBOT_PUBLIC_API_BASE_URL")
val publicApiOrigin = runCatching { URI(publicApiBaseUrl) }
    .getOrNull()
    ?.takeIf { uri ->
        uri.scheme == "https" &&
            !uri.host.isNullOrBlank() &&
            uri.port == -1 &&
            uri.rawUserInfo == null &&
            uri.rawPath.isNullOrEmpty() &&
            uri.rawQuery == null &&
            uri.rawFragment == null
    }
check(publicApiBaseUrl.isBlank() || publicApiOrigin != null) {
    "REKORDBOT_PUBLIC_API_BASE_URL must be an HTTPS origin without path, port, query, or fragment."
}
val normalizedPublicApiBaseUrl = publicApiOrigin
    ?.host
    ?.lowercase()
    ?.let { host -> "https://$host" }
    .orEmpty()
val airtableRedirectUri = normalizedPublicApiBaseUrl
    .takeIf(String::isNotBlank)
    ?.plus("/oauth/airtable/callback")
    .orEmpty()
val isProductionBuild = providers.gradleProperty("rekordbotProduction")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

if (isProductionBuild) {
    check(spotifyClientId.isNotBlank()) {
        "A production build requires REKORDBOT_SPOTIFY_CLIENT_ID."
    }
    check(airtableClientId.isNotBlank()) {
        "A production build requires REKORDBOT_AIRTABLE_CLIENT_ID."
    }
    check(publicApiOrigin != null) {
        "A production build requires REKORDBOT_PUBLIC_API_BASE_URL to be an HTTPS origin without path, port, query, or fragment."
    }
    check(isReleaseSigningConfigured) {
        "A production build requires all four REKORDBOT release signing values."
    }
}

val appVersionCode = 6
val appVersionName = "1.2.1"

android {
    namespace = "com.loe159.rekordbot.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.loe159.rekordbot.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SPOTIFY_CLIENT_ID", quotedBuildConfigValue(spotifyClientId))
        buildConfigField("String", "AIRTABLE_CLIENT_ID", quotedBuildConfigValue(airtableClientId))
        buildConfigField("String", "PUBLIC_API_BASE_URL", quotedBuildConfigValue(normalizedPublicApiBaseUrl))
        buildConfigField("String", "AIRTABLE_REDIRECT_URI", quotedBuildConfigValue(airtableRedirectUri))
        manifestPlaceholders["airtableCallbackScheme"] = "https"
        manifestPlaceholders["airtableCallbackHost"] = publicApiOrigin?.host?.lowercase() ?: "rekordbot.invalid"
    }

    signingConfigs {
        if (isReleaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register("printVersionName") {
    description = "Prints the Android version name for release automation."
    doLast { println(appVersionName) }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

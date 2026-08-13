import java.util.Properties

plugins {
    id("com.android.application")
}

val linkProperties = Properties().apply {
    val localConfig = rootProject.file("links.properties")
    if (localConfig.isFile) {
        localConfig.inputStream().use(::load)
    }
}

fun configuredValue(propertyName: String, environmentName: String, fallback: String): String {
    val environmentValue = providers.environmentVariable(environmentName).orNull?.trim()
    if (!environmentValue.isNullOrEmpty()) return environmentValue

    val propertyValue = linkProperties.getProperty(propertyName)?.trim()
    return propertyValue?.takeIf(String::isNotEmpty) ?: fallback
}

fun quotedBuildConfigValue(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val primaryLabel = configuredValue("nfc.primary.label", "NFC_PRIMARY_LABEL", "Primary link")
val primaryUrl = configuredValue("nfc.primary.url", "NFC_PRIMARY_URL", "https://example.com/profile")
val secondaryLabel = configuredValue("nfc.secondary.label", "NFC_SECONDARY_LABEL", "Secondary link")
val secondaryUrl = configuredValue("nfc.secondary.url", "NFC_SECONDARY_URL", "https://example.com")
val configuredVersionCode = providers.environmentVariable("PLAY_VERSION_CODE").orNull
    ?.trim()
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: 2

val playKeystorePath = providers.environmentVariable("PLAY_UPLOAD_KEYSTORE_PATH").orNull?.trim()
val playKeystorePassword = providers.environmentVariable("PLAY_UPLOAD_KEYSTORE_PASSWORD").orNull
val playKeyAlias = providers.environmentVariable("PLAY_UPLOAD_KEY_ALIAS").orNull?.trim()
val playKeyPassword = providers.environmentVariable("PLAY_UPLOAD_KEY_PASSWORD").orNull
val playSigningConfigured = listOf(
    playKeystorePath,
    playKeystorePassword,
    playKeyAlias,
    playKeyPassword,
).all { !it.isNullOrEmpty() }

listOf(primaryUrl, secondaryUrl).forEach { url ->
    require(url.startsWith("https://") || url.startsWith("http://")) {
        "Configured NFC destinations must use http:// or https://"
    }
}

android {
    namespace = "com.adamrussin.urlnfctap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.adamrussin.urlnfctap.v2"
        minSdk = 23
        targetSdk = 36
        versionCode = configuredVersionCode
        versionName = "1.0.1"

        buildConfigField("String", "PRIMARY_LABEL", quotedBuildConfigValue(primaryLabel))
        buildConfigField("String", "PRIMARY_URL", quotedBuildConfigValue(primaryUrl))
        buildConfigField("String", "SECONDARY_LABEL", quotedBuildConfigValue(secondaryLabel))
        buildConfigField("String", "SECONDARY_URL", quotedBuildConfigValue(secondaryUrl))
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (playSigningConfigured) {
            create("playRelease") {
                storeFile = rootProject.file(playKeystorePath!!)
                storePassword = playKeystorePassword
                keyAlias = playKeyAlias
                keyPassword = playKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (playSigningConfigured) {
                signingConfig = signingConfigs.getByName("playRelease")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all {
            it.useJUnit()
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

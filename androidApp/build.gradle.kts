import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

private val adsPropertiesFileName = "ads.properties"
private val flavorDimensionName = "game"

private val keystorePropertiesFileName = "keystore.properties"

private val googlePropertiesFileName = "google.properties"

private val adsProperties = Properties().apply {
    val adsPropertiesFile = File(rootProject.projectDir, adsPropertiesFileName)
    if (adsPropertiesFile.exists()) {
        adsPropertiesFile.inputStream().use(::load)
    }
}

private val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = File(rootProject.projectDir, keystorePropertiesFileName)
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

private fun keystoreProperty(flavorName: String, suffix: String): String {
    val flavorScopedProperty = "keystore.$flavorName.$suffix"
    val legacyProperty = "keystore.$suffix"
    return (
            trimmedProperty(keystoreProperties, flavorScopedProperty)
                ?: if (flavorName == "stackshift") trimmedProperty(
                    keystoreProperties,
                    legacyProperty
                ) else null
            ) ?: ""
}

private data class AndroidFlavorConfig(
    val name: String,
    val displayName: String = name,
    val applicationId: String,
    val versionCode: Int,
    val versionName: String,
) {
    val flavorName: String get() = name.lowercase()
}

private val androidFlavorConfigs = listOf(
    AndroidFlavorConfig(
        name = "StackShift",
        displayName = "Puzzle Shift",
        applicationId = "com.ugurbuga.stackshift",
        versionCode = 10,
        versionName = "1.1.1",
    ),
    AndroidFlavorConfig(
        name = "BlockWise",
        applicationId = "com.ugurbuga.blockwise",
        versionCode = 2,
        versionName = "1.0.1",
    ),
    AndroidFlavorConfig(
        name = "BlockSort",
        applicationId = "com.ugurbuga.blocksort",
        versionCode = 1,
        versionName = "1.0.0",
    ),
    AndroidFlavorConfig(
        name = "MergeShift",
        applicationId = "com.ugurbuga.mergeshift",
        versionCode = 1,
        versionName = "1.0.0",
    ),
    AndroidFlavorConfig(
        name = "BoomBlocks",
        applicationId = "com.ugurbuga.boomblocks",
        versionCode = 1,
        versionName = "1.0.0",
    ),
)

private fun trimmedProperty(properties: Properties, name: String): String? =
    properties.getProperty(name)?.trim()?.takeIf(String::isNotEmpty)

private fun adsProperty(flavorName: String, suffix: String): String {
    val flavorScopedProperty = "ads.android.$flavorName.$suffix"
    return trimmedProperty(adsProperties, flavorScopedProperty).orEmpty()
}

abstract class GenerateLocalFirebaseConfigTask : DefaultTask() {
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourcePropertiesFile: RegularFileProperty

    @get:OutputFiles
    abstract val outputFiles: ListProperty<RegularFile>

    @get:Input
    abstract val flavorConfigurations: ListProperty<String>

    @TaskAction
    fun generate() {
        data class FlavorConfig(
            val flavorName: String,
            val applicationId: String,
        )

        fun Properties.trimmed(name: String): String? =
            getProperty(name)?.trim()?.takeIf(String::isNotEmpty)

        fun Properties.requireTrimmed(flavorName: String, name: String): String? =
            trimmed("firebase.$flavorName.$name")
                ?: if (flavorName == "stackshift") trimmed("firebase.$name") else null

        fun String.escapeJson(): String = buildString(length) {
            for (character in this@escapeJson) {
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
        }

        val sourceFile = sourcePropertiesFile.asFile.orNull
        val configFiles = outputFiles.get()
        val flavors = flavorConfigurations.get().map { config ->
            val (flavorName, applicationId) = config.split('|', limit = 2)
            FlavorConfig(flavorName = flavorName, applicationId = applicationId)
        }

        if (sourceFile == null || !sourceFile.exists()) {
            return
        }

        val googleProperties = Properties().apply {
            sourceFile.inputStream().use(::load)
        }

        flavors.zip(configFiles).forEach { (flavor, outputFile) ->
            val androidConfigFile = outputFile.asFile
            val projectNumber = googleProperties.requireTrimmed(flavor.flavorName, "projectNumber")
            val projectId = googleProperties.requireTrimmed(flavor.flavorName, "projectId")
            val storageBucket =
                googleProperties.trimmed("firebase.${flavor.flavorName}.storageBucket")
                    ?: if (flavor.flavorName == "stackshift") googleProperties.trimmed("firebase.storageBucket") else null
            val androidAppId = googleProperties.requireTrimmed(flavor.flavorName, "android.appId")
            val androidApiKey = googleProperties.requireTrimmed(flavor.flavorName, "android.apiKey")
            val androidPackageName = (
                    googleProperties.trimmed("firebase.${flavor.flavorName}.android.packageName")
                        ?: if (flavor.flavorName == "stackshift") googleProperties.trimmed("firebase.android.packageName") else null
                    ) ?: flavor.applicationId

            if ((projectNumber == null) || (projectId == null) || (androidAppId == null) || (androidApiKey == null)) {
                return@forEach
            }

            val googleServicesJson = """
                {
                  "project_info": {
                    "project_number": "${projectNumber.escapeJson()}",
                    "project_id": "${projectId.escapeJson()}",
                    "storage_bucket": "${storageBucket.orEmpty().escapeJson()}"
                  },
                  "client": [
                    {
                      "client_info": {
                        "mobilesdk_app_id": "${androidAppId.escapeJson()}",
                        "android_client_info": {
                          "package_name": "${androidPackageName.escapeJson()}"
                        }
                      },
                      "oauth_client": [],
                      "api_key": [
                        {
                          "current_key": "${androidApiKey.escapeJson()}"
                        }
                      ],
                      "services": {
                        "appinvite_service": {
                          "other_platform_oauth_client": []
                        }
                      }
                    }
                  ],
                  "configuration_version": "1"
                }
            """.trimIndent() + "\n"

            androidConfigFile.parentFile?.mkdirs()
            if ((!androidConfigFile.exists()) || (androidConfigFile.readText() != googleServicesJson)) {
                androidConfigFile.writeText(googleServicesJson)
            }
        }
    }
}


val generateLocalFirebaseConfig by tasks.registering(GenerateLocalFirebaseConfigTask::class) {
    group = "build setup"
    description =
        "Generates flavor-specific local Firebase config files from google.properties when available."
    sourcePropertiesFile.set(rootProject.layout.projectDirectory.file(googlePropertiesFileName))
    flavorConfigurations.set(androidFlavorConfigs.map { flavor -> "${flavor.flavorName}|${flavor.applicationId}" })
    outputFiles.set(
        androidFlavorConfigs.map { flavor ->
            layout.projectDirectory.file("src/${flavor.flavorName}/google-services.json")
        }
    )
}

tasks.matching { task ->
    task.name.matches(Regex("process.+GoogleServices"))
}.configureEach {
    dependsOn(generateLocalFirebaseConfig)
}

android {
    namespace = "com.ugurbuga.blockgames"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/GameDebug.jks")
            storePassword = "GameDebug"
            keyAlias = "GameDebug"
            keyPassword = "GameDebug"
        }
        androidFlavorConfigs.forEach { flavor ->
            create(flavor.flavorName) {
                storeFile = keystoreProperty(flavor.flavorName, "storeFile")
                    .takeIf(String::isNotBlank)
                    ?.let(rootProject::file)
                    ?: rootProject.file("keystore/${flavor.name}.jks")
                storePassword = keystoreProperty(flavor.flavorName, "storePassword")
                keyAlias = keystoreProperty(flavor.flavorName, "keyAlias")
                keyPassword = keystoreProperty(flavor.flavorName, "keyPassword")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    flavorDimensions += flavorDimensionName
    productFlavors {
        androidFlavorConfigs.forEach { flavor ->
            create(flavor.flavorName) {
                dimension = flavorDimensionName
                applicationId = flavor.applicationId
                versionCode = flavor.versionCode
                versionName = flavor.versionName
                resValue("string", "app_name", flavor.displayName)
                signingConfig = signingConfigs.getByName(flavor.flavorName)
                manifestPlaceholders["adsApplicationId"] =
                    adsProperty(flavor.flavorName, "applicationId")
                buildConfigField(
                    "String",
                    "ADS_BANNER_UNIT_ID",
                    "\"${adsProperty(flavor.flavorName, "bannerUnitId")}\""
                )
                buildConfigField(
                    "String",
                    "ADS_INTERSTITIAL_UNIT_ID",
                    "\"${adsProperty(flavor.flavorName, "interstitialUnitId")}\""
                )
                buildConfigField(
                    "String",
                    "ADS_REWARDED_UNIT_ID",
                    "\"${adsProperty(flavor.flavorName, "rewardedUnitId")}\""
                )
                buildConfigField(
                    "String",
                    "ADS_REWARDED_SPECIAL_UNIT_ID",
                    "\"${adsProperty(flavor.flavorName, "rewardedSpecialUnitId")}\""
                )
                buildConfigField("String", "GAMEPLAY_STYLE", "\"${flavor.name}\"")
                buildConfigField("String", "APP_VARIANT_NAME", "\"${flavor.flavorName}\"")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.google.play.services.ads)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.compose.uiTooling)
}

plugins {
    alias(libs.plugins.homeassistant.android.application)
    alias(libs.plugins.homeassistant.android.flavor)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.homeassistant.android.dependencies)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    useLibrary("android.car")

    defaultConfig {
        // MOBEX internal distribution: an own application id lets the app be uploaded as a
        // managed Google Play private app alongside the official Play Store app. The namespace
        // (and therefore all code and resources) stays io.homeassistant.companion.android.
        applicationId = "de.mobex.homeassistant"
        // The flavor convention plugin derived APPLICATION_IDS from the original application id
        // before this override runs, so redefine it (used for NFC tag application records).
        buildConfigField(
            "String[]",
            "APPLICATION_IDS",
            "{\"de.mobex.homeassistant\", \"de.mobex.homeassistant.minimal\"}",
        )

        manifestPlaceholders["sentryRelease"] = "$applicationId@$versionName"
        manifestPlaceholders["sentryDsn"] = System.getenv("SENTRY_DSN") ?: ""

        testInstrumentationRunner = "io.homeassistant.companion.android.util.HAAndroidJUnitRunner"

        bundle {
            language {
                // We want to keep the translations in the final AAB for all the language
                enableSplit = false
            }
        }
    }

    buildTypes {
        debug {
            // Required for HWASan wrap.sh to be included uncompressed in the APK
            // See https://developer.android.com/ndk/guides/hwasan
            packaging {
                jniLibs {
                    useLegacyPackaging = true
                }
            }
        }
    }

    lint {
        // Until we fully migrate to Material3 this lint issue is too verbose https://github.com/home-assistant/android/issues/5420
        disable += listOf("UsingMaterialAndMaterial3Libraries")
    }
}

firebaseAppDistributionDefault {
    serviceCredentialsFile = "firebaseAppDistributionServiceCredentialsFile.json"
    releaseNotesFile = "./app/build/outputs/changelogBeta"
    groups = "continuous-deployment"
}

dependencies {
    // Most of the dependencies are coming from the convention plugin to avoid duplication with `:automotive` module.
    "fullImplementation"(libs.car.projected)
    ksp(project(":provides-sensor-processor"))
}

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

val appVersion = System.getenv("APP_VERSION") ?: "0.0.0"

kotlin {
    jvm()
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.material.icons.extended)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "org.kvxd.bonk.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Rpm)
            packageName = "bonk"
            packageVersion = appVersion
            description = "Soundboard for Linux"
            vendor = "kvxd"

            linux {
                modules("java.instrument", "java.prefs")

                rpmLicenseType = "MIT"

                debMaintainer = "kvxd <0x1bd@proton.me>"
            }
        }
    }
}

tasks.register<Zip>("packageDistributionTar") {
    dependsOn("createReleaseDistributable")

    archiveFileName.set("bonk-$appVersion.tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/tar"))

    from(layout.buildDirectory.dir("compose/binaries/main-release/app/bonk")) {
        into("bonk-$appVersion")
    }

    from("bonk.desktop") {
        into("bonk-$appVersion")
    }
}
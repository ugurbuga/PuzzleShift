
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.JavaExec

private fun findDesktopJdkHome(): File? {
    val candidateHomes = buildList {
        System.getenv("JAVA_HOME")?.let(::File)?.let(::add)
        System.getProperty("java.home")?.let(::File)?.let(::add)

        File(System.getProperty("user.home"), "Library/Java/JavaVirtualMachines")
            .listFiles()
            .orEmpty()
            .map { File(it, "Contents/Home") }
            .forEach(::add)

        File("/Library/Java/JavaVirtualMachines")
            .listFiles()
            .orEmpty()
            .map { File(it, "Contents/Home") }
            .forEach(::add)
    }

    return candidateHomes.firstOrNull { it.resolve("bin/jpackage").canExecute() }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

val desktopJdkHome = findDesktopJdkHome()

kotlin {
    android {
        namespace = "com.ugurbuga.blockgames.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        mainRun {
            mainClass = "com.ugurbuga.blockgames.MainKt"
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.google.play.services.ads)
            implementation(libs.androidx.work.runtime)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
        }
        iosMain.dependencies {
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.java.sdk)
        }
    }
}


dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.ugurbuga.blockgames.MainKt"

        if (desktopJdkHome != null) {
            javaHome = desktopJdkHome.absolutePath

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "com.ugurbuga.stackshift"
                packageVersion = "1.0.0"

                macOS {
                    iconFile.set(project.file("desktop-icons/stackshift.icns"))
                }
                windows {
                    iconFile.set(project.file("desktop-icons/stackshift.ico"))
                }
                linux {
                    iconFile.set(project.file("desktop-icons/stackshift.png"))
                }
            }
        }
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach {
    mainClass.set("com.ugurbuga.blockgames.MainKt")
}

tasks.register("packageDesktopApp") {
    group = "distribution"
    description = "Builds the portable desktop application image for the current OS."
    if (desktopJdkHome != null) {
        dependsOn("createDistributable")
    } else {
        doLast {
            error(
                "Desktop packaging requires a JDK with jpackage. Set JAVA_HOME to a full JDK or install one before running packageDesktopApp.",
            )
        }
    }
}


tasks.register("runWeb") {
    group = "application"
    description = "Runs the wasm web app in a browser development server."
    dependsOn("wasmJsBrowserDevelopmentRun")
}

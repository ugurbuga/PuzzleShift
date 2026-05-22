
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

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

abstract class GenerateWordShiftWordListsTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile = outputDir.file("com/ugurbuga/blockgames/game/logic/GeneratedWordShiftWordLists.kt").get().asFile
        outputFile.parentFile.mkdirs()

        val csvEntries = sourceDir.asFileTree
            .matching { include("*.txt") }
            .files
            .sortedBy { it.name }
            .joinToString(separator = ",\n") { file ->
                val localeTag = file.nameWithoutExtension
                val content = file.readText().trim().replace("\"\"\"", "\\\"\\\"\\\"")
                "        \"$localeTag\" to \"\"\"$content\"\"\""
            }

        outputFile.writeText(
            """
            package com.ugurbuga.blockgames.game.logic

            import com.ugurbuga.blockgames.game.model.AppLanguage

            internal object GeneratedWordShiftWordLists {
                private val csvByLocaleTag: Map<String, String> = mapOf(
            $csvEntries
                )

                fun csvFor(language: AppLanguage): String =
                    csvByLocaleTag[language.localeTag]
                        ?: csvByLocaleTag.getValue(AppLanguage.English.localeTag)
            }
            """.trimIndent(),
        )
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

val desktopJdkHome = findDesktopJdkHome()
val wordShiftTxtDir = layout.projectDirectory.dir("src/commonMain/composeResources/files/wordshift")
val generatedWordShiftDir = layout.buildDirectory.dir("generated/wordshift/kotlin/commonMain")

val generateWordShiftWordLists by tasks.registering(GenerateWordShiftWordListsTask::class) {
    group = "code generation"
    description = "Generates shared WordShift word list sources from txt resources."
    sourceDir.set(wordShiftTxtDir)
    outputDir.set(generatedWordShiftDir)
}

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
        commonMain {
            kotlin.srcDir(generatedWordShiftDir)
        }

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

tasks.matching { it.name.contains("compile", ignoreCase = true) && it.name.contains("Kotlin", ignoreCase = true) }
    .configureEach {
        dependsOn(generateWordShiftWordLists)
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

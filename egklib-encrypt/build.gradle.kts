import java.util.*

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                kotlinOptions.freeCompilerArgs = listOf(
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi,kotlinx.serialization.ExperimentalSerializationApi"
                )
            }
        }

        testRuns["test"].executionTask
            .configure {
                useJUnitPlatform()
                minHeapSize = "512m"
                maxHeapSize = "8g"
                jvmArgs = listOf("-Xss128m")

                // Make tests run in parallel
                // More info: https://www.jvt.me/posts/2021/03/11/gradle-speed-parallel/
                systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
                systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
                systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
            }
    }

    js(IR) {
        binaries.library()
        useEsModules()
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add(
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi,kotlinx.serialization.ExperimentalSerializationApi"
                )
            }
        }
        val nodeJsArgs = listOf("--max-old-space-size=4096")
        nodejs {
            testTask {
                this.nodeJsArgs += nodeJsArgs
                useMocha { timeout = "0s" }
            }
        }
        browser {
            testTask {
                this.nodeJsArgs += nodeJsArgs
                useKarma {
                    findLocalProperty("karma.browsers")
                        ?.let { it as String }?.split(",")
                        ?.forEach { browser ->
                            when (browser) {
                                "ChromeHeadless" -> useChromeHeadless()
                                "Chrome" -> useChrome()
                                "Firefox" -> useFirefox()
                                "FirefoxHeadless" -> useFirefoxHeadless()
                            }
                        } ?: useChromeHeadless()
                }
            }
        }

        sourceSets {
            commonMain {
                dependencies {
                    api(project(":egklib-core"))
                    implementation(libs.bundles.eglib)
                }
            }
            commonTest {
                dependencies {
                    implementation(libs.bundles.egtest)
                }
            }
            jvmMain {
                dependencies {
                    implementation(kotlin("stdlib-jdk8"))
                }
            }
            jvmTest {
                dependencies {
                    implementation("me.tongfei:progressbar:0.9.3")
                    implementation(libs.bundles.jvmtest)
                }
            }
            jsMain {
                dependencies {
                    implementation("org.jetbrains.kotlin-wrappers:kotlin-node-js:18.16.12-pre.686")
                }
            }
            jsTest {
                dependencies {
                    implementation(kotlin("test-js"))
                }
            }
        }
    }
}

fun Project.findLocalProperty(name: String): Any? {
    val localProperties = file("${project.rootDir}/local.properties")
    return if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties[name]
    } else null
}

// LOOK some kind of javascript security thing, but may be causing coupled projects
// https://docs.gradle.org/current/userguide/multi_project_configuration_and_execution.html#sec:decoupled_projects
// allprojects {
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask> {
    args += "--ignore-scripts"
}
// }

// Workaround the Gradle bug resolving multi-platform dependencies.
// Fix courtesy of https://github.com/square/okio/issues/647
configurations.forEach {
    if (it.name.lowercase().contains("kapt") || it.name.lowercase().contains("proto")) {
        it.attributes
            .attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach { kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn" }

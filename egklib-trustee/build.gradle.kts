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
            kotlinOptions.jvmTarget = "17"
            testRuns["test"].executionTask.configure {
                useJUnitPlatform()
                systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
                systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
                systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
            }
        }
    }
    js(IR) {
        useEsModules()
        binaries.library()

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
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":egklib-core"))
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotest.property)

                // since we are configuring test browsers dynamically,
                // we need to add the dependencies here to prevent changes in yarn.lock
                runtimeOnly(npm("karma-firefox-launcher", "2.1.2"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.mockk)
//                implementation(libs.kotlin.test.junit5)
                implementation(libs.junit.jupiter.params)
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


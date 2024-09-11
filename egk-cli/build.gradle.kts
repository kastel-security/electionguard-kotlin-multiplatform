import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)
    application
}

repositories {
    mavenCentral()
}

kotlin {
    metadata {
        compilations.all {
            kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
        }
    }
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
            kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
            kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass = "MainKt"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            minHeapSize = "512m"
            maxHeapSize = "8g"

            // Make tests run in parallel
            // More info: https://www.jvt.me/posts/2021/03/11/gradle-speed-parallel/
            systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
            systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
            systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
        }
    }
    js(IR) {
        binaries.executable()
        compilations.all {
            kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "0s"
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":egklib"))
                implementation(project(":egklib-trustee"))
                implementation(project(":egklib-encrypt"))

                implementation(libs.kotlinx.cli)
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
                implementation(libs.sl4j.simple)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.bundles.jvmtest)
            }
        }
    }
}

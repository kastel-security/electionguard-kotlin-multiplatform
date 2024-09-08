import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass = "MainKt"
        }
    }
    js(IR) {
        binaries.executable()
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
                implementation(project(":egklib-core"))
                implementation(project(":egklib-trustee"))

                implementation(libs.oshai.logging)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.cli)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":egklib"))
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.sl4j.simple)
            }
        }
    }
}

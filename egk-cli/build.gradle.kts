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
            dependsOn("copy")
        }
    }
    js(IR) {
        binaries.executable()
        compilations.all {
            kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
        }
        nodejs {
            testTask {
                environment["NODE_OPTIONS"] = "--max-old-space-size=4096"
                useMocha {
                    timeout = "0s"
                    environment["MOCHA_OPTIONS"] = "--parallel"
                }
                dependsOn("copy")
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
                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.bundles.jvmtest)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin-wrappers:kotlin-node-js:18.16.12-pre.686")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

// Part of a multi-module project, hence the rootProject everywhere
tasks.create("copy", Copy::class.java) {
    from("$projectDir/../egklib/src/commonTest/resources")
    into("${rootProject.projectDir}/src/commonTest/resources")
}

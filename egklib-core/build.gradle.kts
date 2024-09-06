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
            kotlinOptions.freeCompilerArgs = listOf(
                "-Xexpect-actual-classes",
            )
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
        compilations.all { kotlinOptions.freeCompilerArgs = listOf("-Xexpect-actual-classes") }
        binaries.library()
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.bull.result)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.oshai.logging)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
                implementation(libs.kotest.property)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("me.tongfei:progressbar:0.9.3")
            }
        }
    }
}

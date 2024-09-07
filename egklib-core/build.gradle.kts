
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import java.util.*

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)
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
        compilations.all { kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes" }
        val nodeJsArgs = listOf("--max-old-space-size=4096")

        nodejs {
            testTask {
                this.nodeJsArgs += nodeJsArgs
                useMocha {
                    timeout = "0s" // disable timeouts
                }
            }
        }
        browser {
            testTask {
                this.nodeJsArgs += nodeJsArgs
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.library()
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
        val jvmMain by getting {
            dependencies {
                implementation(libs.sl4j.simple)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("me.tongfei:progressbar:0.9.3")
            }
        }
    }
}

// exclude benchmark tests from default test runs
tasks.withType<Test> {
    exclude("**/benchmark/**")
}
tasks.withType<KotlinJsTest> {
    filter.excludeTestsMatching("*.benchmark.*")
}

fun Project.findLocalProperty(name: String): Any? {
    val localProperties = file("${project.rootDir}/local.properties")
    return if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties[name]
    } else null
}

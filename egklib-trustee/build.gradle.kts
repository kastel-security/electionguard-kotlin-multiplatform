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
        }
    }
    js(IR) {
        binaries.library()
        nodejs()
        browser()
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

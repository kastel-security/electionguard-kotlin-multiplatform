import java.util.*

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
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    js(IR) {
        binaries.library()
        useEsModules()

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

import java.util.*

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)
    application
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "electionguard-kotlin-multiplatform"
version = "2.0.4-SNAPSHOT"


kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
            kotlinOptions.freeCompilerArgs = listOf(
                "-Xexpect-actual-classes",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi,kotlinx.serialization.ExperimentalSerializationApi"
            )
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
        binaries.executable()
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
                compilerOptions.freeCompilerArgs.add(
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi,kotlinx.serialization.ExperimentalSerializationApi"
                )
            }
        }
        browser {
            testTask {
                dependsOn("cleanAllTests")
                useKarma {
                    // specify the browser for testing in the 'local.properties' file of the root project
                    // for example: 'test.browsers=firefox,chromeHeadless' will use both firefox and chromeHeadless for test execution
                    // make sure the specified browsers are installed - it uses chromeHeadless by default.
                    project.getLocalProperty("test.browsers")
                        ?.let { (it as String).split(",") }
                        ?.map {
                            when(it) {
                                "chrome" -> ::useChrome
                                "chromeHeadless" -> ::useChromeHeadless
                                "firefox" -> ::useFirefox
                                "firefoxHeadless" -> ::useFirefoxHeadless
                                else -> throw StopExecutionException("not a supported testbrowser: $it")

                            }
                        }
                        ?.let { it.forEach { testBrowser -> testBrowser() } }
                        ?: useChromeHeadless()
                    // pass  -Ptests=... to specify which tests to run
                    if (project.hasProperty("tests")) {
                        setTestNameIncludePatterns(
                            (project.property("tests") as String).split(",")
                        )
                    } else {
                        setTestNameIncludePatterns(listOf("electionguard.core.G*", "electionguard.core.B*",
                            "electionguard.core.*"))
                    }
                }
            }
        }
    }

    sourceSets {
        all { languageSettings.optIn("kotlin.RequiresOptIn") }

        val commonMain by
            getting {
                dependencies {
                    implementation(libs.bundles.eglib)
                }
            }
        val commonTest by
            getting {
                dependencies {
                    implementation(libs.bundles.egtest)
                }
            }
        val jvmMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-jdk8"))
                }
            }
        val jvmTest by
            getting {
                dependencies {
                    implementation("me.tongfei:progressbar:0.9.3")
                    implementation(libs.bundles.jvmtest)
                }
            }
        val jsMain by getting {
            dependencies {
                implementation(npm("big-integer", "1.6.52"))
                implementation(npm("@noble/hashes", "1.0.0"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
                //since we are using dynamically configured test browsers,
                //we have to make sure this does not affect the yarn.lock file.
                //Therefore we explicitly name each browser aside from chrome we use for testing here
                runtimeOnly(npm("karma-firefox-launcher", "2.1.2"))
            }
        }
        /* val nativeMain by getting {
            dependencies {
                implementation(project(":hacllib"))
            }
        }
        val nativeTest by getting { dependencies {} }

         */
    }
    jvmToolchain(17)
}

tasks.withType<Test> { testLogging { showStandardStreams = true } }

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
dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

// publish github package
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/votingworks/electionguard-kotlin-multiplatform")
            credentials {
                username = project.findProperty("github.user") as String? ?: System.getenv("GITHUB_USER")
                password = project.findProperty("github.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

fun Project.getLocalProperty(name: String): Any? {
    val properties = Properties()
    try {
        properties.load(rootProject.file("local.properties").reader())
        return properties[name]
    } catch (ignored: java.io.IOException) {
        return null
    }
}

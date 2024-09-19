import java.util.*

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
}

subprojects {
    group = "electionguard-kotlin-multiplatform"
    version = "2.2.0"

    apply(plugin = "maven-publish")
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/kastel-security/electionguard-kotlin-multiplatform")
                credentials {
                    username = project.findLocalProperty("github.user") as String? ?: System.getenv("USERNAME")
                    password = project.findLocalProperty("github.key") as String? ?: System.getenv("TOKEN")
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
    } else {
        null
    }
}

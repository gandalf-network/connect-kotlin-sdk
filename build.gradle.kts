plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
    `maven-publish`
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = "com.gandalf.connect"
    version = "1.0.2"

    publishing {
        publications {
            create<MavenPublication>("span") {
                from(components["java"])
                groupId = "com.gandalf.connect"
                artifactId = "gandalf-connect"
                version = "1.0.2"
            }
        }

        repositories {
            maven {
                url = uri("https://jitpack.io")
            }
        }
    }
}

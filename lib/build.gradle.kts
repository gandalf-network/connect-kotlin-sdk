plugins {
    kotlin("jvm") version "2.0.0"
    id("com.apollographql.apollo3").version("3.8.4")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.4")
    implementation("com.apollographql.apollo3:apollo-rx3-support:3.8.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.reactivex.rxjava3:rxjava:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.6.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testImplementation("org.mockito:mockito-core:4.5.1")
    testImplementation("org.mockito:mockito-inline:4.5.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("io.reactivex.rxjava3:rxjava:3.0.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

apollo {
    service("service") {
        packageName.set("com.gandalf.connect")
        schemaFile.set(file("src/main/graphql/com/gandalf/connect/schema.graphqls"))
    }
}

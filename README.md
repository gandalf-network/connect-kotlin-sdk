# connect-kotlin-sdk

`connect-kotlin-sdk` is a Kotlin library that makes it easier to generate valid [Connect](https://docs.gandalf.network/concepts/connect) URLs that lets your users to link their accounts to Gandalf.

## Features

- Generate valid Connect URLs
- Parameter validation

## Getting Started

This section provides a quick overview of how to integrate the library into your project.

### Prerequisites

- Kotlin: >= v1.8.8 
- Gradle: >= v8.8
- JDK:    >= v11 

## Installation

### Using Gradle

To integrate GandalfConnect into your project, add it to your `build.gradle.kts` or `build.gradle` file:

#### build.gradle.kts

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.TosinJs:maven-try:1.0.0")
}
```

#### build.gradle

```groovy
repositories {
    mavenCentral()
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    implementation 'com.github.TosinJs:maven-try:1.0.0'
}
```

### Using Maven

To integrate GandalfConnect into your project, add it to your `pom.xml` file:

#### build.gradle.kts

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.TosinJs</groupId>
        <artifactId>maven-try</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Usage

### Importing the Library

In your Kotlin file where you want to use GandalfConnect, import the library:

```kotlin
// The Connect class
import com.gandalf.connect.Connect

// Useful Types
import com.gandalf.connect.types.ConnectInput
import com.gandalf.connect.types.InputData
import com.gandalf.connect.types.Service
```

### Initialization

Create an instance of `ConnectInput` with the necessary details:

```kotlin
val publicKey = ""
val redirectURL = "https://example.com"
val services: InputData = mutableMapOf(
    "uber" to Service(traits = listOf("rating"), activities = listOf("trip"))
)

val connectInput = ConnectInput(publicKey, redirectURL, services)
```

Initialize the `Connect` class:

```kotlin
val connect = Connect(connectInput)
```

### Generating URL

To generate a URL, call the `generateURL` method:

```kotlin
runBlocking {
    try {
        val generatedURL = connect.generateURL()
        println("Generated URL: $generatedURL")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
```

### Validations

The `generateURL` method performs several validations:

- **Public Key Validation**: Ensures the public key is valid.
- **Redirect URL Validation**: Checks if the redirect URL is properly formatted.
- **Input Data Validation**: Verifies that the input data conforms to the expected structure and contains supported services and traits/activities.

### Getting Data Key from URL

To extract the data key from a URL:

```kotlin
runBlocking {
    try {
        val dataKey = Connect.getDataKeyFromURL("https://example.com/?dataKey=a100")
        println("DataKey: $dataKey")
    } catch(Exception e) {
        println("Error: ${e.message}")
    }
}
```

## Contributing

We would love you to contribute to `connect-kotlin-sdk`, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The scripts and documentation in this project are released under the [MIT License](LICENSE.md)
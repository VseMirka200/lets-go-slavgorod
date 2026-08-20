# Third-party libraries

[Русский](LIBRARIES.md) | **English**

This document lists the main libraries and tools used by the “Let's Go! Slavgorod” project.

## Platform and UI

- AndroidX Core KTX;
- Lifecycle Runtime, ViewModel Compose, and Lifecycle Process;
- Activity Compose;
- Compose BOM, Compose UI, Material, Material 3, and Navigation Compose.

## Data and background work

- Kotlin Coroutines;
- AndroidX DataStore Preferences;
- Gson for JSON processing;
- OkHttp for network requests.

## Logging and dependency injection

- Timber for logging;
- Koin Android and Koin Compose for dependency injection.

## Testing

- JUnit;
- Kotlin Test;
- Mockito and Mockito Kotlin;
- Robolectric;
- MockWebServer;
- AndroidX Test Core, JUnit Ext, Espresso, and Compose UI Test.

## Build and static analysis

- Android Gradle Plugin;
- Kotlin Android and Kotlin Compose Compiler;
- Detekt;
- Desugar JDK Libraries.

Current dependency versions are defined by the project's Gradle configuration, primarily `gradle/libs.versions.toml`.

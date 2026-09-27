# Fix Gradle Sync Error: IllegalArgumentException 25.0.2

The project is failing to sync because it's being run with JDK 25, but the Kotlin version (2.0.21) and Android Gradle Plugin version (8.10.1) are too old to recognize this Java version. This causes a parsing error in the internal `JavaVersion` utility used by the Kotlin compiler and Gradle integration.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Lenovo/AndroidStudioProjects/Ecommerce-Frontend-Kotlin/gradle/libs.versions.toml)
- Update `kotlin` version to `2.4.20` to support JDK 25.
- Update `agp` version to `9.4.0` for compatibility with the newer Kotlin and Gradle versions.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Lenovo/AndroidStudioProjects/Ecommerce-Frontend-Kotlin/app/build.gradle.kts)
- Update `compileOptions` and `kotlinOptions` to use a more modern Java version (e.g., 21 or 25) if desired, though the main goal is fixing the sync error. I will set them to `21` as a safe baseline for now.

## Verification Plan

### Automated Tests
- Run `gradlew build` to ensure the project compiles with the new versions.
- Run `gradlew test` to verify unit tests.

### Manual Verification
- Perform a Gradle Sync in Android Studio to confirm the `IllegalArgumentException` is resolved.

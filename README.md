# Brain Pal — Stage 1

Native Android starter project using Kotlin, Jetpack Compose, Material 3, MVVM-ready structure, Coroutines, and Room dependencies.

## Stage 1 scope
- Buildable Compose application foundation.
- Honest empty state: no fake watch counts are displayed.
- Room dependencies are configured for the next stage; database entities and DAOs are not implemented yet.
- No accessibility, overlay, notification, Google sign-in, or home-screen widget permissions are requested in this stage.

## Toolchain
- Android Studio stable
- JDK 17 (use Android Studio's embedded JDK)
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.2.21
- compileSdk / targetSdk 36
- minSdk 26

## Important
This source archive does not include the Gradle wrapper JAR. Open the project in Android Studio and allow it to sync. If Android Studio asks to create/update the Gradle wrapper, use Gradle 8.13. Alternatively, after installing Gradle 8.13, run `gradle wrapper --gradle-version 8.13` from this directory.

## Next stages
1. Room entities, DAOs, database, repository, ViewModel, and persistence tests.
2. Goal editor and goal/streak calculation logic.
3. Accessibility-based detection service and per-platform detector interfaces.
4. Notifications, home-screen widget, overlay counter, exports and backup.
5. Optional Google sign-in.

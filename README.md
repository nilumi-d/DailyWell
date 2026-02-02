# DailyWell

DailyWell is a personal health and habits Android app (tracking habits, mood, hydration, reminders and achievements).

## Overview

DailyWell is an Android application that helps users build healthy routines by tracking daily habits, moods, and water intake. It includes reminders, achievements, and a clean Material-style UI.

## Features

- Habit tracking with add/edit/delete and reminders
- Mood logging and history
- Hydration tracking with quick-add water dialogs
- Achievement badges and streaks
- App widget and notifications

## Project Structure

- **app/** — Android app module containing source and resources.
- **app/src/main/** — Application source, resources and AndroidManifest.
- See important files:
  - [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml)
  - [app/build.gradle.kts](app/build.gradle.kts)
  - [build.gradle.kts](build.gradle.kts)

## Requirements

- Android Studio (recommended) or command-line Gradle
- JDK 11+
- Android SDK (API levels configured in the app module)

## Build & Run

From the project root you can use the Gradle wrapper.

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows (PowerShell or CMD)
.\\gradlew.bat assembleDebug
```

Open the project in Android Studio and run the `app` configuration on an emulator or device for the easiest workflow.

To run unit tests and instrumentation tests:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Where to look in the code

- UI layouts: `app/src/main/res/layout/`
- Drawables and icons: `app/src/main/res/drawable/`
- Main app module: `app/`


## Android Client for MG2

### Requirements

[Android SDK](https://developer.android.com/studio/index.html#downloads), [Gradle](https://gradle.org/install/), [JRE](https://www.java.com/download/)
or
[Android Studio](https://developer.android.com/studio)


### Install

#### Install on Android Studio

https://developer.android.com/studio/run/emulator#runningapp

1. Install Android Studio

2. Create an Android Virtual Device

3. In menu bar, click Run -> Run

#### Install via command line (Windows)

1. Have an Android emulator or device, and have it connected via adb

2. Navigate to project root folder and build a APK

  ```
  gradlew assembleDebug
  ```

3. Install via ADB

  ```
  adb install -r src\android-client\build\outputs\apk\debug\android-client-debug.apk
  ```

4. Run app on the emulator or via command:

  ```
  adb shell am start -a android.intent.action.MAIN -n app3cm.mg2/.MainActivity
  ```

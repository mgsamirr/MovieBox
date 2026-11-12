# MovieBox Android

This is the first native Android application shell for MovieBox, separate from the existing Rust terminal UI.

## Current state

- Native Kotlin + Jetpack Compose application module.
- English navigation for Movies, Web Series, Anime, Search, Downloads and Settings.
- A title is eligible only when an available audio track is explicitly identified as Hindi (language code hi, hin or hindi).
- No Live TV, IPTV or live-stream section.
- The catalog is intentionally empty until an authorized content provider is integrated. No sample titles or playback links are invented.

## Supplied decompiled ZIP

The supplied com.community.oneroom archive is a decompiled Android package, not a complete Gradle source project. Its manifest and resources show a OneRoom/Transsion app with movie, TV, anime, download and player screens, plus unrelated live, games, rewards, advertising and mini-app features.

The archive is being treated as a permitted reference. The compiled DEX is not a drop-in source project. Reuse only independently reusable code/resources and appropriate UI concepts; do not transplant app-signature hooks, patching code, unrelated proprietary SDKs or services. MovieBox catalog and playback integration should come from the repository's licensed source or authorized provider APIs.

## Build

Open this directory in Android Studio with JDK 17 and Android SDK 35 installed, then build the app debug variant.

This starter is not yet connected to a catalog or playback provider and is not a release APK.

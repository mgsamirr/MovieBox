# MovieBox — Android Mobile

MovieBox is a native, touch-first Android phone app for on-demand movies, anime, and web series.

## Viewing modes
- **Watch online:** play an available on-demand movie or episode from its authorized HTTPS video source inside the app.
- **Download and watch offline:** download an available title to the phone and play the saved local file later without internet.
- The same on-demand title can support both options when its provider supplies both a playable VOD source and a direct-download source.

## Included app scope
- Android phones only; portrait, touch-first interface.
- Movies, anime, and web series; Hindi-dubbed discovery/filtering is planned.
- AMOLED black UI with Compose navigation and animations.
- Media3 player for on-demand playback.
- Android DownloadManager foundation for direct downloads.
- Local library and offline playback.

## Explicitly excluded
- Web browser, website, and WebView-based browsing.
- Android TV / TV launcher / remote-control UI.
- Live TV, IPTV, M3U playlists, and live channels.
- Terminal UI, CLI commands, keyboard/mouse terminal navigation, terminal themes, Ratatui/Crossterm.

**Important:** Online playback is for on-demand movies and episodes only, not live broadcasts. Actual catalogue and provider integration must be connected to sources that authorize playback and downloads. The current scaffold contains the player and download foundations; it is not yet a complete media catalogue or production APK.

## Build
Open in Android Studio with Android SDK 36 and JDK 17, sync Gradle, then run the app configuration on an Android phone.

Package: com.moviebox.mobile

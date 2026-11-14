# MovieBox — Android Mobile (Offline-first)

MovieBox is being rebuilt as a native Android phone app.

## Product scope
- Android phones only; touch-first portrait UI.
- Movies, anime, and web series, with Hindi-dubbed metadata/filtering planned.
- Download first, watch locally after the download completes.
- Download manager support for pause/resume and progress will be integrated with approved direct-download sources.
- Local playback only. The player accepts local/content URIs; it does not open network stream URLs.

## Explicitly out of scope
- Web browser, website, WebView-based browsing.
- Android TV / TV launcher / remote-control UI.
- Live TV, IPTV, M3U playlists, and live streams.
- Online video streaming or network playback.
- Terminal UI, CLI commands, keyboard/mouse terminal navigation, terminal themes, Ratatui/Crossterm.

## Build
Open this repository in Android Studio (recent stable version with Android SDK installed), then sync Gradle and run the `app` configuration on an Android phone.

Package: `com.moviebox.mobile`

## Download sources
A real catalogue and direct-download provider must be configured separately. Only use sources that explicitly authorize downloading and redistribution. The app scaffold does not include third-party copyrighted media or hidden provider credentials.

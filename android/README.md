# Movie Box — private Android app

Movie Box is intended for personal installation and private use. It is not being prepared for public Play Store or Chrome Web Store distribution.

## Included app behaviour

- Native Android app using Kotlin and Jetpack Compose.
- English interface with Movies, Web Series, Anime, Search, Downloads, Watchlist and Settings.
- AMOLED-black visual theme.
- Only titles with provider-confirmed, available and verified Hindi audio from an authorized source may enter the catalog.
- No Live TV, IPTV, public streaming links, fake catalog entries, or unlicensed content.
- Watchlist is stored locally on the device.
- No account or sign-in is required by the current app shell.
- No analytics, advertising SDK, crash-reporting SDK or third-party tracking SDK is included.
- No storage, contacts, location, microphone, camera or notification permission is requested.
- No automatic app updates or background DRM-license renewal jobs are configured. Cleartext HTTP traffic is blocked; HTTPS is required for any future provider connection.

## Private installation

The debug APK is suitable for development/testing. For a personal long-term installation, create a private release APK and sign it with a keystore kept only on the owner's device or secure password manager. Never commit the keystore, passwords, API tokens or signing properties to GitHub.

After installing a signed release, keep the same keystore safe: Android requires the matching signing identity to update that installed app. If the keystore is lost, an update may require uninstalling the old app, which can erase local app data.

The APK can be transferred privately to the owner's Android phone (for example, USB or a private file transfer) and installed after allowing that file source in Android settings. Do not share the APK publicly.

## Build

- JDK 17
- Android SDK 35
- Gradle 8.9
- Debug: `gradle -p android testDebugUnitTest assembleDebug`
- Release: `gradle -p android testReleaseUnitTest assembleRelease` (release signing must be configured locally before a signed distributable is produced).

## DRM player integration

The supplied archive contains AndroidX Media3 `1.7.1` DRM/player framework classes (`DefaultDrmSessionManager`, `DrmInitData`, Widevine support and offline-license helper APIs). Movie Box pins `androidx.media3:media3-exoplayer:1.7.1` to match that framework generation and uses Media3's supported DRM session configuration for authorized provider streams. The archive also contains other player forks; those are not mixed into the app because their package/API surface is different. No app-specific license URL, provider credentials, or offline-license entitlement is assumed. No automatic app updates or background DRM-license renewal jobs are configured. Protected offline downloads are not enabled until an authorized provider's offline-license contract is available.

## Catalog and playback status

The catalog is deliberately empty until a legitimate content provider and its authorized API credentials are supplied/configured. The app does not invent movie names, Hindi audio availability, posters, or playback URLs. Playback and offline downloads are not advertised as working until a provider supports them.

## Reference archive

The supplied decompiled OneRoom package is treated only as an authorized design/behaviour reference. Unrelated ads, rewards, games, live streams, mini-apps, tracking integrations, signature hooks, patching code and proprietary SDKs are not part of this private app.

# Decompiled Android reference analysis

This note records a read-only inspection of the user-provided `com.community.oneroom_code.zip` archive. The archive is used as a compatibility and product-structure reference for an independently implemented MovieBox Android app. It is not treated as a complete source project.

## Archive identity and contents

- Android package in the decoded manifest: `com.community.oneroom`.
- Version in the decoded manifest: `4.0.03.1029.03`.
- Target SDK: 35; minimum SDK: 23.
- The archive contains a decoded Android manifest, two DEX class inventories, decoded resource XML files, and app assets.
- It does not contain a conventional Gradle project or readable original Java/Kotlin source tree. The main DEX inventory is mostly obfuscated bootstrap/loader classes; the second is a class/member inventory rather than full method source.

## Product flows visible in the manifest

The manifest exposes named app components that indicate these product areas:

- Movie detail and discovery: `MovieDetailActivity`, `MovieStaffActivity`, `MoviePosterActivity`, `SubjectListActivity`, `MovieFilterActivity`, `RankAllActivity`.
- Search: `SearchManagerActivity`.
- On-demand video playback: `TestLongVodActivity`, plus Media3/ExoPlayer-related service declarations.
- Downloads: `DownloadPanelActivity`, `DownloadSeriesListActivity`, `TransferActivity`, and downloader foreground/guard services.
- The manifest also contains `TvLiveDetailActivity`, community/room features, mini-app/WebView activities, payment and advertising SDK components.

These names are evidence of component areas only; they do not establish the complete screen design, backend API, or playback URL protocol.

## MovieBox implementation scope

Use the archive as a guide for the broad mobile content-app structure: discovery, title details, search, on-demand player, episode selection, and download management. Rebuild those areas in MovieBox with original implementation and styling.

Keep the user's requirements:
- App name: MovieBox.
- Android phone UI only, AMOLED black theme, premium motion and touch-first navigation.
- Movies, anime, and web series, with Hindi-dubbed discovery prioritized.
- On-demand online playback and direct downloads for offline playback.
- Exclude live TV/live channels, IPTV/M3U, website/browser navigation, Android TV layouts, and terminal interfaces.

## Reverse-engineering boundary

Do not transplant the decompiled app's obfuscated bootstrap, native loader, signature spoofing, patch/interceptor hooks, payment/ad SDK logic, or private service endpoints. Do not copy proprietary implementation code or protected assets. Reimplement only independently authored app behavior using authorized content metadata, playback URLs, and download URLs.

## Next engineering steps

1. Build a native MovieBox catalogue model and repository interface for authorized title metadata.
2. Add home/discovery rails, title detail, seasons/episodes, and search using the mobile reference structure.
3. Connect the same authorized VOD source to Media3 playback and the download manager.
4. Persist download records and expose a proper offline library with progress, retry, and delete actions.
5. Keep all live-channel entry points out of navigation and provider models.

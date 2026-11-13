# OneRoom reference adaptation

The supplied OneRoom archive is a decompiled Android package, not an editable Android Studio project. It is used as a reference for product behavior and visual patterns while Sherif Movie Box remains an independently built Kotlin/Jetpack Compose application.

## Product patterns carried into Sherif Movie Box

- Category-based discovery, adapted to Movies, Web Series, and Anime.
- Search and category filtering.
- A dedicated downloads destination, ready for a licensed provider's offline capability.
- Account/settings destination and viewing preferences.
- Dark-mode-first viewing surfaces and clear empty/loading states.
- A native Media3 playback surface supports authorized Widevine streams once a provider supplies the media and license configuration.
- A separate local-video player opens only a file explicitly selected through Android’s document picker.

## Deliberately not copied or enabled

- OneRoom DEX files, compiled classes, obfuscated runtime code, signing/patch hooks, or proprietary SDK binaries.
- Ad attribution, analytics, advertising identifiers, push, Transsion-specific integrations, and unrelated permissions.
- WebView bridge scripts or encrypted/obfuscated runtime assets.
- Network interception, response rewriting, DRM circumvention, or extraction of protected media URLs.
- OneRoom branding, remote artwork, or assets whose redistribution rights cannot be established.
- Live TV/IPTV and live-stream features.

## Android implementation boundary

`AuthorizedCatalogSource` is the integration point for a provider that the app owner is authorized to use. `CatalogRepository` applies the same strict Hindi-audio eligibility policy before titles reach the UI. Playback eligibility also fails closed unless the provider confirms authorized distribution and a verified, available Hindi audio track.

No catalog is fabricated while a provider is absent. Provider credentials must not be embedded in the APK; use a backend or provider-approved secure authentication flow when one is available.

## Current status

The native Android shell includes Home, Search, Downloads, Watchlist and Settings, plus Movies/Web Series/Anime categories and verified-Hindi filtering. Local file playback uses Media3 and keeps video and audio disabled until a Hindi language track is exposed. The authorized Widevine player surface and continue-watching persistence are implemented, but the online catalog remains empty and online DRM playback/offline downloads remain unavailable until a compatible licensed provider is configured.
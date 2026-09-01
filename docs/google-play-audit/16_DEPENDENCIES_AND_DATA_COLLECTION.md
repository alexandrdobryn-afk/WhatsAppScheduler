# Dependencies and Data Collection

Direct dependency families:

- AndroidX Core
- AndroidX Lifecycle
- AndroidX Activity Compose
- Jetpack Compose BOM / Material3
- AndroidX Navigation Compose
- Room
- DataStore
- WorkManager
- Hilt
- Kotlin coroutines

No evidence found for:

- Firebase
- Crashlytics
- Google Analytics
- Sentry
- AdMob
- Billing
- Retrofit
- OkHttp as direct app networking
- Ktor
- AppsFlyer
- Adjust
- Facebook SDK

Important note:

Transitive utility libraries such as Okio can appear through AndroidX/DataStore dependency graphs. That is not evidence of network collection by itself.

Network permission:

- `INTERNET` is not declared.

Dependency freshness:

Lint reports several AndroidX/Kotlin ecosystem dependencies have newer stable versions. This is not an immediate Play blocker, but API 36 migration should update the Android Gradle Plugin and AndroidX set carefully.

Data collection conclusion:

Based on manifest and source/dependency scans, the current release does not transmit user data to the developer. Runtime network verification was not performed.

Recommended ongoing control:

Before each release, run dependency and permission scans again and diff the release manifest against the previous release.

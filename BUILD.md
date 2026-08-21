# Сборка WA Schedule

## Требования

- Android Studio Ladybug (2024.2) или новее, либо командная строка с JDK 17 и Android SDK.
- Android SDK Platform 35, Build-Tools совместимые с AGP 8.5.2.
- Установленный на тестовом устройстве официальный WhatsApp (`com.whatsapp` или `com.whatsapp.w4b`).

## Первая сборка в Android Studio

1. `File → Open` → выбрать корень репозитория (папку с `settings.gradle.kts`).
2. Дождаться Gradle sync (первый раз скачает Compose BOM, Room, Hilt, WorkManager и т.д. — нужен доступ в интернет).
3. Выбрать конфигурацию `app` и запустить на устройстве/эмуляторе с Android 12+.

## Сборка из командной строки

```powershell
# Debug APK
.\gradlew.bat :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Signed release APK. First create local signing files with:
# .\scripts\create-release-keystore.ps1
.\gradlew.bat :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Подробности приватной release-сборки и публикации через GitHub Releases:
см. `RELEASE.md`.

## Тесты

```powershell
# Модульные тесты (Scheduler / Deduplication-логика / RetryPolicy / OccurrenceId)
.\gradlew.bat :app:testDebugUnitTest

# Инструментальные тесты (требуют подключённое устройство/эмулятор) —
# ExecutionDao.tryClaim: dedup и concurrent-claim
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Lint

```powershell
.\gradlew.bat :app:lintDebug
```

## Известные ограничения окружения сборки

- Первая сборка требует доступа в интернет для скачивания Gradle-зависимостей
  (AGP, Kotlin, Compose, Room, Hilt, WorkManager, DataStore). Если сборка
  выполняется в изолированной среде без сети, нужно заранее прогреть
  локальный Gradle/Maven кэш или настроить offline-репозиторий.
- Release-сборка требует локальный `keystore.properties` или переменные
  окружения `WASCHEDULER_*`. Без них `assembleRelease` не должен считаться
  готовым приватным релизом.

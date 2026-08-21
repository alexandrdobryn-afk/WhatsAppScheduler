# WA Schedule

**Scheduled WhatsApp Messages for Android.** WA Schedule is a local Android app
for scheduling WhatsApp group messages from a personal phone: choose a chat,
message text, start date, weekdays, and exact times, then the app opens the
official WhatsApp app and sends at the configured time.

Download the latest signed APK from
[GitHub Releases](https://github.com/alexandrdobryn-afk/WhatsAppScheduler/releases/latest/download/app-release.apk).

Keywords: WhatsApp scheduler Android, scheduled WhatsApp messages, automatic
WhatsApp group messages, Android WhatsApp scheduled sender, personal WhatsApp
automation, Kotlin, Jetpack Compose, AccessibilityService.

Локальный Android-планировщик, который от имени владельца устройства
автоматически выполняет заранее заданное действие — ввод и отправку
заготовленного текста — в уже установленном официальном WhatsApp, по
расписанию. Никакого backend, никакого WhatsApp Business API, никакого root.

## Что делает приложение

- Пользователь создаёт правило: название WhatsApp-группы, текст сообщения,
  список времён (например 08:00, 09:00, 10:00) и дни недели.
- В заданное время приложение открывает WhatsApp, находит нужную группу,
  проверяет, что открыт именно тот чат, вводит текст и нажимает «Отправить».
- Отправителем всегда является текущий WhatsApp-аккаунт владельца телефона.

## Архитектура

```
UI (Compose) → ViewModel → Domain (use cases) → Repository → Room
                                                     ↓
                                    Scheduler (AlarmManager, DB = source of truth)
                                                     ↓
                                        AutomationEngine (prechecks, dedup, retry)
                                                     ↓
                                  WhatsAppAccessibilityService → WhatsAppUiAdapter
```

Ключевые модули:

- `core/scheduler` — `AlarmScheduler`: точные будильники (`AlarmManager`),
  БД всегда источник истины, будильник пересчитывается заново при любом
  сомнении (перезагрузка, смена времени, изменение правила).
- `core/automation` — `AutomationEngine` (весь пайплайн: precheck → claim →
  запуск → результат → лог → уведомление), `ExecutionWorker`/`RetryWorker`
  (гарантированное фоновое выполнение через WorkManager), `RetryPolicy`.
- `core/accessibility` — `WhatsAppAccessibilityService` (жёстко ограничен
  пакетами `com.whatsapp`/`com.whatsapp.w4b`), `AutomationStateMachine`
  (явный конечный автомат: LAUNCHING → FINDING_CHAT → VERIFYING_CHAT →
  FINDING_INPUT → SETTING_TEXT → FINDING_SEND_BUTTON → SENDING → VERIFYING →
  SUCCESS/FAILED), `WhatsAppUiAdapter` — единственное место, где живут все
  WhatsApp-специфичные селекторы.
- `data/` — Room (`rules`, `rule_times`, `execution_logs`) + DataStore
  (глобальный переключатель, тема, настройки уведомлений).
- `feature/` — Home, History, Rule Editor, Settings, Diagnostics, Onboarding.

## Требования

- Android 12 (API 31) и новее. Собрано и рассчитано на тестирование вплоть
  до Android 16 (реальное тестирование на устройствах — см. «Тесты» ниже,
  средой сборки автоматический прогон на физических Android-версиях не
  выполнялся, см. «Известные ограничения»).
- Установленный официальный WhatsApp (обычный личный аккаунт).

## Разрешения

| Разрешение | Зачем |
|---|---|
| Accessibility Service | Единственный способ программно взаимодействовать с UI стороннего приложения (WhatsApp) без API — обязателен для отправки |
| Уведомления | Отчёт об успехе/ошибке отправки |
| Exact alarms (`SCHEDULE_EXACT_ALARM`) | Точное время срабатывания, а не «примерно», как у WorkManager |
| Игнорирование battery optimization | Рекомендуется, но не обязательно — без него агрессивные OEM-прошивки (Xiaomi, Samsung и др.) могут убивать фоновые задачи |

Экран онбординга при первом запуске показывает **реальный** статус каждого
разрешения и никогда не выдаёт «всё готово», если что-то не разрешено.

## Как установить

1. Для постоянного использования собрать или скачать подписанный release APK
   (см. `RELEASE.md`). `app-debug.apk` использовать только для разработки.
2. `adb install -r app-release.apk` либо установить вручную на устройстве.
3. Открыть приложение — появится экран разрешений.

Для GitHub-дистрибуции загружайте именно
`app/build/outputs/apk/release/app-release.apk` в GitHub Release. Keystore,
`keystore.properties` и пароли нельзя публиковать в репозитории.

## Как настроить Accessibility

1. На экране онбординга (или Настройки → Диагностика) нажать «Открыть
   настройки» напротив пункта Accessibility.
2. В системных настройках Android найти «WA Schedule - автоматизация»
   и включить.
3. Вернуться в приложение — статус обновится автоматически при возврате.

## Как создать правило

1. Главная → «+ Добавить расписание».
2. Указать название WhatsApp-группы **точно как в WhatsApp** (приложение
   ищет чат через встроенный поиск WhatsApp по названию — см. «Известные
   ограничения» про надёжность этого механизма).
3. Ввести текст сообщения (до 4000 символов).
4. Добавить одно или несколько времён и дни недели.
5. Задать допустимое опоздание (по умолчанию 10 минут) — если телефон не
   успел выполнить действие в это окно, occurrence помечается `SKIPPED /
   MISSED_WINDOW`, а не отправляется с опозданием.
6. Сохранить.

## Как выполнить Dry Run

В редакторе правила — «Проверить без отправки». Приложение пройдёт весь путь
(открыть WhatsApp → найти группу → открыть чат → проверить заголовок → найти
поле ввода → найти кнопку отправки) и **остановится перед нажатием Send**,
показав, какой шаг не удался, если не удался.

## Как выполнить Test Send

В редакторе правила — «Тестовая отправка». Это единственное место, где перед
реальной отправкой запрашивается явное подтверждение с превью группы и
текста.

## Известные ограничения

- **Официального API нет.** Обычный личный WhatsApp не предоставляет
  программного способа отправки сообщений. Всё построено на
  `AccessibilityService`, то есть на автоматизации пользовательского
  интерфейса официального приложения.
- **Нет гарантии доставки.** Приложение фиксирует только то, что команда
  отправки была выполнена в интерфейсе WhatsApp (`SENT`), а не то, что
  сообщение доставлено, прочитано или получено сервером. Это принципиальное
  архитектурное ограничение, а не недоработка.
- **Хрупкость к обновлениям WhatsApp.** WhatsApp не публикует стабильный
  контракт для внешних инструментов; `resource-id`, `contentDescription` и
  структура экрана могут измениться в любом обновлении. Весь WhatsApp-
  специфичный код изолирован в `WhatsAppUiAdapterImpl` и снабжён
  семантическими fallback'ами (className/editable/clickable), но при
  достаточно крупном изменении интерфейса потребуется обновление адаптера.
  Диагностика → «Проверить WhatsApp» позволяет обнаружить это заранее, без
  отправки сообщений.
- **Ограничения производителей (OEM).** Xiaomi/Samsung/Huawei/OnePlus/Oppo/
  Realme и другие могут агрессивно ограничивать фоновую работу поверх
  стандартного Android Doze/App Standby. Приложение не пытается это
  обходить — только показывает статус в Диагностике и просит пользователя
  вручную снять ограничение в системных настройках.
- **Заблокированный экран.** Если Android не позволяет выполнить действие
  при заблокированном устройстве, приложение НЕ пытается разблокировать
  телефон — фиксирует `DEVICE_LOCKED` и повторяет попытку в пределах
  допустимого опоздания.
- **Соответствие условиям использования WhatsApp.** Автоматизация
  интерфейса стороннего приложения, даже без обхода защит и без массовой
  рассылки, может противоречить пользовательскому соглашению WhatsApp
  (запрет на неавторизованную автоматическую отправку сообщений). Это
  ответственность пользователя приложения, а не техническое ограничение —
  явно фиксируется здесь по требованию прозрачности.
- **Дублирование названий групп.** Если в WhatsApp есть две группы с
  одинаковым названием, приложение не отправляет сообщение ни в одну из них
  и помечает occurrence как `AMBIGUOUS_CHAT`, требуя ручного вмешательства.
- **Debug-экран состояния** (`app/src/debug/.../DebugStateScreen.kt`)
  реализован, но не подключён к навигационному графу (чтобы не тянуть
  debug-only зависимости в release source set через общий Gradle-модуль);
  для использования его нужно вручную подключить к своему debug-flavour'у
  или отдельному debug-only NavHost.
- **Реальное end-to-end тестирование с живым WhatsApp** (спецификация,
  раздел 87-88) в этой среде разработки не проводилось: здесь нет
  Android-эмулятора/устройства и нет доступа в интернет для скачивания
  Android SDK/эмулятора. Написаны модульные тесты (`app/src/test`) для
  чистой доменной логики (Scheduler, дедупликация ID, RetryPolicy) и
  инструментальный тест (`app/src/androidTest`) для `ExecutionDao.tryClaim`
  (dedup/race). Перед реальным использованием обязательно нужно прогнать
  ручные сценарии из раздела 87 ТЗ на физическом устройстве.

## Структура репозитория

```
app/src/main/java/com/example/wascheduler/
  core/        scheduler, accessibility, automation, permissions, logging, notifications
  data/        database, dao, entity, repository
  domain/      model, repository, usecase
  feature/     home, rule_editor, history, settings, diagnostics, onboarding
  service/     AlarmReceiver, BootReceiver, TimeChangeReceiver
  di/          Hilt-модули
app/src/test/          модульные тесты
app/src/androidTest/    инструментальные тесты
app/src/debug/          debug-only экран
```

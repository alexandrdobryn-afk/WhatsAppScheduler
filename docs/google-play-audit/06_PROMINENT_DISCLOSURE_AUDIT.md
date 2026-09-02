# Prominent Disclosure Audit

Current implementation:

- Accessibility disclosure strings exist in English, Russian, and Ukrainian resources.
- Disclosure is shown from onboarding before opening Android Accessibility settings.
- Disclosure is shown from settings before opening Android Accessibility settings.
- Dialog includes a cancel/decline path.
- Dialog includes an explicit continue/consent action.

Remediation result:

- Disclosure must now refer only to official consumer WhatsApp support.
- It must not mention WhatsApp Business unless support is re-added and tested.
- It must not imply that data is sent to the developer.
- App resource strings now match the final disclosure meaning in English, Russian, and Ukrainian.

Final disclosure wording prepared for Play review:

English:

WA Schedule uses Android Accessibility Service to perform WhatsApp actions that you configure. The service accesses visible WhatsApp interface elements only to verify the chat or group you selected, find the message field and Send button, enter your saved message, and send it at the scheduled time. Your saved message is transferred to WhatsApp as part of the action you requested. WA Schedule does not store your WhatsApp conversation history or send Accessibility data to the developer or to WA Schedule servers.

Russian:

WA Schedule использует Android Accessibility Service для выполнения действий WhatsApp, которые вы настраиваете. Сервис обращается только к видимым элементам интерфейса WhatsApp, чтобы проверить выбранный вами чат или группу, найти поле сообщения и кнопку Send, ввести сохранённое вами сообщение и отправить его в запланированное время. Ваше сохранённое сообщение передаётся в WhatsApp как часть действия, которое вы запросили. WA Schedule не сохраняет историю ваших переписок WhatsApp и не отправляет данные Accessibility разработчику или на серверы WA Schedule.

Ukrainian:

WA Schedule використовує Android Accessibility Service для виконання дій WhatsApp, які ви налаштовуєте. Сервіс звертається лише до видимих елементів інтерфейсу WhatsApp, щоб перевірити вибраний вами чат або групу, знайти поле повідомлення і кнопку Send, ввести збережене вами повідомлення та надіслати його у запланований час. Ваше збережене повідомлення передається до WhatsApp як частина дії, яку ви запросили. WA Schedule не зберігає історію ваших переписок WhatsApp і не надсилає дані Accessibility розробнику або на сервери WA Schedule.

Status:

- Placement: PASS
- Explicit consent: PASS
- Decline path: PASS
- Final wording in app resources: PASS.
- Declaration alignment: READY.

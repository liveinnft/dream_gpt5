# DreamTracker — Техническая документация

Документ описывает архитектуру, модули, зависимости и ключевые технические решения Android‑приложения DreamTracker.

## Стек и версии
- Язык: Kotlin
- UI: Jetpack Compose (Material 3), Navigation (Accompanist Navigation Animation)
- Архитектура: MVVM‑лайт (экраны + репозитории), Room для хранения
- Асинхронность: Coroutines/Flows
- Хранение настроек: DataStore Preferences
- Сеть: Retrofit + OkHttp + Moshi
- Аналитика снов: OpenRouter (Chat Completions API)
- Локальная БД: Room (SQLite)
- Минимальная версия Android: 24 (7.0)

## Структура проекта
- `app/src/main/java/com/example/dreamtracker`
  - `MainActivity.kt` — навигация, AppBar, темы, переходы (Accompanist AnimatedNavHost)
  - `ui/theme/*` — цвета, типографика, тема Material 3
  - `data/*` — Room (сущности, DAO, БД), репозитории
  - `feature/recording/AudioRecorder.kt` — запись аудио (MediaRecorder)
  - `network/OpenRouterService.kt` — клиент OpenRouter (Retrofit)
  - `secrets/OpenRouterKeyProvider.kt` — чтение ключа из файла/ассетов
  - `settings/SettingsRepository.kt` — DataStore: модель, демо‑лимит, напоминания, статус валидации, онбординг, черновик
  - `screens/*` — Compose‑экраны (список, запись, детали, настройки, статистика, онбординг)
  - `export/*` — экспорт JSON и отчётов (PDF/PNG)
  - `notifications/*` — ежедневные напоминания (AlarmManager + BroadcastReceiver)
  - `ui/charts/Charts.kt` — простые графики (Bar/Pie) на Canvas
- `app/src/main/assets/dream_symbols.json` — словарь символов

## Данные и БД
- `Dream` (Room Entity):
  - `id`, `createdAtEpoch`, `title`, `moodScore`, `audioFilePath`, `transcriptText`
  - Структурированный анализ: `summary`, `symbolsMatched`, `insights`, `recommendations`, `tone`, `confidence`, `analysisJson`
  - UX: `isFavorite`, `tags`
- DAO:
  - `observeAll()`: Flow<List<Dream>> — список
  - `getAll()`, `getById(id)`, `getBetween(start,end)`
  - `upsert(dream)`, `update(dream)`, `deleteById(id)`
- БД: `AppDatabase` (version 3, `fallbackToDestructiveMigration`)

## Настройки и DataStore
Ключи:
- `openrouter_model`, `demo_uses`, `reminder_on`, `reminder_hour`, `reminder_min`
- `key_valid`, `validation_msg`, `onboarded`
- Черновик записи: `draft_title`, `draft_tags`, `draft_text`, `draft_mood`
Потоки: `Flow<T>` для реактивного UI; `suspend`‑сеттеры для записи.

## Сеть и OpenRouter
- Базовый URL: `https://openrouter.ai/`
- Эндпоинты: `POST api/v1/chat/completions`, `GET api/v1/models`
- Заголовки: `Authorization: Bearer <key>`, `HTTP-Referer`, `X-Title`
- Формат запроса: Chat Completions с сообщениями `system/user`.
- Клиент создаётся с/без ключа (для демо), логирование OkHttp — BASIC.

## Анализ снов
- Правило‑ориентированный: поиск символов по словарю (`dream_symbols.json`) в тексте сна
- LLM (OpenRouter):
  - Жёсткий промпт — ответ строго JSON с полями: `summary`, `insights`, `recommendations`, `tone`, `confidence`
  - Ограничения длин для компактного UI
  - Парсинг Moshi в `LlmAnalysis`
- Демо‑режим: без ключа разрешено 5 анализов (счётчик в DataStore)

## Экспорт/импорт
- JSON: `ExportImportManager` — экспорт списка снов, импорт в БД как новые записи
- FileProvider (`res/xml/file_paths.xml`, провайдер в манифесте) — безопасный шаринг
- Отчёт PDF/PNG: `ReportGenerator` — канвас‑рендер одной страницы A4 (~1240x1754) с:
  - пастельным градиентом,
  - карточками (скругления, тени),
  - мини‑логотипом (луна),
  - метриками, легендами, сетками,
  - диаграммами в стиле бара (тональность) и топ‑символов,
  - списком снов, подвалом.
- Директория экспорта: `files/exports/`.

## UI и навигация
- Основные экраны: список, запись, детали, статистика, настройки, онбординг
- Навигация: Accompanist `AnimatedNavHost`
  - Переходы по shared axis (scale+fade), обратные — зеркально
- Анимации UI:
  - Плавное появление главной кнопки и карточек
  - Пульсация кнопки записи и блока плеера
  - Раскрытие секций анализа (AnimatedVisibility)
  - Разворачивание карточек в списке по тапу
- Темизация: Material 3; собственная палитра пастельных синих тонов для светлой/тёмной тем

## Запись и аудио
- `MediaRecorder` (AAC, 44.1kHz, 128 kbps) → `files/recordings/*.m4a`
- Безопасно завершается и освобождает ресурсы; обработка ошибок

## Напоминания
- `AlarmManager.setRepeating` (ежедневно) + `BroadcastReceiver`
- Канал уведомлений `dream_reminders`
- Управление со списка; время хранится в DataStore
- На Android 13+: запрос `POST_NOTIFICATIONS`

## Безопасность/приватность
- Ключ OpenRouter не хранится в коде, читается из `files/openrouter_key.txt` или `assets/openrouter_key.txt` (для разработки)
- Все пользовательские данные в sandbox приложения

## Сборка/зависимости
- Основные зависимости:
  - Compose BOM, Material3, Navigation, Accompanist (permissions + navigation‑animation)
  - Room, Retrofit, OkHttp, Moshi, DataStore
- Java/Kotlin: 17, Kotlin K2 compiler ext `1.5.14`
- Для сборки: Gradle 8.7 wrapper

## Обработка ошибок и UX-защита
- Сеть: try/catch, сообщения “Ключ не найден”, “Ошибка загрузки”
- Анализ: ограничение длин полей, фолыбэк‑тексты
- Права: явные запросы и статусы
- Черновик: автосохранение на вводе, очистка после сохранения

## Точки расширения
- Глубокие графики (MPAndroidChart/Compose Charts)
- Облачные бэкапы/синхронизация
- Локальная/offline STT/аналитика
- Мультиязычность
- CI/CD для сборки APK/релизов

## Тестирование/проверка
- Сборка в Android Studio, проверка lint/inspections
- Рекомендация: включить detekt/ktlint, добавить юнит‑тесты на парсинг JSON и репозитории

## Известные нюансы
- В окружении нет `gradle-wrapper.jar` — необходимо сгенерировать wrapper перед сборкой
- Accompanist 0.35.2‑alpha — при обновлении Compose возможно потребуется синхронизация версий

---
Эта документация призвана помочь быстро разобраться в коде и архитектуре, а также безопасно расширять функциональность проекта.
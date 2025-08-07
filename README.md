# DreamTracker (Android)

Приложение для записи и анализа снов:
- Голосовая запись сна + распознавание речи (системное)
- Две темы: светлая и тёмная (в мягкой пастельной синей гамме)
- Анализ:
  - Правила: список символов в `app/src/main/assets/dream_symbols.json`
  - Нейросеть: через OpenRouter (LLM), ключ хранится отдельно

## Быстрый старт
1. Откройте проект в Android Studio (Arctic Fox+). Установите SDK 34.
2. Поместите OpenRouter API key в один из вариантов:
   - Внутренняя память приложения на устройстве: `/data/data/com.example.dreamtracker/files/openrouter_key.txt` (создастся после установки)
   - Либо положите файл `app/src/main/assets/openrouter_key.txt` (в репозитории он `.gitignore`)
3. Запустите на устройстве/эмуляторе (мин. Android 7.0, API 24).

## Расширение символов
Файл `app/src/main/assets/dream_symbols.json` — список объектов `{ "symbol": "...", "meaning": "..." }`.
Добавляйте новые записи, перезапускайте приложение.

## Примечания
- Модель по умолчанию: `openai/gpt-4o-mini` через OpenRouter. Можно изменить в `OpenRouterService.DEFAULT_MODEL`.
- Хранение ключа в assets подходит для разработки. Для продакшна используйте безопасное хранение.
- Голосовая запись сохраняется в `files/recordings/*.m4a` внутри sandbox приложения.
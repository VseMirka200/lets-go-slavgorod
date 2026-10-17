# Проверки безопасности и качества

Репозиторий проверяет каждый pull request в `main`:

- `Tests, lint and Detekt`: unit-тесты, Android Lint и Detekt;
- `Dependency review`: блокирует добавление зависимостей с уязвимостями уровня `moderate` и выше в runtime-области;
- `CodeQL`: ищет потенциальные уязвимости в Kotlin/Java-коде;
- `Validate schedule data`: проверяет структуру и данные расписания при их изменении.

## Обязательные настройки GitHub

Автоматические проверки сами по себе не запрещают прямую запись в ветку. В настройках репозитория откройте **Settings → Rules → Rulesets** и создайте правило для `main`:

1. включите **Require a pull request before merging**;
2. включите **Require approvals** и установите минимум один review;
3. включите **Require review from Code Owners**;
4. включите **Require status checks to pass** и выберите `Tests, lint and Detekt`, `Dependency review`, `CodeQL` и `Validate schedule data`;
5. включите **Block force pushes** и запретите удаление ветки;
6. не добавляйте обход правила для обычных участников.

В разделе **Settings → Code security and analysis** включите Secret scanning и Push protection. Они не дадут отправить в GitHub распознанные токены и ключи.

Перед слиянием проверяйте изменения в `.github/`, `gradle/`, `build.gradle.kts`, `app/build.gradle.kts` и сетевом коде. Не добавляйте секреты, ключи подписи, токены или локальные файлы в Git.

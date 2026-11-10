# Проверки безопасности и качества

Репозиторий проверяет каждый pull request в `main`:

- `Tests, lint and Detekt`: unit-тесты, Android Lint, Detekt и сборка debug APK;
- `Dependency review`: блокирует добавление зависимостей с уязвимостями уровня `moderate` и выше в runtime-области;
- `CodeQL`: ищет потенциальные уязвимости в Kotlin/Java-коде;
- `Validate schedule data`: запускает тесты валидатора опубликованного расписания на каждом PR; реальный источник проверяется отдельно при ручном запуске workflow.

## Локальные проверки

Из корня репозитория:

```bash
./gradlew --no-daemon --dependency-verification=strict testDebugUnitTest lintDebug detekt assembleDebug
node --test tools/*.test.mjs
python tools/check-links.py
```

Для ручной проверки источника можно выполнить
`node tools/validate-published-schedule.mjs URL`, где URL — действующий HTTPS-адрес.
Скрипт проверяет структуру, а не достоверность расписания перевозчика.
Инструментальные Android-тесты запускаются командой `./gradlew connectedDebugAndroidTest`
при наличии устройства или эмулятора.

## Обязательные настройки GitHub

Автоматические проверки сами по себе не запрещают прямую запись в ветку. После загрузки всех исправлений в `main` и успешного CI включите Pull Requests,
оставьте squash единственным способом слияния и создайте активный Ruleset
для `refs/heads/main`:

1. включите **Require a pull request before merging**;
2. выберите обязательные проверки `Tests, lint and Detekt`, `Dependency review`, `CodeQL` и `Validate schedule data`;
3. включите **Require branches to be up to date** (strict status checks);
4. заблокируйте force push и удаление ветки;
5. оставьте список bypass пустым, включая администратора.

Обязательные одобрения и Code Owner review требуют второго сопровождающего,
способного проверить PR владельца. Для единственного сопровождающего допустим
минимум 0 одобрений. При появлении второго сопровождающего согласуйте более
строгий review отдельно.

Проверьте на реальном PR наличие всех четырёх статусов. Dependency Review
работает для каждого PR; его пропуск на push ожидаем и не подтверждает
прохождение проверки зависимостей.


В разделе **Settings → Code security and analysis** включите Secret scanning и Push protection. Они не дадут отправить в GitHub распознанные токены и ключи.

Перед слиянием проверяйте изменения в `.github/`, `gradle/`, `build.gradle.kts`, `app/build.gradle.kts` и сетевом коде. Не добавляйте секреты, ключи подписи, токены или локальные файлы в Git.

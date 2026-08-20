# Contributing

[Русский](CONTRIBUTING.md) | **English**

Thank you for your interest in “Let's Go! Slavgorod”. Bug fixes, UI improvements, tests, documentation, and suggestions related to data quality are welcome.

## Before you start

1. Check whether a similar issue or pull request already exists.
2. For a significant change, describe the problem or expected behavior in an issue first.
3. Do not publish secrets, signing keys, personal data, or private application logs.
4. Report potential vulnerabilities according to [SECURITY_EN.md](SECURITY_EN.md), not through a public issue.

## Local development

Clone the repository and build the debug version using the Gradle Wrapper:

```bash
git clone https://github.com/VseMirka200/lets-go-slavgorod.git
cd lets-go-slavgorod
./gradlew assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

The project uses the Android Gradle Plugin and the Gradle Wrapper included in the repository. Use a JDK compatible with the current Android Gradle Plugin and Gradle versions, and install an Android SDK that supports `compileSdk 36`.

## Checks

Before opening a pull request, run the following when possible:

```bash
./gradlew test
./gradlew lint
./gradlew detekt
```

If the change affects the UI, navigation, or Android APIs, also test the application on an emulator or physical device.

## Change style

- follow the existing package structure and naming conventions;
- keep changes small and focused;
- avoid mixing refactoring with functional changes unless necessary;
- add or update tests when testable behavior changes;
- update the README and other documentation when user-facing behavior, requirements, or public settings change;
- do not reformat unrelated files solely for style changes.

## Pull requests

In the pull request description, explain:

- what changed;
- why the change is needed;
- how it was tested;
- whether it changes the UI, schedule handling, permissions, or network behavior;
- which related issues it closes.

For visual changes, before-and-after screenshots are useful.

## Schedule data

Schedules are user-relevant data. Changes to the format, source, validation rules, or time display should preserve understandable behavior when network errors or invalid data occur. Do not add private URLs, tokens, or data sources that cannot be safely published.

## Release builds

Release signing credentials must not be stored in the repository. Local release builds use the `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` environment variables or supported `android.injected.signing.*` parameters.

## Documentation

The public website is stored in the `docs/` directory. If a change affects application behavior, privacy, security, or user instructions, check whether the corresponding website pages should be updated at the same time.

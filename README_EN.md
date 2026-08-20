# Let's Go! Slavgorod

[Русский](README.md) | **English**

![Project banner](docs/assets/images/banner_1.png)

<p align="center">
  <a href="https://vsemirka200.github.io/lets-go-slavgorod/"><img alt="Project website" src="https://img.shields.io/badge/Website-open-2ea44f?style=for-the-badge"></a>
  <a href="https://vsemirka200.github.io/lets-go-slavgorod/schedule.html"><img alt="Schedule" src="https://img.shields.io/badge/Schedule-open-0969da?style=for-the-badge"></a>
  <a href="https://github.com/VseMirka200/lets-go-slavgorod/releases"><img alt="Releases" src="https://img.shields.io/badge/Releases-GitHub-8250df?style=for-the-badge"></a>
  <a href="https://github.com/VseMirka200/lets-go-slavgorod/issues"><img alt="Issues" src="https://img.shields.io/badge/Issues-report_a_problem-d1242f?style=for-the-badge"></a>
</p>

**Let's Go! Slavgorod** is an unofficial Android app with urban and suburban public transport schedules for Slavgorod. It helps users quickly find routes and stops, check upcoming departures, and save frequently used routes as favorites.

> [!IMPORTANT]
> This is an independent project and is not affiliated with the city administration, government institutions, LLC “Slavgorodskoye ATP”, MUP “Torgovy Ryad g. Slavgorod”, or other municipal organizations. The app is not an official source of transport information.

## Features

- urban and suburban routes in one app;
- search by route number, name, description, stops, and departure time;
- a dedicated schedule screen with filters and upcoming departures;
- favorite routes for quick access;
- local storage of settings and schedule cache;
- selectable schedule data source;
- notifications about schedule changes;
- light, dark, and system themes;
- display and interface behavior settings;
- user-initiated export of application logs.

## Screenshots

<p>
  <img width="200" src="docs/assets/screenshots/screenshot_1.png" alt="App home screen">
  <img width="200" src="docs/assets/screenshots/screenshot_2.png" alt="Route list">
  <img width="200" src="docs/assets/screenshots/screenshot_3.png" alt="Schedule screen">
  <img width="200" src="docs/assets/screenshots/screenshot_4.png" alt="Settings">
</p>

## Installation

Ready-to-use builds are published on the project release pages:

- [Releases on the project website](https://vsemirka200.github.io/lets-go-slavgorod/releases.html)
- [GitHub Releases](https://github.com/VseMirka200/lets-go-slavgorod/releases)

The minimum supported Android version is **Android 7.0 (API 24)**. Internet access is required to load the latest schedule. Notification permission is used only for the notification feature and can be disabled by the user.

## Building from source

The project uses the Gradle Wrapper, so a separate Gradle installation is not required.

```bash
git clone https://github.com/VseMirka200/lets-go-slavgorod.git
cd lets-go-slavgorod
./gradlew assembleDebug
```

On Windows:

```powershell
gradlew.bat assembleDebug
```

After a successful build, the debug APK can be found in `app/build/outputs/apk/debug/`.

Useful checks before submitting changes:

```bash
./gradlew test
./gradlew lint
./gradlew detekt
```

A release build requires signing configuration through `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`, or through supported Android Gradle Plugin `android.injected.signing.*` parameters.

## Technology stack

- Kotlin and Jetpack Compose;
- Material 3 and Navigation Compose;
- Koin for dependency injection;
- OkHttp and Gson for networking and data processing;
- DataStore Preferences for local settings;
- Kotlin Coroutines;
- JUnit, Robolectric, Mockito, MockWebServer, and AndroidX Test;
- Detekt and Android Lint for static checks.

The current application version in source is **3.0.2**. The project is built with `compileSdk 36`, `targetSdk 36`, and `minSdk 24`.

## Repository structure

```text
app/                    Android application, resources, and tests
config/detekt/          static analysis configuration
docs/                   project website and public documentation
gradle/                 Gradle Wrapper and version catalog
README.md                Russian project overview
README_EN.md             English project overview
CONTRIBUTING.md          Russian contribution guide
CONTRIBUTING_EN.md       English contribution guide
CODE_OF_CONDUCT.md       Russian Code of Conduct
CODE_OF_CONDUCT_EN.md    English Code of Conduct
SECURITY.md              Russian security policy
SECURITY_EN.md           English security policy
PRIVACY.md               Russian privacy policy
PRIVACY_EN.md            English privacy policy
CHANGELOG.md             Russian changelog
CHANGELOG_EN.md          English changelog
```

## Data and privacy

The app works without an account. User settings, favorite routes, schedule cache, and local logs are stored on the device. To retrieve schedules, the app connects over the network to the selected data source. See [PRIVACY_EN.md](PRIVACY_EN.md) and the [public privacy policy page](https://vsemirka200.github.io/lets-go-slavgorod/privacy.html) for details.

## Documentation

- [Project website](https://vsemirka200.github.io/lets-go-slavgorod/)
- [Current schedule](https://vsemirka200.github.io/lets-go-slavgorod/schedule.html)
- [Releases](https://vsemirka200.github.io/lets-go-slavgorod/releases.html)
- [Privacy policy](PRIVACY_EN.md)
- [Security policy](SECURITY_EN.md)
- [Code of Conduct](CODE_OF_CONDUCT_EN.md)
- [Third-party libraries](https://vsemirka200.github.io/lets-go-slavgorod/libraries.html)
- [Contributing](CONTRIBUTING_EN.md)
- [Changelog](CHANGELOG_EN.md)

The project website is currently primarily in Russian; the repository documentation is available in both Russian and English.

## Contributing

Bug reports, suggestions, and pull requests are welcome. Before making changes, please read [CONTRIBUTING_EN.md](CONTRIBUTING_EN.md) and [CODE_OF_CONDUCT_EN.md](CODE_OF_CONDUCT_EN.md). Potential security vulnerabilities should be reported according to [SECURITY_EN.md](SECURITY_EN.md), not through a public issue.

## Feedback

You can report a bug or suggest an improvement through [GitHub Issues](https://github.com/VseMirka200/lets-go-slavgorod/issues) or through the contact section on the [project website](https://vsemirka200.github.io/lets-go-slavgorod/#feedback).

## License

Terms for using the source code are provided in the existing [LICENSE](LICENSE) file.

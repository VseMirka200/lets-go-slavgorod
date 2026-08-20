# Security Policy

[Русский](SECURITY.md) | **English**

The security of the application, project website, and user data is treated as part of the project's overall quality.

## Reporting a vulnerability

Do not publish details about a potential vulnerability in a public issue, pull request, discussion, or comment.

Send a description to **vladislav-olyushin@mail.ru**. When possible, include:

- the affected application version or website component;
- reproduction conditions and steps;
- expected and actual behavior;
- an assessment of the possible impact;
- a minimal example or proof of the issue without exposing sensitive data.

Do not send real passwords, signing keys, tokens, users' personal data, or other secrets.

## Scope

This policy covers:

- the “Let's Go! Slavgorod” Android application;
- the source code in this repository;
- the public website in the `docs/` directory;
- handling of schedule sources, network requests, local settings, cache, and application logs.

Third-party services and external data sources have their own security policies. A problem in an external service should also be reported to its owner when doing so is safe and appropriate.

## Baseline project measures

The application uses network permissions only for features that require network access and a separate permission for system notifications. Standard cleartext traffic is disabled in the manifest, and local files exported by the user are shared through `FileProvider` with limited URI permissions.

Release signing secrets must not be stored in the repository. They are supplied through environment variables or build-system parameters.

## Public policy page

An extended user-facing security policy page is available here:

https://vsemirka200.github.io/lets-go-slavgorod/security.html

If there is a discrepancy, the responsible vulnerability reporting instructions in this file should be treated as the current repository entry point.

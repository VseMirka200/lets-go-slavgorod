#!/usr/bin/env python3
"""Guard against reintroducing known-vulnerable Gradle build-tool dependencies.

Input: the output of :app:dependencies --configuration releaseRuntimeClasspath
and :buildEnvironment :app:buildEnvironment. Only resolved versions count:
for "requested -> selected", check the selected version, not the old request.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MODULE = re.compile(
    r"(?P<group>[A-Za-z0-9_.-]+):(?P<name>[A-Za-z0-9_.-]+):"
    r"(?P<requested>[0-9][A-Za-z0-9.+_-]*)"
    r"(?:\s+->\s+(?P<selected>[0-9][A-Za-z0-9.+_-]*))?"
)

FLOORS = {
    ("com.google.guava", "guava"): (33, 8, 0),
    ("org.apache.commons", "commons-lang3"): (3, 18, 0),
    ("org.bitbucket.b_c", "jose4j"): (0, 9, 6),
    ("org.jdom", "jdom2"): (2, 0, 6, 1),
    ("org.apache.httpcomponents", "httpclient"): (4, 5, 14),
    ("org.jetbrains.kotlin", "kotlin-gradle-plugin"): (2, 4, 21),
}

def numeric_version(value: str) -> tuple[int, ...]:
    """Compare published numeric release versions, ignoring -jre qualifiers."""
    prefix = value.split("-", 1)[0]
    return tuple(int(part) for part in prefix.split(".") if part.isdigit())

def extract_modules(report: str) -> set[tuple[str, str, str]]:
    resolved = set()
    for line in report.splitlines():
        if line.strip().endswith("(c)"):
            continue  # a Gradle BOM constraint is not a resolved artifact
        for match in MODULE.finditer(line):
            resolved.add((
                match.group("group"),
                match.group("name"),
                match.group("selected") or match.group("requested"),
            ))
    return resolved

def is_watched(group: str, name: str) -> bool:
    return (group, name) in FLOORS or (
        group == "org.bouncycastle" and name.endswith("-jdk18on")
    )

def audit(release: str, build: str, compiler: str = "") -> list[str]:
    release_modules = extract_modules(release)
    build_modules = extract_modules(build)
    compiler_modules = extract_modules(compiler)
    problems = []
    for group, name, version in sorted(release_modules):
        if is_watched(group, name):
            problems.append(
                f"Unexpected build-tool module in release runtime: {group}:{name}:{version}"
            )
    for group, name, version in sorted(build_modules):
        if not is_watched(group, name):
            continue
        minimum = (1, 85) if group == "org.bouncycastle" else FLOORS[(group, name)]
        selected = numeric_version(version)
        if not selected or selected < minimum:
            problems.append(
                f"Vulnerable or unrecognized Gradle plugin dependency: {group}:{name}:{version}"
            )
    for group, name, version in sorted(compiler_modules):
        if not is_watched(group, name):
            continue
        minimum = (1, 85) if group == "org.bouncycastle" else FLOORS[(group, name)]
        selected = numeric_version(version)
        if not selected or selected < minimum:
            problems.append(
                f"Vulnerable Kotlin compiler plugin dependency: {group}:{name}:{version}"
            )
    # A Gradle plugin audit that cannot see the Kotlin plugin is incomplete.
    if not any(
        group == "org.jetbrains.kotlin" and name == "kotlin-gradle-plugin"
        for group, name, _ in build_modules
    ):
        problems.append("Kotlin Gradle plugin not found in buildEnvironment report")
    return problems

def main() -> int:
    if len(sys.argv) not in (3, 4):
        print("Usage: check_gradle_security_baseline.py RELEASE_REPORT BUILD_REPORT [COMPILER_REPORT]", file=sys.stderr)
        return 2
    problems = audit(
        Path(sys.argv[1]).read_text(encoding="utf-8"),
        Path(sys.argv[2]).read_text(encoding="utf-8"),
        Path(sys.argv[3]).read_text(encoding="utf-8") if len(sys.argv) == 4 else "",
    )
    if problems:
        for problem in problems:
            print("ERROR: " + problem, file=sys.stderr)
        return 1
    print("OK: no flagged modules in release runtime; Gradle plugin and compiler versions meet security floors")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

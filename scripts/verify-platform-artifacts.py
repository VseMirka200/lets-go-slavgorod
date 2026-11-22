"""Проверяет SHA-256 AAPT2 для Windows и macOS по метаданным Gradle.

Скачивает артефакты из Google Maven и завершает работу с ошибкой, если запись
или подходящая контрольная сумма отсутствует либо загрузка не удалась.
"""
import hashlib
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

metadata = Path(__file__).resolve().parents[1] / "gradle/verification-metadata.xml"
root = ET.parse(metadata).getroot()
ns = {"g": "https://schema.gradle.org/dependency-verification"}
components = [
    item for item in root.findall(".//g:component", ns)
    if item.get("group") == "com.android.tools.build" and item.get("name") == "aapt2"
]
if not components:
    raise SystemExit("AAPT2 component is missing from verification metadata")
for component in components:
    version = component.attrib["version"]
    for platform in ("windows", "osx"):
        name = f"aapt2-{version}-{platform}.jar"
        artifact = next(
            (item for item in component.findall("g:artifact", ns) if item.get("name") == name),
            None,
        )
        if artifact is None:
            raise SystemExit(f"Missing platform artifact: {name}")
        expected = {item.attrib["value"] for item in artifact.findall("g:sha256", ns)}
        url = f"https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/{version}/{name}"
        with urllib.request.urlopen(url, timeout=60) as response:
            actual = hashlib.file_digest(response, "sha256").hexdigest()
        if actual not in expected:
            raise SystemExit(f"Checksum mismatch: {name}")
        print(f"Verified {name}")

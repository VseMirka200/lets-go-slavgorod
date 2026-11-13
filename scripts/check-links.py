"""Проверяет ссылки Markdown и HTML; внешние адреса проверяются с ключом --external.

Программа сообщает о недоступных ресурсах, но HTTP 403 и сетевые ошибки требуют
ручной проверки: сайт может запрещать автоматические запросы или требовать вход.
"""

import argparse
import concurrent.futures
import html.parser
import pathlib
import re
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
REPOSITORY_BLOB_PREFIX = "/VseMirka200/lets-go-slavgorod/blob/main/"


class HtmlLinks(html.parser.HTMLParser):
    """Собирает адреса ресурсов и идентификаторы якорей HTML-документа."""

    def __init__(self):
        super().__init__()
        self.links = []
        self.ids = set()

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        self.ids.update(value for key, value in attrs.items() if key in ("id", "name") and value)
        for key in ("href", "src"):
            if attrs.get(key):
                self.links.append(attrs[key])


def extract(path):
    """Возвращает ссылки и известные HTML-якоря для одного файла."""
    content = path.read_text(encoding="utf-8")
    if path.suffix.lower() == ".kt":
        # Проверяем только постоянные адреса, а не ссылки с Kotlin-переменными.
        links = re.findall(r'https?://[^\s"<>]+', content)
        return [link for link in links if "$" not in link], set()
    if path.suffix.lower() == ".css":
        return re.findall(r"url\(\s*['\"]?([^)'\"]+)", content), set()
    if path.suffix.lower() == ".js":
        return re.findall(r'https?://[^\s"\'<>]+', content), set()
    parser = HtmlLinks()
    parser.feed(content)
    if path.suffix.lower() == ".md":
        # Блоки кода содержат примеры команд, а не ссылки самого документа.
        content = re.sub(r"(?ms)^\s*```.*?^\s*```", "", content)
        parser.links.extend(re.findall(r"\]\((?:<([^>]+)>|([^\s)]+))", content))
        parser.links = [next((value for value in item if value), "") if isinstance(item, tuple) else item for item in parser.links]
        counts = {}
        for heading in re.findall(r"(?m)^#{1,6}\s+(.+?)\s*#*\s*$", content):
            heading = re.sub(r"\[([^]]+)\]\([^)]+\)", r"\1", heading).lower()
            slug = "".join(char for char in heading if char in " -_" or unicodedata.category(char)[0] in "LN").replace(" ", "-")
            count = counts.get(slug, 0)
            counts[slug] = count + 1
            parser.ids.add(f"{slug}-{count}" if count else slug)
    return parser.links, parser.ids


def check_external(url):
    """Запрашивает адрес с переходом по перенаправлениям без отправки данных."""
    parts = urllib.parse.urlsplit(url)
    encoded_url = urllib.parse.urlunsplit((parts.scheme, parts.netloc, urllib.parse.quote(parts.path, safe="/%:@"), urllib.parse.quote(parts.query, safe="=&%:/?+"), ""))
    request = urllib.request.Request(encoded_url, headers={"User-Agent": "Mozilla/5.0 (compatible; project-link-check/1.0)"})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return response.status, response.geturl()
    except urllib.error.HTTPError as error:
        return error.code, error.geturl()
    except (OSError, ValueError) as error:
        return "ошибка", str(error)


def main():
    """Проверяет файлы проекта и печатает неисправные или непроверенные ссылки."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--external", action="store_true", help="также проверить внешние HTTP-адреса")
    args = parser.parse_args()
    paths = [ROOT / "README.md", *ROOT.joinpath("docs").rglob("*.md"), *ROOT.joinpath("docs").rglob("*.html"), *ROOT.joinpath("docs").rglob("*.css"), *ROOT.joinpath("docs").rglob("*.js"), *ROOT.joinpath("app/src/main/java").rglob("*.kt")]
    parsed = {path: extract(path) for path in paths}
    external = set()
    failures = []
    local_count = 0
    for path, (links, _) in parsed.items():
        for link in links:
            parsed_url = urllib.parse.urlsplit(link)
            # Ссылки на файлы текущей ветки GitHub проверяем по рабочему дереву:
            # это позволяет заметить устаревшие пути до публикации изменений.
            repository_file = (
                parsed_url.netloc.lower() == "github.com"
                and parsed_url.path.startswith(REPOSITORY_BLOB_PREFIX)
            )
            if repository_file:
                local_count += 1
                target = (ROOT / urllib.parse.unquote(parsed_url.path[len(REPOSITORY_BLOB_PREFIX):])).resolve()
                if not target.is_relative_to(ROOT) or not target.is_file():
                    failures.append(f"{path.relative_to(ROOT)}: отсутствует {link}")
                elif parsed_url.fragment and target.suffix.lower() in (".html", ".md"):
                    ids = parsed.get(target, (None, None))[1]
                    if ids is None:
                        ids = extract(target)[1]
                    if urllib.parse.unquote(parsed_url.fragment) not in ids:
                        failures.append(f"{path.relative_to(ROOT)}: не найден якорь {link}")
            elif parsed_url.scheme in ("http", "https"):
                external.add(urllib.parse.urldefrag(link).url)
            elif not parsed_url.scheme and not link.startswith("//"):
                local_count += 1
                target = (path.parent / urllib.parse.unquote(parsed_url.path)).resolve() if parsed_url.path else path
                # GitHub отображает ссылки на каталоги из Markdown как списки файлов.
                # Веб-серверу же для ссылки на каталог нужна индексная страница.
                if target.is_dir() and path.suffix.lower() != ".md":
                    target = target / "index.html"
                if not target.exists():
                    failures.append(f"{path.relative_to(ROOT)}: отсутствует {link}")
                elif parsed_url.fragment and target.suffix.lower() in (".html", ".md"):
                    ids = parsed.get(target, (None, None))[1]
                    if ids is None:
                        ids = extract(target)[1]
                    if urllib.parse.unquote(parsed_url.fragment) not in ids:
                        failures.append(f"{path.relative_to(ROOT)}: не найден якорь {link}")
    print(f"Локальных ссылок: {local_count}; уникальных внешних адресов: {len(external)}")
    for failure in failures:
        print(failure)
    if args.external:
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
            for url, (status, result) in zip(sorted(external), executor.map(check_external, sorted(external))):
                print(f"{status} {url}" + (f" -> {result}" if result != url else ""))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())

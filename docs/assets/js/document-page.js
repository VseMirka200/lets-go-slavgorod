const documents = {
    privacy: { title: "Политика конфиденциальности", lead: "Какие данные обрабатывает приложение, зачем они нужны и как ими управлять.", file: "PRIVACY.md" },
    security: { title: "Политика безопасности", lead: "Как сообщать об уязвимостях и какие базовые меры безопасности применяются в проекте.", file: "SECURITY.md" },
    conduct: { title: "Кодекс поведения", lead: "Правила уважительного общения и совместной работы в проекте «Поехали! Славгород».", file: "CODE_OF_CONDUCT.md" },
    libraries: { title: "Используемые библиотеки", lead: "Основные библиотеки и инструменты, используемые приложением и системой сборки.", file: "LIBRARIES.md" },
    contributing: { title: "Участие в разработке", lead: "Как предложить изменение, подготовить pull request и проверить его перед отправкой.", file: "CONTRIBUTING.md" },
    changelog: { title: "История изменений", lead: "Заметные изменения проекта и правила ведения журнала версий.", file: "CHANGELOG.md" }
};

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function inlineMarkdown(value) {
    let text = escapeHtml(value);
    const placeholders = [];
    const stash = (html) => `@@DOC_${placeholders.push(html) - 1}@@`;

    text = text.replace(/`([^`]+)`/g, (_, code) => stash(`<code>${code}</code>`));
    text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, href) => {
        const safeHref = escapeHtml(href.trim());
        const external = /^https?:\/\//i.test(href) ? ' target="_blank" rel="noreferrer"' : "";
        return stash(`<a class="text-link" href="${safeHref}"${external}>${label}</a>`);
    });
    text = text.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
    text = text.replace(/(^|\s)https?:\/\/[^\s<]+/g, (url) => {
        const leading = url.startsWith(" ") ? " " : "";
        const raw = url.trim();
        return leading + stash(`<a class="text-link" href="${raw}" target="_blank" rel="noreferrer">${raw}</a>`);
    });
    text = text.replace(/@@DOC_(\d+)@@/g, (_, index) => placeholders[Number(index)] || "");
    return text;
}

function markdownToHtml(markdown) {
    const lines = markdown.replaceAll("\r\n", "\n").split("\n");
    const html = [];
    let paragraph = [];
    let listType = null;
    let inCode = false;
    let codeLines = [];
    let skippedTitle = false;

    const flushParagraph = () => {
        if (!paragraph.length) return;
        html.push(`<p>${inlineMarkdown(paragraph.join(" "))}</p>`);
        paragraph = [];
    };

    const closeList = () => {
        if (!listType) return;
        html.push(`</${listType}>`);
        listType = null;
    };

    const openList = (type) => {
        if (listType === type) return;
        closeList();
        html.push(`<${type}>`);
        listType = type;
    };

    const flushCode = () => {
        html.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
        codeLines = [];
    };

    for (const rawLine of lines) {
        const line = rawLine.trim();

        if (line.startsWith("```")) {
            if (inCode) {
                inCode = false;
                flushCode();
            } else {
                flushParagraph();
                closeList();
                inCode = true;
            }
            continue;
        }

        if (inCode) {
            codeLines.push(rawLine);
            continue;
        }

        if (!skippedTitle && line.startsWith("# ")) {
            skippedTitle = true;
            continue;
        }

        if (!line) {
            flushParagraph();
            closeList();
            continue;
        }

        if (line.startsWith("### ")) {
            flushParagraph();
            closeList();
            html.push(`<h3>${inlineMarkdown(line.slice(4))}</h3>`);
            continue;
        }

        if (line.startsWith("## ")) {
            flushParagraph();
            closeList();
            html.push(`<h2>${inlineMarkdown(line.slice(3))}</h2>`);
            continue;
        }

        if (line.startsWith("> ")) {
            flushParagraph();
            closeList();
            html.push(`<blockquote>${inlineMarkdown(line.slice(2))}</blockquote>`);
            continue;
        }

        if (line.startsWith("- ")) {
            flushParagraph();
            openList("ul");
            html.push(`<li>${inlineMarkdown(line.slice(2))}</li>`);
            continue;
        }

        const ordered = line.match(/^\d+\.\s+(.+)$/);
        if (ordered) {
            flushParagraph();
            openList("ol");
            html.push(`<li>${inlineMarkdown(ordered[1])}</li>`);
            continue;
        }

        paragraph.push(line);
    }

    if (inCode) flushCode();
    flushParagraph();
    closeList();
    return html.join("\n");
}

async function initDocumentPage() {
    const params = new URLSearchParams(window.location.search);
    const key = documents[params.get("doc")] ? params.get("doc") : "privacy";
    const config = documents[key];

    document.documentElement.lang = "ru";
    document.title = `${config.title} - Поехали! Славгород`;
    document.getElementById("document-title").textContent = config.title;
    document.getElementById("document-lead").textContent = config.lead;

    const content = document.getElementById("document-content");
    content.innerHTML = '<p class="document-state">Загрузка документа…</p>';

    try {
        const response = await fetch(config.file, { cache: "no-store" });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        content.innerHTML = markdownToHtml(await response.text());
    } catch (_) {
        content.innerHTML = '<p class="document-state">Не удалось загрузить документ.</p>';
    }
}

initDocumentPage();

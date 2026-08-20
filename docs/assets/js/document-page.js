const documents = {
    privacy: {
        ru: { title: "Политика конфиденциальности", lead: "Какие данные обрабатывает приложение, зачем они нужны и как ими управлять.", file: "PRIVACY.md" },
        en: { title: "Privacy Policy", lead: "What data the app processes, why it is needed, and how users can control it.", file: "PRIVACY_EN.md" }
    },
    security: {
        ru: { title: "Политика безопасности", lead: "Как сообщать об уязвимостях и какие базовые меры безопасности применяются в проекте.", file: "SECURITY.md" },
        en: { title: "Security Policy", lead: "How to report vulnerabilities and which baseline security measures the project uses.", file: "SECURITY_EN.md" }
    },
    conduct: {
        ru: { title: "Кодекс поведения", lead: "Правила уважительного общения и совместной работы в проекте «Поехали! Славгород».", file: "CODE_OF_CONDUCT.md" },
        en: { title: "Code of Conduct", lead: "Rules for respectful communication and collaboration in the “Let's Go! Slavgorod” project.", file: "CODE_OF_CONDUCT_EN.md" }
    },
    libraries: {
        ru: { title: "Используемые библиотеки", lead: "Основные библиотеки и инструменты, используемые приложением и системой сборки.", file: "LIBRARIES.md" },
        en: { title: "Third-party libraries", lead: "Main libraries and tools used by the application and build system.", file: "LIBRARIES_EN.md" }
    },
    contributing: {
        ru: { title: "Участие в разработке", lead: "Как предложить изменение, подготовить pull request и проверить его перед отправкой.", file: "CONTRIBUTING.md" },
        en: { title: "Contributing", lead: "How to propose a change, prepare a pull request, and verify it before submission.", file: "CONTRIBUTING_EN.md" }
    },
    changelog: {
        ru: { title: "История изменений", lead: "Заметные изменения проекта и правила ведения журнала версий.", file: "CHANGELOG.md" },
        en: { title: "Changelog", lead: "Notable project changes and rules for maintaining the version history.", file: "CHANGELOG_EN.md" }
    }
};

const ui = {
    ru: {
        eyebrow: "Документы",
        loading: "Загрузка документа…",
        error: "Не удалось загрузить документ.",
        nav: ["Возможности", "Документы", "Контакты", "Релизы", "Поддержать", "Расписание"],
        footer: ["Документы", "Расписание", "Релизы", "Контакты", "Исходный код", "Группа ВКонтакте", "Поддержать"],
        back: "Вернуться наверх"
    },
    en: {
        eyebrow: "Documents",
        loading: "Loading document…",
        error: "The document could not be loaded.",
        nav: ["Features", "Documents", "Contact", "Releases", "Support", "Schedule"],
        footer: ["Documents", "Schedule", "Releases", "Contact", "Source code", "VK community", "Support"],
        back: "Back to top"
    }
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

        if (/^(\*\*Русский\*\* \| \[English\]|\[Русский\].*\| \*\*English\*\*)/.test(line)) {
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

function setText(id, value) {
    const node = document.getElementById(id);
    if (node) node.textContent = value;
}

async function initDocumentPage() {
    const params = new URLSearchParams(window.location.search);
    const key = documents[params.get("doc")] ? params.get("doc") : "privacy";
    const lang = params.get("lang") === "en" ? "en" : "ru";
    const config = documents[key][lang];
    const strings = ui[lang];

    document.documentElement.lang = lang;
    document.title = `${config.title} - ${lang === "ru" ? "Поехали! Славгород" : "Let's Go! Slavgorod"}`;

    setText("document-eyebrow", strings.eyebrow);
    setText("document-title", config.title);
    setText("document-lead", config.lead);

    const navIds = ["nav-features", "nav-documents", "nav-feedback", "nav-releases", "nav-support", "nav-schedule"];
    navIds.forEach((id, index) => setText(id, strings.nav[index]));
    const footerIds = ["footer-documents", "footer-schedule", "footer-releases", "footer-feedback", "footer-source", "footer-vk", "footer-support"];
    footerIds.forEach((id, index) => setText(id, strings.footer[index]));
    document.getElementById("back-to-top")?.setAttribute("aria-label", strings.back);

    const ruLink = document.getElementById("lang-ru");
    const enLink = document.getElementById("lang-en");
    ruLink.href = `document.html?doc=${encodeURIComponent(key)}&lang=ru`;
    enLink.href = `document.html?doc=${encodeURIComponent(key)}&lang=en`;
    (lang === "ru" ? ruLink : enLink).setAttribute("aria-current", "page");

    const content = document.getElementById("document-content");
    content.innerHTML = `<p class="document-state">${strings.loading}</p>`;

    try {
        const response = await fetch(config.file, { cache: "no-store" });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        content.innerHTML = markdownToHtml(await response.text());
    } catch (_) {
        content.innerHTML = `<p class="document-state">${strings.error}</p>`;
    }
}

initDocumentPage();

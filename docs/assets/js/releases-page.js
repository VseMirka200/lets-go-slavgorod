const GITHUB_REPO = "VseMirka200/lets-go-slavgorod";
const GITHUB_RELEASES_API = `https://api.github.com/repos/${GITHUB_REPO}/releases?per_page=30`;

function escapeHtml(input) {
    return String(input)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function applyInlineMarkdown(text) {
    let value = escapeHtml(text);
    const links = [];

    value = value.replace(/`([^`]+)`/g, "<code>$1</code>");
    value = value.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, (_, label, url) => {
        const index = links.push(`<a class="text-link" href="${url}" target="_blank" rel="noreferrer">${label}</a>`) - 1;
        return `@@LINK_${index}@@`;
    });
    value = value.replace(/(https?:\/\/[^\s<]+)/g, '<a class="text-link" href="$1" target="_blank" rel="noreferrer">$1</a>');
    value = value.replace(/@@LINK_(\d+)@@/g, (_, index) => links[Number(index)] || "");
    return value;
}

function markdownToHtml(markdown) {
    const lines = String(markdown || "").replaceAll("\r\n", "\n").split("\n");
    const html = [];
    let inList = false;
    let paragraph = [];

    const flushParagraph = () => {
        if (!paragraph.length) return;
        html.push(`<p>${applyInlineMarkdown(paragraph.join(" "))}</p>`);
        paragraph = [];
    };

    const closeList = () => {
        if (!inList) return;
        html.push("</ul>");
        inList = false;
    };

    for (const rawLine of lines) {
        const line = rawLine.trim();

        if (!line) {
            flushParagraph();
            closeList();
            continue;
        }

        if (line.startsWith("### ")) {
            flushParagraph();
            closeList();
            html.push(`<h3>${applyInlineMarkdown(line.slice(4))}</h3>`);
            continue;
        }

        if (line.startsWith("## ")) {
            flushParagraph();
            closeList();
            html.push(`<h2>${applyInlineMarkdown(line.slice(3))}</h2>`);
            continue;
        }

        if (line.startsWith("# ")) {
            flushParagraph();
            closeList();
            html.push(`<h1>${applyInlineMarkdown(line.slice(2))}</h1>`);
            continue;
        }

        if (line.startsWith("- ")) {
            flushParagraph();
            if (!inList) {
                html.push("<ul>");
                inList = true;
            }
            html.push(`<li>${applyInlineMarkdown(line.slice(2))}</li>`);
            continue;
        }

        paragraph.push(line);
    }

    flushParagraph();
    closeList();
    return html.join("\n");
}

function formatReleaseDate(value) {
    if (!value) {
        return "Дата не указана";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "Дата не указана";
    }

    return new Intl.DateTimeFormat("ru-RU", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(date);
}

function formatReleaseBody(body) {
    const text = String(body ?? "").trim();
    if (!text) {
        return "";
    }

    return markdownToHtml(text);
}

function renderRelease(release) {
    const name = escapeHtml(release.name || release.tag_name || "Без названия");
    const tagName = escapeHtml(release.tag_name || "");
    const publishedAt = escapeHtml(formatReleaseDate(release.published_at));
    const isPrerelease = Boolean(release.prerelease);
    const isDraft = Boolean(release.draft);
    const body = formatReleaseBody(release.body);
    const assets = Array.isArray(release.assets) ? release.assets : [];

    const assetsMarkup = assets.length
        ? assets.map((asset) => {
            const assetName = escapeHtml(asset.name || "Файл");
            const assetUrl = escapeHtml(asset.browser_download_url || "#");
            const sizeMb = asset.size ? `${(asset.size / (1024 * 1024)).toFixed(1)} МБ` : "";

            return `
                <a class="release-asset-link" href="${assetUrl}" target="_blank" rel="noreferrer">
                    <span class="release-asset-name">${assetName}</span>
                    <span class="release-asset-meta">${sizeMb}</span>
                </a>
            `;
        }).join("")
        : `<div class="release-empty-assets">Файлы для этого релиза не приложены.</div>`;

    return `
        <article class="release-card">
            <header class="release-card-header">
                <div class="release-card-title-row">
                    <h2>${name}</h2>
                    <div class="release-badges">
                        ${tagName ? `<span class="release-badge">${tagName}</span>` : ""}
                        ${isPrerelease ? `<span class="release-badge release-badge--accent">Pre-release</span>` : ""}
                        ${isDraft ? `<span class="release-badge release-badge--muted">Draft</span>` : ""}
                    </div>
                </div>
                <div class="release-meta">
                    <span>Опубликован: ${publishedAt}</span>
                    <span><a href="${escapeHtml(release.html_url || "#")}" target="_blank" rel="noreferrer">Открыть на GitHub</a></span>
                </div>
            </header>
            ${body ? `<div class="release-body release-markdown">${body}</div>` : ""}
            <div class="release-assets">
                <strong>Файлы релиза</strong>
                <div class="release-assets-grid">
                    ${assetsMarkup}
                </div>
            </div>
        </article>
    `;
}

async function loadReleases() {
    const loadStateLabel = document.getElementById("releases-load-state");
    const latestTagLabel = document.getElementById("releases-latest-tag");
    const countLabel = document.getElementById("releases-count");
    const statusMessage = document.getElementById("releases-status-message");
    const list = document.getElementById("releases-list");

    if (!loadStateLabel || !latestTagLabel || !countLabel || !statusMessage || !list) {
        return;
    }

    try {
        const response = await fetch(GITHUB_RELEASES_API, {
            headers: {
                Accept: "application/vnd.github+json"
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const releases = await response.json();
        const releaseList = Array.isArray(releases) ? releases : [];

        loadStateLabel.textContent = `Загружено: ${releaseList.length}`;
        countLabel.textContent = String(releaseList.length);
        latestTagLabel.textContent = releaseList[0]?.tag_name || "Нет релизов";
        statusMessage.textContent = releaseList.length ? "" : "Релизы пока не опубликованы.";
        list.innerHTML = releaseList.length
            ? releaseList.map(renderRelease).join("")
            : `
                <div class="release-empty-state">
                    <strong>Релизы не найдены</strong>
                    <p>В репозитории пока нет опубликованных релизов.</p>
                </div>
            `;
    } catch (error) {
        loadStateLabel.textContent = "Ошибка загрузки";
        statusMessage.textContent = "Не удалось получить релизы из GitHub.";
        list.innerHTML = `
            <div class="release-empty-state release-empty-state-error">
                <strong>Источник недоступен</strong>
                <p>Проверьте доступность репозитория или попробуйте позже.</p>
                <p class="release-error-detail">${escapeHtml(error.message || "unknown error")}</p>
            </div>
        `;
    }
}

loadReleases();

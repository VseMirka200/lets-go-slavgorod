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
    const lines = markdown.replaceAll("\r\n", "\n").split("\n");
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

async function loadMarkdownDocument() {
    const container = document.getElementById("policy-content");
    if (!container) return;

    const sources = (container.dataset.sources || "")
        .split("|")
        .map((source) => source.trim())
        .filter(Boolean);

    for (const source of sources) {
        try {
            const response = await fetch(source, { cache: "no-store" });
            if (!response.ok) continue;
            const text = await response.text();
            container.innerHTML = markdownToHtml(text);
            return;
        } catch (_) {
            // Try the next configured source.
        }
    }

    container.textContent = container.dataset.error || "Не удалось загрузить документ из репозитория.";
}

loadMarkdownDocument();

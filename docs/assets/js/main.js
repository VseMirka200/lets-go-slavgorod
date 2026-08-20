/**
 * Выравнивает текстовую колонку героя по высоте с визуальной колонкой на широких экранах.
 * Когда макет переходит в одну колонку, ограничение по высоте снимается.
 */
function syncHeroColumnsHeight() {
    const heroCopy = document.querySelector(".hero-copy");
    const heroVisual = document.querySelector(".hero-visual");
    if (!heroCopy || !heroVisual) {
        return;
    }

    const isDesktop = window.matchMedia("(min-width: 981px)").matches;
    if (!isDesktop) {
        heroCopy.style.maxHeight = "";
        heroCopy.classList.remove("hero-copy-scroll");
        return;
    }

    const visualHeight = heroVisual.getBoundingClientRect().height;
    if (visualHeight > 0) {
        heroCopy.style.maxHeight = `${Math.round(visualHeight)}px`;
        heroCopy.classList.add("hero-copy-scroll");
    }
}

/**
 * Пересчитывает высоту колонок героя при изменении ширины окна или баннера.
 * Так мы избегаем лишнего пустого пространства в первом экране на десктопе.
 */
function setupHeroHeightSync() {
    syncHeroColumnsHeight();

    let rafId = 0;
    const scheduleSync = () => {
        if (rafId) {
            cancelAnimationFrame(rafId);
        }
        rafId = requestAnimationFrame(syncHeroColumnsHeight);
    };

    window.addEventListener("resize", scheduleSync);

    const heroVisual = document.querySelector(".hero-visual");
    if (heroVisual && "ResizeObserver" in window) {
        const observer = new ResizeObserver(scheduleSync);
        observer.observe(heroVisual);
    }
}

setupHeroHeightSync();

function setupImagePreviewModal() {
    const modal = document.getElementById("image-modal");
    const modalImage = modal?.querySelector(".image-modal-image");
    const modalCaption = modal?.querySelector(".image-modal-caption");
    const prevButton = modal?.querySelector(".image-modal-nav-prev");
    const nextButton = modal?.querySelector(".image-modal-nav-next");
    const previewLinks = document.querySelectorAll(".hero-stack a");

    if (!modal || !modalImage || !modalCaption || !prevButton || !nextButton || previewLinks.length === 0) {
        return;
    }

    const previewItems = Array.from(previewLinks)
        .map((link) => {
            const image = link.querySelector("img");
            return {
                link,
                imageUrl: link.getAttribute("href") || "",
                altText: image?.alt || "",
            };
        })
        .filter((item) => item.imageUrl);

    let lastFocusedElement = null;
    let currentIndex = 0;

    const closeModal = () => {
        modal.hidden = true;
        modal.setAttribute("aria-hidden", "true");
        modal.classList.remove("is-open");
        modalImage.src = "";
        modalImage.alt = "";
        modalCaption.textContent = "";
        document.body.style.overflow = "";
        if (lastFocusedElement instanceof HTMLElement) {
            lastFocusedElement.focus();
        }
    };

    const renderItem = (index) => {
        if (previewItems.length === 0) {
            return;
        }

        currentIndex = (index + previewItems.length) % previewItems.length;
        const item = previewItems[currentIndex];
        modalImage.src = item.imageUrl;
        modalImage.alt = item.altText || "Изображение";
        modalCaption.textContent = item.altText || "";
    };

    const openModal = (index, trigger) => {
        lastFocusedElement = trigger;
        renderItem(index);
        modal.hidden = false;
        modal.setAttribute("aria-hidden", "false");
        modal.classList.add("is-open");
        document.body.style.overflow = "hidden";
        nextButton.focus();
    };

    previewLinks.forEach((link) => {
        link.addEventListener("click", (event) => {
            const imageUrl = link.getAttribute("href");
            if (!imageUrl) {
                return;
            }

            event.preventDefault();
            const index = previewItems.findIndex((item) => item.link === link);
            openModal(index >= 0 ? index : 0, link);
        });
    });

    prevButton.addEventListener("click", () => {
        renderItem(currentIndex - 1);
    });

    nextButton.addEventListener("click", () => {
        renderItem(currentIndex + 1);
    });

    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (modal.hidden) {
            return;
        }

        if (event.key === "Escape") {
            closeModal();
        } else if (event.key === "ArrowLeft") {
            renderItem(currentIndex - 1);
        } else if (event.key === "ArrowRight") {
            renderItem(currentIndex + 1);
        }
    });
}

setupImagePreviewModal();

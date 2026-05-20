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

const comingSoonPlayButton = document.querySelector("[data-coming-soon-play]");
if (comingSoonPlayButton) {
    comingSoonPlayButton.addEventListener("click", (event) => {
        event.preventDefault();
        window.alert("Скоро и там будем");
    });
}

let deferredInstallPrompt = null;

const isIos = /iphone|ipad|ipod/i.test(window.navigator.userAgent);
const isAndroid = /android/i.test(window.navigator.userAgent);
const isStandalone = window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;

const updateInstallButtons = () => {
    const shouldShow = !isStandalone && (isIos || isAndroid || deferredInstallPrompt !== null);

    document.querySelectorAll("[data-install-web-app]").forEach((button) => {
        button.hidden = !shouldShow;
    });
};

const showInstallInstructions = () => {
    window.alert("Откройте меню браузера ⋮ и выберите «Установить приложение» или «Добавить на главный экран».");
};

window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    deferredInstallPrompt = event;
    updateInstallButtons();
});

document.addEventListener("DOMContentLoaded", () => {
    const navigationBreakpoint = window.matchMedia("(min-width: 781px)");
    const syncNavigationMenu = ({ matches }) => {
        document.querySelectorAll(".portal-menu").forEach((menu) => {
            menu.open = matches;
        });
    };

    syncNavigationMenu(navigationBreakpoint);
    navigationBreakpoint.addEventListener("change", syncNavigationMenu);

    if ("serviceWorker" in navigator) {
        navigator.serviceWorker.register("sw.js");
    }

    document.querySelectorAll("[data-install-web-app]").forEach((button) => {
        button.addEventListener("click", async () => {
            if (deferredInstallPrompt) {
                try {
                    await deferredInstallPrompt.prompt();
                    await deferredInstallPrompt.userChoice;
                } catch (error) {
                    showInstallInstructions();
                } finally {
                    deferredInstallPrompt = null;
                    updateInstallButtons();
                }
                return;
            }

            if (isIos) {
                window.alert("В Safari нажмите «Поделиться», затем выберите «На экран Домой».");
                return;
            }

            showInstallInstructions();
        });
    });

    updateInstallButtons();
});

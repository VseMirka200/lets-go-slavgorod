let deferredInstallPrompt = null;

const isIos = /iphone|ipad|ipod/i.test(window.navigator.userAgent);
const isStandalone = window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;

window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    deferredInstallPrompt = event;
    document.querySelectorAll("[data-install-web-app]").forEach((button) => {
        button.hidden = false;
    });
});

document.addEventListener("DOMContentLoaded", () => {
    if ("serviceWorker" in navigator) {
        navigator.serviceWorker.register("sw.js");
    }

    document.querySelectorAll("[data-install-web-app]").forEach((button) => {
        if (isStandalone || (!isIos && !deferredInstallPrompt)) {
            button.hidden = true;
            return;
        }

        button.hidden = isIos ? false : !deferredInstallPrompt;
        button.addEventListener("click", async () => {
            if (deferredInstallPrompt) {
                deferredInstallPrompt.prompt();
                await deferredInstallPrompt.userChoice;
                deferredInstallPrompt = null;
                button.hidden = true;
                return;
            }

            if (isIos) {
                window.alert("В Safari нажмите «Поделиться», затем выберите «На экран Домой».");
                return;
            }

        });
    });
});

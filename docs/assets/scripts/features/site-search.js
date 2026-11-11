/** Общий поиск по разделам сайта; ссылки остаются доступными и в боковом меню. */
(() => {
    const trigger = document.querySelector('[data-open-search]');
    if (!trigger) return;

    const pages = [
        ['Начало', 'index.html'],
        ['Расписание', 'schedule.html'],
        ['Как пользоваться', 'index.html#how'],
        ['Скачать приложение', 'index.html#download'],
        ['Скачать из RuStore', 'https://www.rustore.ru/catalog/app/ru.slavgorod.transport'],
        ['Скачать из Google Play', 'https://play.google.com/store/apps/details?id=ru.slavgorod.transport'],
        ['Скачать APK (GitHub)', 'https://github.com/VseMirka200/lets-go-slavgorod/releases'],
        ['Обратная связь', 'feedback.html'],
        ['Как обновляется расписание', 'index.html#schedule-data'],
        ['Документация', 'index.html#project'],
        ['Связаться с проектом', 'index.html#contact'],
        ['Поддержать проект', 'support.html'],
        ['GitHub', 'https://github.com/VseMirka200/lets-go-slavgorod']
    ];
    const dialog = document.createElement('dialog');
    dialog.id = 'search-dialog';
    dialog.setAttribute('aria-labelledby', 'site-search-title');
    dialog.innerHTML = '<div class="dialog-head"><strong id="site-search-title">Поиск по сайту</strong><button type="button" aria-label="Закрыть поиск">×</button></div><input id="search-input" type="search" aria-label="Поиск по сайту" placeholder="Расписание, APK, обратная связь"><div id="results" aria-live="polite"></div>';
    document.body.appendChild(dialog);
    const input = dialog.querySelector('#search-input');
    const results = dialog.querySelector('#results');

    /** Обновляет ссылки по подстроке названия без учёта регистра и пробелов по краям. */
    const render = () => {
        const query = input.value.trim().toLocaleLowerCase('ru');
        const matches = pages.filter(([title]) => title.toLocaleLowerCase('ru').includes(query));
        results.replaceChildren();
        if (!matches.length) {
            results.textContent = 'Ничего не найдено.';
            return;
        }
        for (const [title, href] of matches) {
            const link = document.createElement('a');
            link.textContent = title;
            link.href = href;
            results.appendChild(link);
        }
    };
    /** Открывает поиск с пустым запросом и переносит фокус в поле ввода. */
    const open = () => {
        if (dialog.open) return;
        input.value = '';
        render();
        dialog.showModal();
        input.focus();
    };
    trigger.addEventListener('click', open);
    dialog.querySelector('button').addEventListener('click', () => dialog.close());
    input.addEventListener('input', render);
    document.addEventListener('keydown', (event) => {
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
            event.preventDefault();
            open();
        }
    });
})();

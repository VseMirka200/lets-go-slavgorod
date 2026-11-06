import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';

test('на всех страницах доступны общие ссылки и основные разделы', () => {
    for (const name of ['index.html', 'schedule.html', 'feedback.html']) {
        const html = readFileSync(`docs/${name}`, 'utf8');
        assert.ok(html.includes('data-open-search'), `${name}: отсутствует поиск`);
        assert.ok(html.includes('assets/scripts/features/site-search.js'), `${name}: не подключён общий поиск`);
        const topline = html.match(/<div class="portal-topline">([\s\S]*?)<\/div>/)?.[1];
        assert.ok(topline, `${name}: отсутствует верхняя панель`);
        for (const title of ['Группа ВКонтакте', 'Канал в МАКС', 'Релизы GitHub']) {
            assert.ok(topline.includes(title), `${name}: отсутствует ${title}`);
        }
        const navigation = html.match(/<nav class="portal-nav"[^>]*>([\s\S]*?)<\/nav>/)?.[1];
        assert.ok(navigation, `${name}: отсутствует меню`);
        for (const title of ['Начало', 'Расписание', 'Как пользоваться', 'Обратная связь', 'Документация']) {
            assert.ok(navigation.includes(title), `${name}: отсутствует ${title}`);
        }
        assert.ok(!navigation.includes('Канал в МАКС'), `${name}: МАКС не должен быть в меню`);
        const footer = html.match(/<footer\b[^>]*>([\s\S]*?)<\/footer>/)?.[1];
        assert.ok(footer?.includes('© Поехали! Славгород'), `${name}: отсутствует подпись в подвале`);
        assert.ok(footer?.includes('Исходный код'), `${name}: отсутствует ссылка на исходный код`);
    }
});

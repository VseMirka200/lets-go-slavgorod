import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

function createFeedback() {
    const events = {};
    const start = { addEventListener: (_name, fn) => { events.start = fn; }, setAttribute() {} };
    const email = { hidden: true };
    const topicSection = { hidden: true, scrollIntoView() {} };
    const link = { setAttribute(name, value) { this[name] = value; }, removeAttribute(name) { delete this[name]; } };
    const elements = { 'feedback-start': start, 'feedback-email': email, 'feedback-topic': topicSection, 'feedback-email-link': link };
    const selection = { client: 'app', topic: 'bug' };
    const clients = [{ addEventListener: (_name, fn) => { events.client = fn; } }];
    const topics = [{ addEventListener: (_name, fn) => { events.topic = fn; } }];
    const document = {
        getElementById: id => elements[id],
        querySelector: selector => ({ value: selector.includes('feedback-client') ? selection.client : selection.topic }),
        querySelectorAll: selector => selector.includes('feedback-client') ? clients : topics
    };
    const context = { document, encodeURIComponent };
    runInNewContext(readFileSync('docs/assets/scripts/pages/feedback-page.js', 'utf8'), context);
    return { context, events, selection, email, topicSection, link };
}

test('сначала открывается выбор почты, затем тема обращения', () => {
    const { events, email, topicSection, link } = createFeedback();
    events.start();
    assert.equal(email.hidden, false);
    assert.equal(topicSection.hidden, true);
    events.client();
    assert.equal(topicSection.hidden, false);
    assert.equal(new URL(link.href).pathname, 'vsemirka200@bk.ru');
});

test('каждая почта получает адрес, тему и шаблон обращения', () => {
    const { context } = createFeedback();
    for (const [value, title, prompt] of [
        ['bug', 'Ошибка в приложении', 'Как повторить ошибку:'],
        ['schedule', 'Неточность в расписании', 'Маршрут и направление:'],
        ['suggestion', 'Предложение по улучшению', 'Как это поможет:'],
        ['other', 'Другой вопрос', 'Ваше сообщение:']
    ]) {
        for (const client of ['app', 'gmail', 'mailru', 'yandex']) {
            const url = new URL(context.buildFeedbackUrl(client, value));
            const mail = client === 'app' ? url : client === 'gmail' ? null : new URL(url.searchParams.get('mailto'));
            const address = client === 'gmail' ? url.searchParams.get('to') : mail.pathname;
            const subject = client === 'gmail' ? url.searchParams.get('su') : mail.searchParams.get('subject');
            const body = client === 'gmail' ? url.searchParams.get('body') : mail.searchParams.get('body');
            assert.equal(address, 'vsemirka200@bk.ru');
            assert.equal(subject, `Поехали! Славгород — ${title}`);
            assert.ok(body.includes(prompt));
        }
    }
});

test('смена почты и темы обновляет письмо и способ открытия', () => {
    const { events, selection, link } = createFeedback();
    selection.client = 'gmail';
    events.client();
    assert.equal(link.target, '_blank');
    selection.topic = 'schedule';
    events.topic();
    assert.equal(new URL(link.href).searchParams.get('su'), 'Поехали! Славгород — Неточность в расписании');
    selection.client = 'app';
    events.client();
    assert.equal(link.target, undefined);
    assert.equal(new URL(link.href).searchParams.get('subject'), 'Поехали! Славгород — Неточность в расписании');
});

test('неизвестная почта или тема не создаёт ссылку', () => {
    const { context } = createFeedback();
    assert.equal(context.buildFeedbackUrl('unknown', 'bug'), null);
    assert.equal(context.buildFeedbackUrl('gmail', 'unknown'), null);
});

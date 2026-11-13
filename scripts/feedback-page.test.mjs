import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

function createForm() {
    const element = (extra = {}) => ({ events: {}, addEventListener(name, fn) { this.events[name] = fn; }, setAttribute(name, value) { this[name] = value; }, ...extra });
    const subject = element({ value: 'Тема' });
    const message = element({ value: 'Текст' });
    const open = element();
    const email = element({ hidden: true, scrollIntoView() {} });
    const templates = ['bug', 'schedule', 'idea', 'question', 'other'].map(value => element({ dataset: { template: value } }));
    const providers = ['yandex', 'mailru', 'gmail', 'outlook', 'yahoo', 'proton', 'app'].map(value => element({ dataset: { provider: value } }));
    const elements = { '#subject': subject, '#message': message, '#open-email': open, '#email': email };
    const document = { querySelector: selector => elements[selector], querySelectorAll: selector => selector === '[data-provider]' ? providers : templates };
    runInNewContext(readFileSync('docs/assets/scripts/pages/contact.js', 'utf8'), { document, URLSearchParams });
    return { subject, message, open, email, templates, providers };
}

test('форма раскрывается и закрывается с корректным aria-expanded', () => {
    const { open, email } = createForm();
    open.events.click();
    assert.equal(email.hidden, false);
    assert.equal(open['aria-expanded'], 'true');
    open.events.click();
    assert.equal(email.hidden, true);
    assert.equal(open['aria-expanded'], 'false');
});

test('шаблон расписания и изменённый текст передаются во все почтовые ссылки', () => {
    const { subject, message, templates, providers } = createForm();
    templates.find(item => item.dataset.template === 'schedule').events.click();
    assert.match(subject.value, /неточность в расписании/);
    assert.match(message.value, /Маршрут и направление/);
    message.value = 'Маршрут 102 & остановка';
    message.events.input();
    for (const link of providers) {
        const url = new URL(link.href);
        const params = link.dataset.provider === 'yandex' ? new URLSearchParams(url.hash.split('?')[1]) : url.searchParams;
        assert.equal(params.get('body'), message.value, link.dataset.provider);
        assert.equal(params.get(link.dataset.provider === 'gmail' ? 'su' : 'subject'), subject.value);
        assert.equal(link.dataset.provider === 'app' ? url.pathname : params.get('to'), 'vsemirka200@bk.ru');
    }
});

test('mailto encodes spaces and special characters for URI decoding', () => {
    const { subject, message, providers } = createForm();
    subject.value = 'Bus + route & stop';
    message.value = 'First line\nSecond + line & stop';
    subject.events.input();
    message.events.input();
    const link = providers.find(item => item.dataset.provider === 'app').href;
    const fields = Object.fromEntries(link.split('?')[1].split('&').map(field => {
        const [name, value] = field.split('=');
        return [name, decodeURIComponent(value)];
    }));
    assert.equal(fields.subject, subject.value);
    assert.equal(fields.body, message.value);
    assert.ok(!link.includes('+'));
});

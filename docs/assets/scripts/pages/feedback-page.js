/** Темы и поля письма соответствуют шаблонам обратной связи в приложении. */
const feedbackTopics = {
    bug: {
        title: "Ошибка в приложении",
        body: "Что произошло:\n\n\nКак повторить ошибку:\n1. \n2. \n\nЧто вы ожидали увидеть:\n\n\nВерсия приложения (если известно):\nВерсия Android (если известно):"
    },
    schedule: {
        title: "Неточность в расписании",
        body: "Маршрут и направление:\n\n\nПункт отправления, день и время:\n\n\nЧто указано неверно:\n\n\nИсточник или ссылка на объявление перевозчика:\n\n\nВерсия приложения (если известно):\nВерсия Android (если известно):"
    },
    suggestion: {
        title: "Предложение по улучшению",
        body: "Что можно улучшить:\n\n\nКак это поможет:\n\n\nВерсия приложения (если известно):\nВерсия Android (если известно):"
    },
    other: {
        title: "Другой вопрос",
        body: "Ваше сообщение:\n\n\nВерсия приложения (если известно):\nВерсия Android (если известно):"
    }
};

/** Готовит ссылку для выбранной почты, не отправляя письмо автоматически. */
function buildFeedbackUrl(client, topicKey) {
    const topic = feedbackTopics[topicKey];
    if (!topic) return null;
    const address = "vsemirka200@bk.ru";
    const subject = `Поехали! Славгород — ${topic.title}`;
    const mailto = `mailto:${address}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(topic.body)}`;
    switch (client) {
        case "app": return mailto;
        case "gmail": return `https://mail.google.com/mail/?view=cm&fs=1&to=${encodeURIComponent(address)}&su=${encodeURIComponent(subject)}&body=${encodeURIComponent(topic.body)}`;
        case "mailru": return `https://e.mail.ru/compose/?mailto=${encodeURIComponent(mailto)}`;
        case "yandex": return `https://mail.yandex.ru/compose?mailto=${encodeURIComponent(mailto)}`;
        default: return null;
    }
}

const feedbackStart = document.getElementById("feedback-start");
const feedbackEmail = document.getElementById("feedback-email");
const feedbackTopicSection = document.getElementById("feedback-topic");
const feedbackEmailLink = document.getElementById("feedback-email-link");

function updateFeedbackEmailLink() {
    const client = document.querySelector('input[name="feedback-client"]:checked')?.value;
    const topic = document.querySelector('input[name="feedback-topic"]:checked')?.value;
    const url = buildFeedbackUrl(client, topic);
    if (!url || !feedbackEmailLink) return;
    feedbackEmailLink.href = url;
    if (client === "app") feedbackEmailLink.removeAttribute("target");
    else feedbackEmailLink.setAttribute("target", "_blank");
    feedbackEmailLink.setAttribute("rel", "noreferrer");
}

feedbackStart?.addEventListener("click", () => {
    feedbackEmail.hidden = false;
    feedbackStart.setAttribute("aria-expanded", "true");
});
document.querySelectorAll('input[name="feedback-client"]').forEach((input) => {
    input.addEventListener("change", () => {
        feedbackTopicSection.hidden = false;
        updateFeedbackEmailLink();
        feedbackTopicSection.scrollIntoView({ behavior: "smooth", block: "nearest" });
    });
});
document.querySelectorAll('input[name="feedback-topic"]').forEach((input) => {
    input.addEventListener("change", updateFeedbackEmailLink);
});

// Сайт только подготавливает письма: отправка выполняется пользователем.
(() => {
  const subject = document.querySelector("#subject");
  const message = document.querySelector("#message");
  const templates = {
    bug: ["Поехали! Славгород — сообщение об ошибке", "Здравствуйте!\n\nВерсия приложения:\nВерсия Android:\n\nЧто я делал(а):\n\nЧто произошло:\n\nКакой результат ожидался:\n\nСпасибо!"],
    schedule: ["Поехали! Славгород — неточность в расписании", "Здравствуйте!\n\nМаршрут и направление:\nОстановка, день и время:\n\nЧто нужно исправить:\n\nИсточник информации:\n\nСпасибо!"],
    idea: ["Поехали! Славгород — предложение", "Здравствуйте!\n\nЯ предлагаю улучшить приложение:\n\n\nЭто поможет:\n\nСпасибо!"],
    question: ["Поехали! Славгород — вопрос", "Здравствуйте!\n\nУ меня есть вопрос о приложении:\n\n\nСпасибо!"],
    other: ["Поехали! Славгород — обращение", "Здравствуйте!\n\n\n\nСпасибо!"],
  };
  const address = "vsemirka200@bk.ru";
  const providers = document.querySelectorAll("[data-provider]");
  const openEmail = document.querySelector("#open-email");
  const emailSection = document.querySelector("#email");

  openEmail.addEventListener("click", () => {
    const opening = emailSection.hidden;
    emailSection.hidden = !opening;
    openEmail.setAttribute("aria-expanded", String(opening));
    openEmail.textContent = opening ? "Скрыть форму" : "Написать письмо";
    if (opening) emailSection.scrollIntoView({ behavior: "smooth", block: "start" });
  });

  /** Обновляет ссылки создания письма по текущим теме и тексту формы. */
  function updateLinks() {
    // Сервисы используют разные имена параметров; URLSearchParams кодирует текст.
    const params = new URLSearchParams({ to: address, subject: subject.value, body: message.value });
    const gmail = new URLSearchParams({ view: "cm", fs: "1", to: address, su: subject.value, body: message.value });
    const links = {
      yandex: `https://mail.yandex.ru/#compose?${params}`,
      mailru: `https://e.mail.ru/compose/?${params}`,
      gmail: `https://mail.google.com/mail/?${gmail}`,
      outlook: `https://outlook.live.com/mail/0/deeplink/compose?${params}`,
      yahoo: `https://compose.mail.yahoo.com/?${params}`,
      proton: `https://mail.proton.me/u/0/compose?${params}`,
      app: `mailto:${address}?${new URLSearchParams({ subject: subject.value, body: message.value })}`,
    };
    providers.forEach((link) => { link.href = links[link.dataset.provider]; });
  }

  document.querySelectorAll("[data-template]").forEach((button) => {
    button.addEventListener("click", () => {
      [subject.value, message.value] = templates[button.dataset.template];
      document.querySelectorAll("[data-template]").forEach((item) => {
        item.setAttribute("aria-pressed", String(item === button));
      });
      updateLinks();
    });
    button.setAttribute("aria-pressed", String(button.dataset.template === "question"));
  });
  subject.addEventListener("input", updateLinks);
  message.addEventListener("input", updateLinks);
  updateLinks();
})();

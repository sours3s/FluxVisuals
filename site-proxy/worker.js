/**
 * Бесплатный прокси сайта FluxVisuals через Cloudflare Workers.
 * Даёт адрес вида https://fluxvisuals.<ваш-аккаунт>.workers.dev без покупки домена.
 *
 * Сайт использует относительные пути (/api, /css, /js), поэтому за прокси
 * работает полностью: логин, регистрация, кабинет, скачивание и API.
 */
const DEFAULT_ORIGIN = 'fluxvisuals-server.onrender.com';

export default {
  async fetch(request, env) {
    const origin = (env.ORIGIN || DEFAULT_ORIGIN).trim();
    const url = new URL(request.url);

    url.protocol = 'https:';
    url.host = origin;
    url.port = '';

    // Render выбирает сервис по Host — обязательно подменяем его на origin сервера.
    const headers = new Headers(request.headers);
    headers.set('host', origin);

    const init = {
      method: request.method,
      headers,
      redirect: 'manual'
    };

    if (request.method !== 'GET' && request.method !== 'HEAD') {
      init.body = request.body;
      init.duplex = 'half';
    }

    return fetch(url.toString(), init);
  }
};

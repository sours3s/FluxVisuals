/**
 * Бесплатный прокси FluxVisuals для адреса fluxvisuals.eu.org.
 *
 * Маршруты:
 *   /, /api/*, /css/*, /js/* ... → fluxvisuals-auth на Northflank (сайт, кабинет, Catal, API)
 *   /news, /api/news/*, /bot-status → fluxvisuals-bots на Northflank (панель новостей)
 *
 * Сайт использует относительные пути (/api, /css, /js), поэтому за прокси
 * работает полностью: логин, регистрация, кабинет, скачивание и API.
 */
const DEFAULT_ORIGIN = 'p01--fluxvisuals-auth--7wqqdlfq28t5.code.run';
const NEWS_ORIGIN = 'p01--fluxvisuals-bots--7wqqdlfq28t5.code.run';

function targetFor(pathname, origin, newsOrigin) {
  if (
    pathname === '/news' ||
    pathname.startsWith('/news/') ||
    pathname.startsWith('/api/news/') ||
    pathname === '/bot-status'
  ) {
    return newsOrigin;
  }
  return origin;
}

export default {
  async fetch(request, env) {
    const origin = (env.ORIGIN || DEFAULT_ORIGIN).trim();
    const newsOrigin = (env.NEWS_ORIGIN || NEWS_ORIGIN).trim();
    const url = new URL(request.url);
    const target = targetFor(url.pathname, origin, newsOrigin);

    url.protocol = 'https:';
    url.host = target;
    url.port = '';

    // Render выбирает сервис по Host — обязательно подменяем его на выбранный сервис.
    const headers = new Headers(request.headers);
    headers.set('host', target);

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

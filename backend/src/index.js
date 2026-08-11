const SOUNDCHARTS_TOKEN_URL = "https://account.soundcharts.com/oauth/token";
const SOUNDCHARTS_API_URL = "https://customer.api.soundcharts.com/api/v2.25";
const SPOTIFY_TRACK_ID = /^[A-Za-z0-9]{22}$/;
const ANDROID_PACKAGE_NAME_PATTERN =
  /^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$/;
const SHA256_FINGERPRINT = /^(?:[A-Fa-f0-9]{2}:){31}[A-Fa-f0-9]{2}$/;
const SHA256_HEX = /^[A-Fa-f0-9]{64}$/;
const CACHE_TTL_SECONDS = 7 * 24 * 60 * 60;
const ASSET_LINKS_CACHE_TTL_SECONDS = 60 * 60;

let cachedAccessToken = null;

export default {
  async fetch(request, env) {
    return handleRequest(request, env, fetch);
  },
};

export async function handleRequest(request, env, fetcher) {
  const url = new URL(request.url);
  if (request.method === "GET" && url.pathname === "/health") {
    return jsonResponse({ status: "ok" });
  }
  if (request.method === "GET" && url.pathname === "/.well-known/assetlinks.json") {
    return assetLinksResponse(env);
  }
  if (request.method === "GET" && url.pathname === "/oauth/airtable/callback") {
    return airtableCallbackLandingResponse(url);
  }

  const match = url.pathname.match(/^\/v1\/soundcharts\/tracks\/spotify\/([^/]+)$/);
  if (request.method !== "GET" || !match) {
    return jsonResponse({ error: "not_found" }, 404);
  }

  const spotifyTrackId = match[1];
  if (!SPOTIFY_TRACK_ID.test(spotifyTrackId)) {
    return jsonResponse({ error: "invalid_spotify_track_id" }, 400);
  }
  if (!env.SOUNDCHARTS_CLIENT_ID || !env.SOUNDCHARTS_CLIENT_SECRET) {
    return jsonResponse({ error: "service_not_configured" }, 503);
  }

  try {
    const accessToken = await getSoundchartsAccessToken(env, fetcher);
    const upstream = await fetcher(
      `${SOUNDCHARTS_API_URL}/song/by-platform/spotify/${spotifyTrackId}`,
      {
        headers: {
          Accept: "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    if (!upstream.ok) {
      return jsonResponse({ error: upstreamError(upstream.status) }, publicStatus(upstream.status));
    }
    const payload = await upstream.json();
    const song = payload?.object;
    if (!song || typeof song !== "object") {
      return jsonResponse({ error: "invalid_provider_response" }, 502);
    }
    return jsonResponse(
      {
        object: {
          isrc: song.isrc ?? null,
          genres: Array.isArray(song.genres) ? song.genres : [],
        },
      },
      200,
      { "Cache-Control": `public, max-age=${CACHE_TTL_SECONDS}` },
    );
  } catch {
    return jsonResponse({ error: "provider_unavailable" }, 503);
  }
}

function assetLinksResponse(env) {
  const packageName = env.ANDROID_PACKAGE_NAME?.trim();
  const fingerprints = parseCertificateFingerprints(
    env.ANDROID_SHA256_CERT_FINGERPRINTS,
  );
  if (!packageName || !ANDROID_PACKAGE_NAME_PATTERN.test(packageName) || !fingerprints) {
    return jsonResponse({ error: "service_not_configured" }, 503, {
      "Cache-Control": "no-store",
    });
  }

  return jsonResponse(
    [
      {
        relation: ["delegate_permission/common.handle_all_urls"],
        target: {
          namespace: "android_app",
          package_name: packageName,
          sha256_cert_fingerprints: fingerprints,
        },
      },
    ],
    200,
    {
      "Cache-Control": `public, max-age=${ASSET_LINKS_CACHE_TTL_SECONDS}`,
      "X-Content-Type-Options": "nosniff",
    },
  );
}

function parseCertificateFingerprints(value) {
  if (typeof value !== "string" || !value.trim()) return null;

  const fingerprints = value
    .split(/[\s,;]+/)
    .filter(Boolean)
    .map(normalizeCertificateFingerprint);
  if (fingerprints.length === 0 || fingerprints.some((fingerprint) => !fingerprint)) {
    return null;
  }
  return [...new Set(fingerprints)];
}

function normalizeCertificateFingerprint(value) {
  if (SHA256_FINGERPRINT.test(value)) return value.toUpperCase();
  if (!SHA256_HEX.test(value)) return null;
  return value
    .toUpperCase()
    .match(/.{2}/g)
    .join(":");
}

function airtableCallbackLandingResponse(url) {
  // Android consumes the original URI. A browser fallback must not retain the
  // one-time authorization code or state in its address bar and history.
  if (url.search) {
    return new Response(null, {
      status: 303,
      headers: {
        "Cache-Control": "no-store",
        Location: url.pathname,
        "Referrer-Policy": "no-referrer",
      },
    });
  }

  const body = `<!doctype html>
<html lang="fr">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Connexion Airtable · Rekordbot</title>
    <style>
      :root { color-scheme: dark; font-family: system-ui, sans-serif; }
      body { display: grid; min-height: 100vh; margin: 0; place-items: center; background: #111318; color: #f4f6fb; }
      main { max-width: 34rem; padding: 2rem; text-align: center; }
      h1 { color: #4da3ff; }
    </style>
  </head>
  <body>
    <main>
      <h1>Retour vers Rekordbot</h1>
      <p>Si l’application ne s’est pas ouverte automatiquement, vérifiez qu’elle est installée puis relancez la connexion Airtable depuis ses réglages.</p>
    </main>
  </body>
</html>`;
  return new Response(body, {
    status: 200,
    headers: {
      "Cache-Control": "no-store",
      "Content-Security-Policy":
        "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
      "Content-Type": "text/html; charset=utf-8",
      "Permissions-Policy": "camera=(), geolocation=(), microphone=()",
      "Referrer-Policy": "no-referrer",
      "X-Content-Type-Options": "nosniff",
      "X-Frame-Options": "DENY",
    },
  });
}

async function getSoundchartsAccessToken(env, fetcher) {
  const now = Math.floor(Date.now() / 1000);
  if (cachedAccessToken?.expiresAt > now + 60) return cachedAccessToken.value;

  const form = new URLSearchParams({ grant_type: "client_credentials" });
  if (env.SOUNDCHARTS_TEAM_ID) form.set("team_id", env.SOUNDCHARTS_TEAM_ID);
  const credentials = btoa(`${env.SOUNDCHARTS_CLIENT_ID}:${env.SOUNDCHARTS_CLIENT_SECRET}`);
  const response = await fetcher(SOUNDCHARTS_TOKEN_URL, {
    method: "POST",
    headers: {
      Accept: "application/json",
      Authorization: `Basic ${credentials}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: form.toString(),
  });
  if (!response.ok) throw new Error("Soundcharts authorization failed");
  const payload = await response.json();
  if (!payload.access_token || !Number.isFinite(payload.expires_in)) {
    throw new Error("Incomplete Soundcharts authorization response");
  }
  cachedAccessToken = {
    value: payload.access_token,
    expiresAt: now + payload.expires_in,
  };
  return cachedAccessToken.value;
}

function publicStatus(status) {
  if (status === 404 || status === 429) return status;
  if (status === 403) return 403;
  return 502;
}

function upstreamError(status) {
  if (status === 404) return "track_not_found";
  if (status === 429) return "rate_limited";
  if (status === 403) return "plan_access_denied";
  return "provider_error";
}

function jsonResponse(body, status = 200, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...headers,
    },
  });
}

export function resetTokenCacheForTests() {
  cachedAccessToken = null;
}

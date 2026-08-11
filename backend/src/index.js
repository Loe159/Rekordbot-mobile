const SOUNDCHARTS_TOKEN_URL = "https://account.soundcharts.com/oauth/token";
const SOUNDCHARTS_API_URL = "https://customer.api.soundcharts.com/api/v2.25";
const SPOTIFY_TRACK_ID = /^[A-Za-z0-9]{22}$/;
const CACHE_TTL_SECONDS = 7 * 24 * 60 * 60;

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

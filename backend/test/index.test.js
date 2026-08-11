import assert from "node:assert/strict";
import test from "node:test";
import { handleRequest, resetTokenCacheForTests } from "../src/index.js";

const env = {
  SOUNDCHARTS_CLIENT_ID: "client",
  SOUNDCHARTS_CLIENT_SECRET: "secret",
};

const androidEnv = {
  ANDROID_PACKAGE_NAME: "com.loe159.rekordbot.mobile",
  ANDROID_SHA256_CERT_FINGERPRINTS:
    "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff, " +
    "FF:EE:DD:CC:BB:AA:99:88:77:66:55:44:33:22:11:00:FF:EE:DD:CC:BB:AA:99:88:77:66:55:44:33:22:11:00",
};

test("rejects an invalid Spotify ID without calling Soundcharts", async () => {
  let calls = 0;
  const response = await handleRequest(
    new Request("https://api.example/v1/soundcharts/tracks/spotify/invalid"),
    env,
    async () => {
      calls += 1;
      return new Response();
    },
  );
  assert.equal(response.status, 400);
  assert.equal(calls, 0);
});

test("keeps credentials server-side and returns only useful metadata", async () => {
  resetTokenCacheForTests();
  const requests = [];
  const response = await handleRequest(
    new Request("https://api.example/v1/soundcharts/tracks/spotify/4uLU6hMCjMI75M1A2tKUQC"),
    env,
    async (url, init) => {
      requests.push({ url, init });
      if (url.includes("/oauth/token")) {
        return Response.json({ access_token: "short-lived", expires_in: 3600 });
      }
      return Response.json({
        object: {
          isrc: { value: "USSM19902990" },
          genres: [{ root: "Pop", sub: ["Dance Pop"] }],
          privateProviderField: "must-not-leak",
        },
      });
    },
  );

  assert.equal(response.status, 200);
  assert.equal(requests.length, 2);
  assert.match(requests[0].init.headers.Authorization, /^Basic /);
  assert.equal(requests[1].init.headers.Authorization, "Bearer short-lived");
  const body = await response.json();
  assert.equal(body.object.privateProviderField, undefined);
  assert.equal(body.object.isrc.value, "USSM19902990");
  assert.deepEqual(body.object.genres, [{ root: "Pop", sub: ["Dance Pop"] }]);
});

test("serves Android App Links association from validated configuration", async () => {
  const response = await handleRequest(
    new Request("https://api.example/.well-known/assetlinks.json"),
    androidEnv,
    fetch,
  );

  assert.equal(response.status, 200);
  assert.equal(response.headers.get("content-type"), "application/json; charset=utf-8");
  assert.equal(response.headers.get("x-content-type-options"), "nosniff");
  assert.match(response.headers.get("cache-control"), /^public, max-age=/);
  assert.deepEqual(await response.json(), [
    {
      relation: ["delegate_permission/common.handle_all_urls"],
      target: {
        namespace: "android_app",
        package_name: "com.loe159.rekordbot.mobile",
        sha256_cert_fingerprints: [
          "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF",
          "FF:EE:DD:CC:BB:AA:99:88:77:66:55:44:33:22:11:00:FF:EE:DD:CC:BB:AA:99:88:77:66:55:44:33:22:11:00",
        ],
      },
    },
  ]);
});

test("fails closed when Android App Links configuration is missing or invalid", async () => {
  for (const invalidEnv of [
    {},
    {
      ANDROID_PACKAGE_NAME: "not a package",
      ANDROID_SHA256_CERT_FINGERPRINTS:
        "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF",
    },
    {
      ANDROID_PACKAGE_NAME: "com.loe159.rekordbot.mobile",
      ANDROID_SHA256_CERT_FINGERPRINTS: "not-a-certificate-fingerprint",
    },
  ]) {
    const response = await handleRequest(
      new Request("https://api.example/.well-known/assetlinks.json"),
      invalidEnv,
      fetch,
    );
    assert.equal(response.status, 503);
    assert.equal(response.headers.get("cache-control"), "no-store");
    assert.deepEqual(await response.json(), { error: "service_not_configured" });
  }
});

test("strips OAuth values before serving the static Airtable callback fallback", async () => {
  const redirect = await handleRequest(
    new Request(
      "https://api.example/oauth/airtable/callback?code=sensitive-code&state=sensitive-state",
    ),
    {},
    fetch,
  );

  assert.equal(redirect.status, 303);
  assert.equal(redirect.headers.get("cache-control"), "no-store");
  assert.equal(redirect.headers.get("location"), "/oauth/airtable/callback");
  assert.doesNotMatch(redirect.headers.get("location"), /sensitive-code|sensitive-state/);

  const response = await handleRequest(
    new Request("https://api.example/oauth/airtable/callback"),
    {},
    fetch,
  );
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("cache-control"), "no-store");
  assert.equal(response.headers.get("referrer-policy"), "no-referrer");
  assert.equal(response.headers.get("x-frame-options"), "DENY");
  assert.match(response.headers.get("content-security-policy"), /default-src 'none'/);
  const body = await response.text();
  assert.match(body, /Retour vers Rekordbot/);
  assert.doesNotMatch(body, /sensitive-code|sensitive-state/);
});

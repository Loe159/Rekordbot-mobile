import assert from "node:assert/strict";
import test from "node:test";
import { handleRequest, resetTokenCacheForTests } from "../src/index.js";

const env = {
  SOUNDCHARTS_CLIENT_ID: "client",
  SOUNDCHARTS_CLIENT_SECRET: "secret",
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

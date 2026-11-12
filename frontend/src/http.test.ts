import { afterEach, describe, expect, it, vi } from 'vitest';
import { answer, call, MERGE_PATCH_TYPE, sending, type Session, SessionEndedError } from './http';

const session: Session = {
  accessToken: 'test-token',
  expiresAt: Date.now() + 60_000,
  person: { personId: 'p', displayName: 'Test Person' },
};

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('answer', () => {
  it('reads the content of a success, or none', async () => {
    expect(await answer(json(200, { id: 1 }))).toEqual({ outcome: 'ok', value: { id: 1 } });
    expect(await answer(new Response(null, { status: 204 }), true)).toEqual({ outcome: 'ok', value: null });
  });

  it('maps each refusal', async () => {
    expect(await answer(json(404, {}))).toEqual({ outcome: 'notFound' });
    expect(await answer(json(403, {}))).toEqual({ outcome: 'forbidden' });
    expect(await answer(json(409, {}))).toEqual({ outcome: 'completed' });
    expect(await answer(json(500, {}))).toEqual({ outcome: 'failed' });
    expect(await answer(json(422, { errors: [{ pointer: '/name', detail: 'name is required' }] }))).toEqual({
      outcome: 'invalid',
      problems: [{ pointer: '/name', detail: 'name is required' }],
    });
    expect(await answer(new Response('not json', { status: 422 }))).toEqual({ outcome: 'invalid', problems: [] });
  });
});

describe('call', () => {
  it('sends the token in the Authorization header to the configured address', async () => {
    const fetch = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => json(200, []));
    vi.stubGlobal('fetch', fetch);

    await call(session, '/projects', sending('PATCH', { name: 'x' }, MERGE_PATCH_TYPE));

    const [address, init] = fetch.mock.calls[0];
    const headers = new Headers(init?.headers);
    expect(address).toBe('/api/projects');
    expect(headers.get('Authorization')).toBe('Bearer test-token');
    expect(headers.get('Content-Type')).toBe(MERGE_PATCH_TYPE);
    expect(init?.method).toBe('PATCH');
    expect(init?.body).toBe('{"name":"x"}');
  });

  it('throws SessionEndedError on a 401', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => json(401, {})));
    await expect(call(session, '/me')).rejects.toBeInstanceOf(SessionEndedError);
  });
});

import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Session } from './http';
import { listEligibleParticipants } from './membersApi';
import { createProject, updateProject } from './projectsApi';
import { createTask, updateTask } from './tasksApi';

const session: Session = {
  accessToken: 'test-token',
  expiresAt: Date.now() + 60_000,
  person: { personId: 'p', displayName: 'Test Person' },
};

function capture() {
  const fetch = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
    new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }),
  );
  vi.stubGlobal('fetch', fetch);
  return fetch;
}

afterEach(() => vi.unstubAllGlobals());

describe('optional descriptions', () => {
  it.each(['\ufeff', '\u200b'])('preserves %j in project and task create/update payloads', async (description) => {
    const fetch = capture();
    await createProject(session, { name: 'Project', description });
    await updateProject(session, 'p', { name: 'Project', description });
    await createTask(session, 'p', { title: 'Task', description, priority: 'MEDIUM' });
    await updateTask(session, 'p', 't', { description });
    for (const [, init] of fetch.mock.calls) {
      expect(JSON.parse(String(init?.body)).description).toBe(description);
    }
  });

  it('omits Java-whitespace-only descriptions on create and clears them on update', async () => {
    const fetch = capture();
    const description = '\u001c\u001f\u2007';
    await createProject(session, { name: 'Project', description });
    await updateProject(session, 'p', { name: 'Project', description });
    await createTask(session, 'p', { title: 'Task', description, priority: 'MEDIUM' });
    await updateTask(session, 'p', 't', { description });
    const bodies = fetch.mock.calls.map(([, init]) => JSON.parse(String(init?.body)));
    expect(bodies.map((body) => body.description)).toEqual([undefined, null, undefined, null]);
  });
});

describe('eligible-person search', () => {
  it('preserves FEFF and trims Java whitespace before sending a query', async () => {
    const fetch = capture();
    await listEligibleParticipants(session, 'p', '\u001c\ufeff\u001f');
    expect(new URL(String(fetch.mock.calls[0][0]), 'https://example.test').searchParams.get('name')).toBe('\ufeff');
  });
});

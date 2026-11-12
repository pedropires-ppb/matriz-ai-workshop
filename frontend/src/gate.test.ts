import { describe, expect, it } from 'vitest';
import { Gate } from './gate';

describe('Gate', () => {
  it('runs one piece of work at a time and ignores the others', async () => {
    const gate = new Gate();
    let release: () => void = () => undefined;
    let runs = 0;
    const work = () =>
      new Promise<string>((resolve) => {
        runs += 1;
        release = () => resolve('done');
      });

    const first = gate.run(work);
    const second = gate.run(work);
    expect(gate.busy).toBe(true);
    release();

    expect(await first).toBe('done');
    expect(await second).toBeUndefined();
    expect(runs).toBe(1);
    expect(gate.busy).toBe(false);
  });

  it('opens again after work that fails', async () => {
    const gate = new Gate();
    await expect(gate.run(() => Promise.reject(new Error('refused')))).rejects.toThrow('refused');
    expect(await gate.run(() => Promise.resolve(2))).toBe(2);
  });
});

import { describe, expect, it } from 'vitest';
import { apiBase } from './config';

describe('apiBase', () => {
  it('is the same origin when no address is configured', () => {
    expect(apiBase(undefined)).toBe('/api');
    expect(apiBase('')).toBe('/api');
    expect(apiBase('   ')).toBe('/api');
  });

  it('adds /api to a configured origin, without doubled slashes', () => {
    expect(apiBase('https://matriz.example.test')).toBe('https://matriz.example.test/api');
    expect(apiBase(' https://matriz.example.test// ')).toBe('https://matriz.example.test/api');
  });
});

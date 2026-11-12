/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The application serves this build from the same origin as the API (ADR-004, ADR-007 decision 2).
// Vitest tests the pure modules only, in Node, with no browser (I13 decision 6).
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../target/classes/static',
    emptyOutDir: true,
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
});

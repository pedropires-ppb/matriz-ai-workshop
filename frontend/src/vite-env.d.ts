/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** The backend's origin, for example `https://matriz.example`; empty or absent means the same origin. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

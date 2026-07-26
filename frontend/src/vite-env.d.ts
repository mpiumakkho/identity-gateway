/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DIP_CHIP_BRIDGE_URL?: string;
  readonly VITE_DIP_CHIP_BRIDGE_TIMEOUT_MS?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

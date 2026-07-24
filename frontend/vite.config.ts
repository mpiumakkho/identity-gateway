import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const frontendPort = 7000;

export default defineConfig({
  plugins: [react()],
  server: {
    host: "127.0.0.1",
    port: frontendPort,
    strictPort: true,
    proxy: {
      "/api": "http://localhost:8080"
    }
  },
  preview: {
    host: "127.0.0.1",
    port: frontendPort,
    strictPort: true
  }
});
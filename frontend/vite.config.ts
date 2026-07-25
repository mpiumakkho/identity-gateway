import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const frontendPort = 7000;

export default defineConfig({
  plugins: [tailwindcss(), react()],
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
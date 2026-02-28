import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/auth": { target: "http://localhost:4004", changeOrigin: true },
      "/patients": { target: "http://localhost:4004", changeOrigin: true }
    }
  }
});

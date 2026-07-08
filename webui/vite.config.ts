import react from "@vitejs/plugin-react";
import { defineConfig, type UserConfig } from "vite";

const config: UserConfig = {
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: false,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/actuator": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
};

export default defineConfig(config);

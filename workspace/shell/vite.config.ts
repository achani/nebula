import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'shell',
      remotes: {
        codeRemote: 'http://localhost:3001/assets/remoteEntry.js',
      },
      shared: ['react', 'react-dom', 'react-router-dom', '@blueprintjs/core', 'zustand', '@tanstack/react-query']
    })
  ],
  build: {
    target: 'esnext',
    minify: false,
    cssCodeSplit: false
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true
      },
      '/ide': {
        target: 'http://localhost:8090',
        changeOrigin: true
      }
    }
  }
})

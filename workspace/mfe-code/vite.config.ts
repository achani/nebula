import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'codeRemote',
      filename: 'remoteEntry.js',
      exposes: {
        './CodeApp': './src/CodeApp.tsx',
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
    port: 3001,
    strictPort: true,
  },
  preview: {
    port: 3001,
    strictPort: true,
  }
})

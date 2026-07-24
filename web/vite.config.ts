import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import scalaJSPlugin from '@scala-js/vite-plugin-scalajs'

export default defineConfig({
  plugins: [
    tailwindcss(),
    scalaJSPlugin({ cwd: '..', projectID: 'frontend' }),
  ],
  build: {
    rollupOptions: {
      input: {
        main: 'index.html',
        vanilla: 'vanilla/index.html',
        ts: 'ts/index.html',
        scalajs: 'scalajs/index.html',
      },
    },
  },
  server: {
    proxy: {
      '/api/cask': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/cask/, ''),
      },
      '/api/zio': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/zio/, ''),
      },
    },
  },
})

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
        'vanilla-users': 'vanilla/users/index.html',
        'vanilla-users-new': 'vanilla/users/new/index.html',
        'vanilla-users-detail': 'vanilla/users/detail/index.html',
        ts: 'ts/index.html',
        'ts-users': 'ts/users/index.html',
        'ts-users-new': 'ts/users/new/index.html',
        'ts-users-detail': 'ts/users/detail/index.html',
        scalajs: 'scalajs/index.html',
        'scalajs-users': 'scalajs/users/index.html',
        'scalajs-users-new': 'scalajs/users/new/index.html',
        'scalajs-users-detail': 'scalajs/users/detail/index.html',
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

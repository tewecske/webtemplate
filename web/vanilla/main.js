const inputIds = ['note-a', 'note-b', 'note-c']

const backendSelect = document.getElementById('backend-select')
const authSection = document.getElementById('auth-section')
const appSection = document.getElementById('app-section')
const currentUserEmail = document.getElementById('current-user-email')
const loginForm = document.getElementById('login-form')
const signupForm = document.getElementById('signup-form')
const googleDevForm = document.getElementById('google-dev-form')
const logoutBtn = document.getElementById('logout-btn')
const authTabs = document.querySelectorAll('.auth-tab')

function apiBase() {
  return `/api/${backendSelect.value}`
}

function container(inputId) {
  return document.querySelector(`[data-input-id="${inputId}"]`)
}

async function save(inputId, value) {
  const res = await fetch(`${apiBase()}/entries`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ inputId, value }),
  })
  if (res.status === 401) showAuth()
}

async function refreshHistory(inputId) {
  const res = await fetch(`${apiBase()}/entries/${inputId}`, { credentials: 'same-origin' })
  if (res.status === 401) {
    showAuth()
    return
  }
  const entries = await res.json()
  render(inputId, entries)
}

function render(inputId, entries) {
  const list = container(inputId).querySelector('.history-list')
  list.innerHTML = ''
  for (const entry of entries) {
    const li = document.createElement('li')
    li.textContent = entry.value
    list.appendChild(li)
  }
}

function wireInput(inputId) {
  const root = container(inputId)
  const input = root.querySelector('.entry-input')
  const button = root.querySelector('.save-btn')

  button.addEventListener('click', async () => {
    const value = input.value.trim()
    if (!value) return
    await save(inputId, value)
    input.value = ''
    await refreshHistory(inputId)
  })
}

function showAuth() {
  authSection.classList.remove('hidden')
  appSection.classList.add('hidden')
}

function showApp(user) {
  authSection.classList.add('hidden')
  appSection.classList.remove('hidden')
  currentUserEmail.textContent = user.email
  inputIds.forEach(refreshHistory)
}

async function checkAuth() {
  const res = await fetch(`${apiBase()}/auth/me`, { credentials: 'same-origin' })
  if (res.ok) {
    showApp(await res.json())
  } else {
    showAuth()
  }
}

function setAuthTab(tab) {
  loginForm.classList.toggle('hidden', tab !== 'login')
  signupForm.classList.toggle('hidden', tab !== 'signup')
  authTabs.forEach((btn) => {
    const active = btn.dataset.authTab === tab
    btn.classList.toggle('border-indigo-600', active)
    btn.classList.toggle('text-indigo-600', active)
    btn.classList.toggle('border-transparent', !active)
    btn.classList.toggle('text-slate-500', !active)
  })
}

async function submitAuthForm(path, body, errorEl) {
  errorEl.textContent = ''
  const res = await fetch(`${apiBase()}${path}`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (res.ok) {
    showApp(await res.json())
  } else {
    const err = await res.json().catch(() => ({ error: 'request failed' }))
    errorEl.textContent = err.error || 'request failed'
  }
}

function setup() {
  inputIds.forEach(wireInput)

  authTabs.forEach((btn) => btn.addEventListener('click', () => setAuthTab(btn.dataset.authTab)))

  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const email = loginForm.querySelector('.login-email').value
    const password = loginForm.querySelector('.login-password').value
    await submitAuthForm('/auth/login', { email, password }, loginForm.querySelector('.login-error'))
  })

  signupForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const email = signupForm.querySelector('.signup-email').value
    const password = signupForm.querySelector('.signup-password').value
    await submitAuthForm('/auth/signup', { email, password }, signupForm.querySelector('.signup-error'))
  })

  googleDevForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const email = googleDevForm.querySelector('.google-dev-email').value
    await submitAuthForm('/auth/google-dev', { email }, googleDevForm.querySelector('.google-dev-error'))
  })

  logoutBtn.addEventListener('click', async () => {
    await fetch(`${apiBase()}/auth/logout`, { method: 'POST', credentials: 'same-origin' })
    showAuth()
  })

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

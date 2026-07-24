interface Entry {
  inputId: string
  value: string
  createdAt: number
}

interface CreateEntryRequest {
  inputId: string
  value: string
}

interface UserView {
  id: number
  email: string
}

interface ApiError {
  error: string
}

const inputIds = ['note-a', 'note-b', 'note-c'] as const

const backendSelect = document.getElementById('backend-select') as HTMLSelectElement
const authSection = document.getElementById('auth-section') as HTMLElement
const appSection = document.getElementById('app-section') as HTMLElement
const currentUserEmail = document.getElementById('current-user-email') as HTMLElement
const loginForm = document.getElementById('login-form') as HTMLFormElement
const signupForm = document.getElementById('signup-form') as HTMLFormElement
const googleDevForm = document.getElementById('google-dev-form') as HTMLFormElement
const logoutBtn = document.getElementById('logout-btn') as HTMLButtonElement
const authTabs = document.querySelectorAll<HTMLButtonElement>('.auth-tab')

function apiBase(): string {
  return `/api/${backendSelect.value}`
}

function container(inputId: string): HTMLElement {
  return document.querySelector(`[data-input-id="${inputId}"]`) as HTMLElement
}

async function save(inputId: string, value: string): Promise<void> {
  const body: CreateEntryRequest = { inputId, value }
  const res = await fetch(`${apiBase()}/entries`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (res.status === 401) showAuth()
}

async function refreshHistory(inputId: string): Promise<void> {
  const res = await fetch(`${apiBase()}/entries/${inputId}`, { credentials: 'same-origin' })
  if (res.status === 401) {
    showAuth()
    return
  }
  const entries: Entry[] = await res.json()
  render(inputId, entries)
}

function render(inputId: string, entries: Entry[]): void {
  const list = container(inputId).querySelector('.history-list') as HTMLElement
  list.innerHTML = ''
  for (const entry of entries) {
    const li = document.createElement('li')
    li.textContent = entry.value
    list.appendChild(li)
  }
}

function wireInput(inputId: string): void {
  const root = container(inputId)
  const input = root.querySelector('.entry-input') as HTMLInputElement
  const button = root.querySelector('.save-btn') as HTMLButtonElement

  button.addEventListener('click', async () => {
    const value = input.value.trim()
    if (!value) return
    await save(inputId, value)
    input.value = ''
    await refreshHistory(inputId)
  })
}

function showAuth(): void {
  authSection.classList.remove('hidden')
  appSection.classList.add('hidden')
}

function showApp(user: UserView): void {
  authSection.classList.add('hidden')
  appSection.classList.remove('hidden')
  currentUserEmail.textContent = user.email
  inputIds.forEach(refreshHistory)
}

async function checkAuth(): Promise<void> {
  const res = await fetch(`${apiBase()}/auth/me`, { credentials: 'same-origin' })
  if (res.ok) {
    showApp(await res.json())
  } else {
    showAuth()
  }
}

function setAuthTab(tab: string): void {
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

async function submitAuthForm(path: string, body: unknown, errorEl: HTMLElement): Promise<void> {
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
    const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
    errorEl.textContent = err.error || 'request failed'
  }
}

function setup(): void {
  inputIds.forEach(wireInput)

  authTabs.forEach((btn) => btn.addEventListener('click', () => setAuthTab(btn.dataset.authTab ?? 'login')))

  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const email = (loginForm.querySelector('.login-email') as HTMLInputElement).value
    const password = (loginForm.querySelector('.login-password') as HTMLInputElement).value
    await submitAuthForm('/auth/login', { email, password }, loginForm.querySelector('.login-error') as HTMLElement)
  })

  signupForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const email = (signupForm.querySelector('.signup-email') as HTMLInputElement).value
    const password = (signupForm.querySelector('.signup-password') as HTMLInputElement).value
    await submitAuthForm('/auth/signup', { email, password }, signupForm.querySelector('.signup-error') as HTMLElement)
  })

  googleDevForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const email = (googleDevForm.querySelector('.google-dev-email') as HTMLInputElement).value
    await submitAuthForm('/auth/google-dev', { email }, googleDevForm.querySelector('.google-dev-error') as HTMLElement)
  })

  logoutBtn.addEventListener('click', async () => {
    await fetch(`${apiBase()}/auth/logout`, { method: 'POST', credentials: 'same-origin' })
    showAuth()
  })

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

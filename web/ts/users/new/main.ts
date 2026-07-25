export {}

interface ApiError {
  error: string
}

const backendSelect = document.getElementById('backend-select') as HTMLSelectElement
const authSection = document.getElementById('auth-section') as HTMLElement
const notAdminSection = document.getElementById('not-admin-section') as HTMLElement
const createUserForm = document.getElementById('create-user-form') as HTMLFormElement
const loginForm = document.getElementById('login-form') as HTMLFormElement
const signupForm = document.getElementById('signup-form') as HTMLFormElement
const authTabs = document.querySelectorAll<HTMLButtonElement>('.auth-tab')

function apiBase(): string {
  return `/api/${backendSelect.value}`
}

function showAuth(): void {
  authSection.classList.remove('hidden')
  notAdminSection.classList.add('hidden')
  createUserForm.classList.add('hidden')
}

function showNotAdmin(): void {
  authSection.classList.add('hidden')
  notAdminSection.classList.remove('hidden')
  createUserForm.classList.add('hidden')
}

function showForm(): void {
  authSection.classList.add('hidden')
  notAdminSection.classList.add('hidden')
  createUserForm.classList.remove('hidden')
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
    checkAuth()
  } else {
    const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
    errorEl.textContent = err.error || 'request failed'
  }
}

async function checkAuth(): Promise<void> {
  const res = await fetch(`${apiBase()}/auth/me`, { credentials: 'same-origin' })
  if (!res.ok) {
    showAuth()
    return
  }
  const user = await res.json()
  if (user.isAdmin) showForm()
  else showNotAdmin()
}

function setup(): void {
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

  createUserForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const errorEl = createUserForm.querySelector('.create-user-error') as HTMLElement
    errorEl.textContent = ''
    const email = (createUserForm.querySelector('.new-user-email') as HTMLInputElement).value
    const password = (createUserForm.querySelector('.new-user-password') as HTMLInputElement).value
    const isAdmin = (createUserForm.querySelector('.new-user-is-admin') as HTMLInputElement).checked
    const res = await fetch(`${apiBase()}/admin/users`, {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, isAdmin }),
    })
    if (res.ok) {
      window.location.href = '/ts/users/index.html'
    } else {
      const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
      errorEl.textContent = err.error || 'request failed'
    }
  })

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

export {}

interface AdminUserView {
  id: number
  email: string
  isAdmin: boolean
  createdAt: number
}

interface ApiError {
  error: string
}

const userId = new URLSearchParams(window.location.search).get('id')

const backendSelect = document.getElementById('backend-select') as HTMLSelectElement
const authSection = document.getElementById('auth-section') as HTMLElement
const notAdminSection = document.getElementById('not-admin-section') as HTMLElement
const notFoundSection = document.getElementById('not-found-section') as HTMLElement
const editUserForm = document.getElementById('edit-user-form') as HTMLFormElement
const deleteUserBtn = document.getElementById('delete-user-btn') as HTMLButtonElement
const loginForm = document.getElementById('login-form') as HTMLFormElement
const signupForm = document.getElementById('signup-form') as HTMLFormElement
const authTabs = document.querySelectorAll<HTMLButtonElement>('.auth-tab')

let currentUserId: number | null = null

function apiBase(): string {
  return `/api/${backendSelect.value}`
}

function hideAll(): void {
  authSection.classList.add('hidden')
  notAdminSection.classList.add('hidden')
  notFoundSection.classList.add('hidden')
  editUserForm.classList.add('hidden')
}

function showAuth(): void {
  hideAll()
  authSection.classList.remove('hidden')
}

function showNotAdmin(): void {
  hideAll()
  notAdminSection.classList.remove('hidden')
}

function showNotFound(): void {
  hideAll()
  notFoundSection.classList.remove('hidden')
}

function showForm(user: AdminUserView): void {
  hideAll()
  editUserForm.classList.remove('hidden')
  ;(editUserForm.querySelector('.edit-user-email') as HTMLInputElement).value = user.email
  ;(editUserForm.querySelector('.edit-user-password') as HTMLInputElement).value = ''
  ;(editUserForm.querySelector('.edit-user-is-admin') as HTMLInputElement).checked = user.isAdmin

  const isSelf = user.id === currentUserId
  editUserForm.querySelector('.edit-user-self-note')!.classList.toggle('hidden', !isSelf)
  deleteUserBtn.classList.toggle('hidden', isSelf)
  ;(editUserForm.querySelector('.edit-user-is-admin') as HTMLInputElement).disabled = isSelf
}

async function loadUser(): Promise<void> {
  if (!userId) {
    showNotFound()
    return
  }
  const res = await fetch(`${apiBase()}/admin/users/${userId}`, { credentials: 'same-origin' })
  if (res.status === 401) {
    showAuth()
    return
  }
  if (res.status === 403) {
    showNotAdmin()
    return
  }
  if (res.status === 404 || !res.ok) {
    showNotFound()
    return
  }
  showForm(await res.json())
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
  currentUserId = user.id
  if (user.isAdmin) await loadUser()
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

  editUserForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const errorEl = editUserForm.querySelector('.edit-user-error') as HTMLElement
    errorEl.textContent = ''
    const body: { email: string; isAdmin: boolean; password?: string } = {
      email: (editUserForm.querySelector('.edit-user-email') as HTMLInputElement).value,
      isAdmin: (editUserForm.querySelector('.edit-user-is-admin') as HTMLInputElement).checked,
    }
    const password = (editUserForm.querySelector('.edit-user-password') as HTMLInputElement).value
    if (password) body.password = password

    const res = await fetch(`${apiBase()}/admin/users/${userId}`, {
      method: 'PUT',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (res.ok) {
      showForm(await res.json())
    } else {
      const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
      errorEl.textContent = err.error || 'request failed'
    }
  })

  deleteUserBtn.addEventListener('click', async () => {
    if (!window.confirm('Delete this user? This cannot be undone.')) return
    const res = await fetch(`${apiBase()}/admin/users/${userId}`, {
      method: 'DELETE',
      credentials: 'same-origin',
    })
    if (res.ok) {
      window.location.href = '/ts/users/index.html'
    } else {
      const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
      ;(editUserForm.querySelector('.edit-user-error') as HTMLElement).textContent = err.error || 'request failed'
    }
  })

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

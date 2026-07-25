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

const backendSelect = document.getElementById('backend-select') as HTMLSelectElement
const authSection = document.getElementById('auth-section') as HTMLElement
const notAdminSection = document.getElementById('not-admin-section') as HTMLElement
const usersSection = document.getElementById('users-section') as HTMLElement
const currentUserEmail = document.getElementById('current-user-email') as HTMLElement
const usersTbody = document.getElementById('users-tbody') as HTMLElement
const usersError = document.getElementById('users-error') as HTMLElement
const loginForm = document.getElementById('login-form') as HTMLFormElement
const signupForm = document.getElementById('signup-form') as HTMLFormElement
const authTabs = document.querySelectorAll<HTMLButtonElement>('.auth-tab')

function apiBase(): string {
  return `/api/${backendSelect.value}`
}

function showAuth(): void {
  authSection.classList.remove('hidden')
  notAdminSection.classList.add('hidden')
  usersSection.classList.add('hidden')
}

function showNotAdmin(): void {
  authSection.classList.add('hidden')
  notAdminSection.classList.remove('hidden')
  usersSection.classList.add('hidden')
}

function showUsers(user: { email: string }): void {
  authSection.classList.add('hidden')
  notAdminSection.classList.add('hidden')
  usersSection.classList.remove('hidden')
  currentUserEmail.textContent = user.email
  loadUsers()
}

function renderUsers(users: AdminUserView[]): void {
  usersTbody.innerHTML = ''
  for (const u of users) {
    const tr = document.createElement('tr')
    tr.className = 'border-t border-slate-100'
    tr.innerHTML = `
      <td class="py-2">${u.email}</td>
      <td class="py-2">${u.isAdmin ? 'Yes' : 'No'}</td>
      <td class="py-2">${new Date(u.createdAt).toLocaleDateString()}</td>
      <td class="py-2 text-right">
        <a href="/ts/users/detail/index.html?id=${u.id}" class="text-indigo-600 hover:underline">View / Edit</a>
      </td>
    `
    usersTbody.appendChild(tr)
  }
}

async function loadUsers(): Promise<void> {
  usersError.textContent = ''
  const res = await fetch(`${apiBase()}/admin/users`, { credentials: 'same-origin' })
  if (res.status === 401) {
    showAuth()
    return
  }
  if (res.status === 403) {
    showNotAdmin()
    return
  }
  if (!res.ok) {
    usersError.textContent = 'Failed to load users.'
    return
  }
  renderUsers(await res.json())
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
  if (user.isAdmin) showUsers(user)
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

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

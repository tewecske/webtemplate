import UniversalRouter from 'universal-router'
import type { RouteContext } from 'universal-router'
import { mountUserMenu } from './userMenu.js'

interface Entry {
  inputId: string
  value: string
  createdAt: number
}

interface UserView {
  id: number
  email: string
  isAdmin: boolean
}

interface AdminUserView {
  id: number
  email: string
  isAdmin: boolean
  createdAt: number
}

interface ApiError {
  error: string
}

const inputIds = ['note-a', 'note-b', 'note-c'] as const
const contentSectionIds = [
  'not-admin-section',
  'not-found-section',
  'entries-section',
  'users-list-section',
  'users-new-section',
  'users-detail-section',
] as const

const backendSelect = document.getElementById('backend-select') as HTMLSelectElement
const authSection = document.getElementById('auth-section') as HTMLElement
const userMenuEl = document.getElementById('user-menu') as HTMLElement
const loginForm = document.getElementById('login-form') as HTMLFormElement
const signupForm = document.getElementById('signup-form') as HTMLFormElement
const googleDevForm = document.getElementById('google-dev-form') as HTMLFormElement
const authTabs = document.querySelectorAll<HTMLButtonElement>('.auth-tab')

const usersTbody = document.getElementById('users-tbody') as HTMLElement
const usersError = document.getElementById('users-error') as HTMLElement
const createUserForm = document.getElementById('create-user-form') as HTMLFormElement
const editUserForm = document.getElementById('edit-user-form') as HTMLFormElement
const deleteUserBtn = document.getElementById('delete-user-btn') as HTMLButtonElement

let currentUser: UserView | null = null
let currentDetailUserId: string | null = null

function apiBase(): string {
  return `/api/${backendSelect.value}`
}

function showAuth(): void {
  authSection.classList.remove('hidden')
  userMenuEl.classList.add('hidden')
  contentSectionIds.forEach((id) => document.getElementById(id)!.classList.add('hidden'))
}

function showAuthedSection(id: (typeof contentSectionIds)[number]): void {
  authSection.classList.add('hidden')
  userMenuEl.classList.remove('hidden')
  contentSectionIds.forEach((sid) => document.getElementById(sid)!.classList.toggle('hidden', sid !== id))
}

// --- entries ---

function entryContainer(inputId: string): HTMLElement {
  return document.querySelector(`[data-input-id="${inputId}"]`) as HTMLElement
}

async function saveEntry(inputId: string, value: string): Promise<void> {
  const res = await fetch(`${apiBase()}/entries`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ inputId, value }),
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
  renderEntryHistory(inputId, entries)
}

function renderEntryHistory(inputId: string, entries: Entry[]): void {
  const list = entryContainer(inputId).querySelector('.history-list') as HTMLElement
  list.innerHTML = ''
  for (const entry of entries) {
    const li = document.createElement('li')
    li.textContent = entry.value
    list.appendChild(li)
  }
}

function wireEntryInput(inputId: string): void {
  const root = entryContainer(inputId)
  const input = root.querySelector('.entry-input') as HTMLInputElement
  const button = root.querySelector('.save-btn') as HTMLButtonElement
  button.addEventListener('click', async () => {
    const value = input.value.trim()
    if (!value) return
    await saveEntry(inputId, value)
    input.value = ''
    await refreshHistory(inputId)
  })
}

// --- users list ---

function renderUsers(users: AdminUserView[]): void {
  usersTbody.innerHTML = ''
  for (const u of users) {
    const tr = document.createElement('tr')
    tr.className = 'border-t border-base-200'
    tr.innerHTML = `
      <td class="py-2">${u.email}</td>
      <td class="py-2">${u.isAdmin ? 'Yes' : 'No'}</td>
      <td class="py-2">${new Date(u.createdAt).toLocaleDateString()}</td>
      <td class="py-2 text-right">
        <a href="#/users/${u.id}" class="text-primary hover:underline">View / Edit</a>
      </td>
    `
    usersTbody.appendChild(tr)
  }
}

async function loadUsersList(): Promise<void> {
  usersError.textContent = ''
  const res = await fetch(`${apiBase()}/admin/users`, { credentials: 'same-origin' })
  if (res.status === 401) {
    showAuth()
    return
  }
  if (res.status === 403) {
    showAuthedSection('not-admin-section')
    return
  }
  if (!res.ok) {
    usersError.textContent = 'Failed to load users.'
    return
  }
  renderUsers(await res.json())
}

// --- create user ---

function resetCreateUserForm(): void {
  createUserForm.reset()
  ;(createUserForm.querySelector('.create-user-error') as HTMLElement).textContent = ''
}

// --- user detail ---

function fillDetailForm(user: AdminUserView): void {
  ;(editUserForm.querySelector('.edit-user-email') as HTMLInputElement).value = user.email
  ;(editUserForm.querySelector('.edit-user-password') as HTMLInputElement).value = ''
  ;(editUserForm.querySelector('.edit-user-is-admin') as HTMLInputElement).checked = user.isAdmin

  const isSelf = currentUser != null && user.id === currentUser.id
  editUserForm.querySelector('.edit-user-self-note')!.classList.toggle('hidden', !isSelf)
  deleteUserBtn.classList.toggle('hidden', isSelf)
  ;(editUserForm.querySelector('.edit-user-is-admin') as HTMLInputElement).disabled = isSelf
}

async function loadUserDetail(id: string): Promise<void> {
  currentDetailUserId = id
  const res = await fetch(`${apiBase()}/admin/users/${id}`, { credentials: 'same-origin' })
  if (res.status === 401) {
    showAuth()
    return
  }
  if (res.status === 403) {
    showAuthedSection('not-admin-section')
    return
  }
  if (res.status === 404 || !res.ok) {
    showAuthedSection('not-found-section')
    return
  }
  fillDetailForm(await res.json())
  showAuthedSection('users-detail-section')
}

// --- router ---
// Hash-based (not History API): each variant is an independent Vite entry, so path-based
// routing would need a per-prefix server rewrite rule in both dev and prod. Hash routing needs
// no server config at all.

const router = new UniversalRouter<boolean>([
  {
    path: '/',
    action: async () => {
      showAuthedSection('entries-section')
      inputIds.forEach(refreshHistory)
      return true
    },
  },
  {
    path: '/users',
    action: async () => {
      if (!currentUser!.isAdmin) {
        showAuthedSection('not-admin-section')
        return true
      }
      showAuthedSection('users-list-section')
      await loadUsersList()
      return true
    },
  },
  {
    path: '/users/new',
    action: async () => {
      if (!currentUser!.isAdmin) {
        showAuthedSection('not-admin-section')
        return true
      }
      resetCreateUserForm()
      showAuthedSection('users-new-section')
      return true
    },
  },
  {
    path: '/users/:id',
    action: async (context: RouteContext<boolean>) => {
      if (!currentUser!.isAdmin) {
        showAuthedSection('not-admin-section')
        return true
      }
      await loadUserDetail(context.params.id as string)
      return true
    },
  },
])

function resolveRoute(): void {
  const path = location.hash.slice(1) || '/'
  router.resolve(path).catch(() => showAuthedSection('not-found-section'))
}

// --- auth ---

function setAuthTab(tab: string): void {
  loginForm.classList.toggle('hidden', tab !== 'login')
  signupForm.classList.toggle('hidden', tab !== 'signup')
  authTabs.forEach((btn) => {
    const active = btn.dataset.authTab === tab
    btn.classList.toggle('border-primary', active)
    btn.classList.toggle('text-primary', active)
    btn.classList.toggle('border-transparent', !active)
    btn.classList.toggle('text-base-content/60', !active)
  })
}

function afterAuthSuccess(): void {
  mountUserMenu(userMenuEl, currentUser!, {
    onManageUsers: () => {
      location.hash = '#/users'
    },
    onLogout: async () => {
      await fetch(`${apiBase()}/auth/logout`, { method: 'POST', credentials: 'same-origin' })
      currentUser = null
      showAuth()
    },
  })
  resolveRoute()
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
    currentUser = await res.json()
    afterAuthSuccess()
  } else {
    const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
    errorEl.textContent = err.error || 'request failed'
  }
}

async function checkAuth(): Promise<void> {
  const res = await fetch(`${apiBase()}/auth/me`, { credentials: 'same-origin' })
  if (res.ok) {
    currentUser = await res.json()
    afterAuthSuccess()
  } else {
    currentUser = null
    showAuth()
  }
}

function setup(): void {
  inputIds.forEach(wireEntryInput)

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
      location.hash = '#/users'
    } else {
      const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
      errorEl.textContent = err.error || 'request failed'
    }
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

    const res = await fetch(`${apiBase()}/admin/users/${currentDetailUserId}`, {
      method: 'PUT',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (res.ok) {
      fillDetailForm(await res.json())
    } else {
      const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
      errorEl.textContent = err.error || 'request failed'
    }
  })

  deleteUserBtn.addEventListener('click', async () => {
    if (!window.confirm('Delete this user? This cannot be undone.')) return
    const res = await fetch(`${apiBase()}/admin/users/${currentDetailUserId}`, {
      method: 'DELETE',
      credentials: 'same-origin',
    })
    if (res.ok) {
      location.hash = '#/users'
    } else {
      const err: ApiError = await res.json().catch(() => ({ error: 'request failed' }))
      ;(editUserForm.querySelector('.edit-user-error') as HTMLElement).textContent = err.error || 'request failed'
    }
  })

  backendSelect.addEventListener('change', () => checkAuth())
  window.addEventListener('hashchange', () => {
    if (currentUser) resolveRoute()
  })

  checkAuth()
}

setup()

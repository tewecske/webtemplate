import UniversalRouter from 'universal-router'
import { mountUserMenu } from './userMenu.js'

const inputIds = ['note-a', 'note-b', 'note-c']
const contentSectionIds = [
  'not-admin-section',
  'not-found-section',
  'entries-section',
  'users-list-section',
  'users-new-section',
  'users-detail-section',
]

const backendSelect = document.getElementById('backend-select')
const authSection = document.getElementById('auth-section')
const userMenuEl = document.getElementById('user-menu')
const loginForm = document.getElementById('login-form')
const signupForm = document.getElementById('signup-form')
const googleDevForm = document.getElementById('google-dev-form')
const authTabs = document.querySelectorAll('.auth-tab')

const usersTbody = document.getElementById('users-tbody')
const usersError = document.getElementById('users-error')
const createUserForm = document.getElementById('create-user-form')
const editUserForm = document.getElementById('edit-user-form')
const deleteUserBtn = document.getElementById('delete-user-btn')

let currentUser = null
let currentDetailUserId = null

function apiBase() {
  return `/api/${backendSelect.value}`
}

function showAuth() {
  authSection.classList.remove('hidden')
  userMenuEl.classList.add('hidden')
  contentSectionIds.forEach((id) => document.getElementById(id).classList.add('hidden'))
}

function showAuthedSection(id) {
  authSection.classList.add('hidden')
  userMenuEl.classList.remove('hidden')
  contentSectionIds.forEach((sid) => document.getElementById(sid).classList.toggle('hidden', sid !== id))
}

// --- entries ---

function entryContainer(inputId) {
  return document.querySelector(`[data-input-id="${inputId}"]`)
}

async function saveEntry(inputId, value) {
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
  renderEntryHistory(inputId, entries)
}

function renderEntryHistory(inputId, entries) {
  const list = entryContainer(inputId).querySelector('.history-list')
  list.innerHTML = ''
  for (const entry of entries) {
    const li = document.createElement('li')
    li.textContent = entry.value
    list.appendChild(li)
  }
}

function wireEntryInput(inputId) {
  const root = entryContainer(inputId)
  const input = root.querySelector('.entry-input')
  const button = root.querySelector('.save-btn')
  button.addEventListener('click', async () => {
    const value = input.value.trim()
    if (!value) return
    await saveEntry(inputId, value)
    input.value = ''
    await refreshHistory(inputId)
  })
}

// --- users list ---

function renderUsers(users) {
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

async function loadUsersList() {
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

function resetCreateUserForm() {
  createUserForm.reset()
  createUserForm.querySelector('.create-user-error').textContent = ''
}

// --- user detail ---

function fillDetailForm(user) {
  editUserForm.querySelector('.edit-user-email').value = user.email
  editUserForm.querySelector('.edit-user-password').value = ''
  editUserForm.querySelector('.edit-user-is-admin').checked = user.isAdmin

  const isSelf = currentUser != null && user.id === currentUser.id
  editUserForm.querySelector('.edit-user-self-note').classList.toggle('hidden', !isSelf)
  deleteUserBtn.classList.toggle('hidden', isSelf)
  editUserForm.querySelector('.edit-user-is-admin').disabled = isSelf
}

async function loadUserDetail(id) {
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

const router = new UniversalRouter([
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
      if (!currentUser.isAdmin) {
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
      if (!currentUser.isAdmin) {
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
    action: async (context) => {
      if (!currentUser.isAdmin) {
        showAuthedSection('not-admin-section')
        return true
      }
      await loadUserDetail(context.params.id)
      return true
    },
  },
])

function resolveRoute() {
  const path = location.hash.slice(1) || '/'
  router.resolve(path).catch(() => showAuthedSection('not-found-section'))
}

// --- auth ---

function setAuthTab(tab) {
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

function afterAuthSuccess() {
  mountUserMenu(userMenuEl, currentUser, {
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

async function submitAuthForm(path, body, errorEl) {
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
    const err = await res.json().catch(() => ({ error: 'request failed' }))
    errorEl.textContent = err.error || 'request failed'
  }
}

async function checkAuth() {
  const res = await fetch(`${apiBase()}/auth/me`, { credentials: 'same-origin' })
  if (res.ok) {
    currentUser = await res.json()
    afterAuthSuccess()
  } else {
    currentUser = null
    showAuth()
  }
}

function setup() {
  inputIds.forEach(wireEntryInput)

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

  createUserForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const errorEl = createUserForm.querySelector('.create-user-error')
    errorEl.textContent = ''
    const email = createUserForm.querySelector('.new-user-email').value
    const password = createUserForm.querySelector('.new-user-password').value
    const isAdmin = createUserForm.querySelector('.new-user-is-admin').checked
    const res = await fetch(`${apiBase()}/admin/users`, {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, isAdmin }),
    })
    if (res.ok) {
      location.hash = '#/users'
    } else {
      const err = await res.json().catch(() => ({ error: 'request failed' }))
      errorEl.textContent = err.error || 'request failed'
    }
  })

  editUserForm.addEventListener('submit', async (e) => {
    e.preventDefault()
    const errorEl = editUserForm.querySelector('.edit-user-error')
    errorEl.textContent = ''
    const body = {
      email: editUserForm.querySelector('.edit-user-email').value,
      isAdmin: editUserForm.querySelector('.edit-user-is-admin').checked,
    }
    const password = editUserForm.querySelector('.edit-user-password').value
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
      const err = await res.json().catch(() => ({ error: 'request failed' }))
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
      const err = await res.json().catch(() => ({ error: 'request failed' }))
      editUserForm.querySelector('.edit-user-error').textContent = err.error || 'request failed'
    }
  })

  backendSelect.addEventListener('change', () => checkAuth())
  window.addEventListener('hashchange', () => {
    if (currentUser) resolveRoute()
  })

  checkAuth()
}

setup()

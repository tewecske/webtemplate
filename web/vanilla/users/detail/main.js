const userId = new URLSearchParams(window.location.search).get('id')

const backendSelect = document.getElementById('backend-select')
const authSection = document.getElementById('auth-section')
const notAdminSection = document.getElementById('not-admin-section')
const notFoundSection = document.getElementById('not-found-section')
const editUserForm = document.getElementById('edit-user-form')
const deleteUserBtn = document.getElementById('delete-user-btn')
const loginForm = document.getElementById('login-form')
const signupForm = document.getElementById('signup-form')
const authTabs = document.querySelectorAll('.auth-tab')

let currentUserId = null

function apiBase() {
  return `/api/${backendSelect.value}`
}

function hideAll() {
  authSection.classList.add('hidden')
  notAdminSection.classList.add('hidden')
  notFoundSection.classList.add('hidden')
  editUserForm.classList.add('hidden')
}

function showAuth() {
  hideAll()
  authSection.classList.remove('hidden')
}

function showNotAdmin() {
  hideAll()
  notAdminSection.classList.remove('hidden')
}

function showNotFound() {
  hideAll()
  notFoundSection.classList.remove('hidden')
}

function showForm(user) {
  hideAll()
  editUserForm.classList.remove('hidden')
  editUserForm.querySelector('.edit-user-email').value = user.email
  editUserForm.querySelector('.edit-user-password').value = ''
  editUserForm.querySelector('.edit-user-is-admin').checked = user.isAdmin

  const isSelf = user.id === currentUserId
  editUserForm.querySelector('.edit-user-self-note').classList.toggle('hidden', !isSelf)
  deleteUserBtn.classList.toggle('hidden', isSelf)
  editUserForm.querySelector('.edit-user-is-admin').disabled = isSelf
}

async function loadUser() {
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
  if (res.status === 404) {
    showNotFound()
    return
  }
  if (!res.ok) {
    showNotFound()
    return
  }
  showForm(await res.json())
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
    checkAuth()
  } else {
    const err = await res.json().catch(() => ({ error: 'request failed' }))
    errorEl.textContent = err.error || 'request failed'
  }
}

async function checkAuth() {
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

function setup() {
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

    const res = await fetch(`${apiBase()}/admin/users/${userId}`, {
      method: 'PUT',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (res.ok) {
      showForm(await res.json())
    } else {
      const err = await res.json().catch(() => ({ error: 'request failed' }))
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
      window.location.href = '/vanilla/users/index.html'
    } else {
      const err = await res.json().catch(() => ({ error: 'request failed' }))
      editUserForm.querySelector('.edit-user-error').textContent = err.error || 'request failed'
    }
  })

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

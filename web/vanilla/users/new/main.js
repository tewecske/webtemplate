const backendSelect = document.getElementById('backend-select')
const authSection = document.getElementById('auth-section')
const notAdminSection = document.getElementById('not-admin-section')
const createUserForm = document.getElementById('create-user-form')
const loginForm = document.getElementById('login-form')
const signupForm = document.getElementById('signup-form')
const authTabs = document.querySelectorAll('.auth-tab')

function apiBase() {
  return `/api/${backendSelect.value}`
}

function showAuth() {
  authSection.classList.remove('hidden')
  notAdminSection.classList.add('hidden')
  createUserForm.classList.add('hidden')
}

function showNotAdmin() {
  authSection.classList.add('hidden')
  notAdminSection.classList.remove('hidden')
  createUserForm.classList.add('hidden')
}

function showForm() {
  authSection.classList.add('hidden')
  notAdminSection.classList.add('hidden')
  createUserForm.classList.remove('hidden')
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
  if (user.isAdmin) showForm()
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
      window.location.href = '/vanilla/users/index.html'
    } else {
      const err = await res.json().catch(() => ({ error: 'request failed' }))
      errorEl.textContent = err.error || 'request failed'
    }
  })

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

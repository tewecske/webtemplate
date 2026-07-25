const backendSelect = document.getElementById('backend-select')
const authSection = document.getElementById('auth-section')
const notAdminSection = document.getElementById('not-admin-section')
const usersSection = document.getElementById('users-section')
const currentUserEmail = document.getElementById('current-user-email')
const usersTbody = document.getElementById('users-tbody')
const usersError = document.getElementById('users-error')
const loginForm = document.getElementById('login-form')
const signupForm = document.getElementById('signup-form')
const authTabs = document.querySelectorAll('.auth-tab')

function apiBase() {
  return `/api/${backendSelect.value}`
}

function showAuth() {
  authSection.classList.remove('hidden')
  notAdminSection.classList.add('hidden')
  usersSection.classList.add('hidden')
}

function showNotAdmin() {
  authSection.classList.add('hidden')
  notAdminSection.classList.remove('hidden')
  usersSection.classList.add('hidden')
}

function showUsers(user) {
  authSection.classList.add('hidden')
  notAdminSection.classList.add('hidden')
  usersSection.classList.remove('hidden')
  currentUserEmail.textContent = user.email
  loadUsers()
}

function renderUsers(users) {
  usersTbody.innerHTML = ''
  for (const u of users) {
    const tr = document.createElement('tr')
    tr.className = 'border-t border-slate-100'
    tr.innerHTML = `
      <td class="py-2">${u.email}</td>
      <td class="py-2">${u.isAdmin ? 'Yes' : 'No'}</td>
      <td class="py-2">${new Date(u.createdAt).toLocaleDateString()}</td>
      <td class="py-2 text-right">
        <a href="/vanilla/users/detail/index.html?id=${u.id}" class="text-indigo-600 hover:underline">View / Edit</a>
      </td>
    `
    usersTbody.appendChild(tr)
  }
}

async function loadUsers() {
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
  if (user.isAdmin) showUsers(user)
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

  backendSelect.addEventListener('change', () => checkAuth())
  checkAuth()
}

setup()

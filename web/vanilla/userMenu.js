export function mountUserMenu(container, user, { onManageUsers, onLogout }) {
  container.innerHTML = `
    <p class="text-base-content/70">Signed in as <span class="menu-email font-semibold text-base-content"></span></p>
    <div class="flex gap-2">
      <a href="#/users" class="manage-users-link hidden btn btn-outline btn-sm">Manage users</a>
      <button type="button" class="logout-btn btn btn-outline btn-sm">Log out</button>
    </div>
  `

  container.querySelector('.menu-email').textContent = user.email

  const manageLink = container.querySelector('.manage-users-link')
  manageLink.classList.toggle('hidden', !user.isAdmin)
  manageLink.addEventListener('click', (e) => {
    e.preventDefault()
    onManageUsers()
  })

  container.querySelector('.logout-btn').addEventListener('click', onLogout)
}

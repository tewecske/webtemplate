export {}

interface MenuUser {
  email: string
  isAdmin: boolean
}

interface MountUserMenuOptions {
  onManageUsers: () => void
  onLogout: () => void
}

export function mountUserMenu(container: HTMLElement, user: MenuUser, opts: MountUserMenuOptions): void {
  container.innerHTML = `
    <p class="text-base-content/70">Signed in as <span class="menu-email font-semibold text-base-content"></span></p>
    <div class="flex gap-2">
      <a href="#/users" class="manage-users-link hidden btn btn-outline btn-sm">Manage users</a>
      <button type="button" class="logout-btn btn btn-outline btn-sm">Log out</button>
    </div>
  `

  ;(container.querySelector('.menu-email') as HTMLElement).textContent = user.email

  const manageLink = container.querySelector('.manage-users-link') as HTMLAnchorElement
  manageLink.classList.toggle('hidden', !user.isAdmin)
  manageLink.addEventListener('click', (e) => {
    e.preventDefault()
    opts.onManageUsers()
  })

  ;(container.querySelector('.logout-btn') as HTMLButtonElement).addEventListener('click', opts.onLogout)
}

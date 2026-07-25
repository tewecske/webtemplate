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
    <p class="text-slate-600">Signed in as <span class="menu-email font-semibold text-slate-900"></span></p>
    <div class="flex gap-2">
      <a href="#/users" class="manage-users-link hidden rounded-md border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100">Manage users</a>
      <button type="button" class="logout-btn rounded-md border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100">Log out</button>
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

package webtemplate.frontend

import org.scalajs.dom
import org.scalajs.dom.html
import webtemplate.shared.UserView

object UserMenu {

  def mount(container: dom.Element, user: UserView, onManageUsers: () => Unit, onLogout: () => Unit): Unit = {
    container.innerHTML = """
      <p class="text-slate-600">Signed in as <span class="menu-email font-semibold text-slate-900"></span></p>
      <div class="flex gap-2">
        <a href="#/users" class="manage-users-link hidden rounded-md border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100">Manage users</a>
        <button type="button" class="logout-btn rounded-md border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100">Log out</button>
      </div>
    """

    container.querySelector(".menu-email").textContent = user.email

    val manageLink = container.querySelector(".manage-users-link").asInstanceOf[html.Anchor]
    manageLink.classList.toggle("hidden", !user.isAdmin)
    manageLink.addEventListener(
      "click",
      (e: dom.Event) => {
        e.preventDefault()
        onManageUsers()
      }
    )

    container.querySelector(".logout-btn").addEventListener("click", (_: dom.Event) => onLogout())
  }
}

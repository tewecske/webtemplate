package webtemplate.frontend

import org.scalajs.dom
import org.scalajs.dom.{document, html}
import webtemplate.shared.{AdminUserView, ApiError, LoginRequest, SignupRequest, UpdateUserRequest, UserView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits._
import scala.util.{Failure, Success, Try}

object UserDetailPage {

  private var currentUserId: Option[Long] = None

  private def userId: Option[Long] = {
    val params = new dom.URLSearchParams(dom.window.location.search)
    Option(params.get("id")).flatMap(s => Try(s.toLong).toOption)
  }

  def setup(): Unit = {
    loginTab.addEventListener("click", (_: dom.Event) => setAuthTab("login"))
    signupTab.addEventListener("click", (_: dom.Event) => setAuthTab("signup"))

    loginForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val email = loginForm.querySelector(".login-email").asInstanceOf[html.Input].value
        val password = loginForm.querySelector(".login-password").asInstanceOf[html.Input].value
        submitAuthJson("/auth/login", upickle.default.write(LoginRequest(email, password)), loginForm.querySelector(".login-error"))
      }
    )

    signupForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val email = signupForm.querySelector(".signup-email").asInstanceOf[html.Input].value
        val password = signupForm.querySelector(".signup-password").asInstanceOf[html.Input].value
        submitAuthJson(
          "/auth/signup",
          upickle.default.write(SignupRequest(email, password)),
          signupForm.querySelector(".signup-error")
        )
      }
    )

    editUserForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val errorEl = editUserForm.querySelector(".edit-user-error")
        errorEl.textContent = ""
        val email = editUserForm.querySelector(".edit-user-email").asInstanceOf[html.Input].value
        val isAdmin = editUserForm.querySelector(".edit-user-is-admin").asInstanceOf[html.Input].checked
        val password = editUserForm.querySelector(".edit-user-password").asInstanceOf[html.Input].value
        val passwordOpt = if (password.isEmpty) None else Some(password)

        val init = new dom.RequestInit {
          method = dom.HttpMethod.PUT
          headers = jsonHeaders()
          credentials = dom.RequestCredentials.`same-origin`
          body = upickle.default.write(UpdateUserRequest(Some(email), passwordOpt, Some(isAdmin)))
        }
        dom
          .fetch(s"${apiBase()}/admin/users/${userId.getOrElse("")}", init)
          .flatMap(res => res.text().map(text => (res.ok, text)))
          .onComplete {
            case Success((true, text)) => showForm(upickle.default.read[AdminUserView](text))
            case Success((false, text)) =>
              val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
              errorEl.textContent = err.error
            case Failure(_) => errorEl.textContent = "request failed"
          }
      }
    )

    deleteUserBtn.addEventListener(
      "click",
      (_: dom.Event) => {
        if (dom.window.confirm("Delete this user? This cannot be undone.")) {
          val init = new dom.RequestInit {
            method = dom.HttpMethod.DELETE
            credentials = dom.RequestCredentials.`same-origin`
          }
          dom
            .fetch(s"${apiBase()}/admin/users/${userId.getOrElse("")}", init)
            .flatMap(res => res.text().map(text => (res.ok, text)))
            .onComplete {
              case Success((true, _)) => dom.window.location.href = "/scalajs/users/index.html"
              case Success((false, text)) =>
                val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
                editUserForm.querySelector(".edit-user-error").textContent = err.error
              case Failure(_) => editUserForm.querySelector(".edit-user-error").textContent = "request failed"
            }
        }
      }
    )

    backendSelect.addEventListener("change", (_: dom.Event) => checkAuth())
    checkAuth()
  }

  private def backendSelect: html.Select = document.getElementById("backend-select").asInstanceOf[html.Select]
  private def authSection: dom.Element = document.getElementById("auth-section")
  private def notAdminSection: dom.Element = document.getElementById("not-admin-section")
  private def notFoundSection: dom.Element = document.getElementById("not-found-section")
  private def editUserForm: html.Form = document.getElementById("edit-user-form").asInstanceOf[html.Form]
  private def deleteUserBtn: html.Button = document.getElementById("delete-user-btn").asInstanceOf[html.Button]
  private def loginForm: html.Form = document.getElementById("login-form").asInstanceOf[html.Form]
  private def signupForm: html.Form = document.getElementById("signup-form").asInstanceOf[html.Form]
  private def loginTab: html.Button = document.querySelector("[data-auth-tab='login']").asInstanceOf[html.Button]
  private def signupTab: html.Button = document.querySelector("[data-auth-tab='signup']").asInstanceOf[html.Button]

  private def apiBase(): String = s"/api/${backendSelect.value}"

  private def jsonHeaders(): dom.Headers = {
    val h = new dom.Headers()
    h.append("Content-Type", "application/json")
    h
  }

  private def hideAll(): Unit = {
    authSection.classList.add("hidden")
    notAdminSection.classList.add("hidden")
    notFoundSection.classList.add("hidden")
    editUserForm.classList.add("hidden")
  }

  private def showAuth(): Unit = {
    hideAll()
    authSection.classList.remove("hidden")
  }

  private def showNotAdmin(): Unit = {
    hideAll()
    notAdminSection.classList.remove("hidden")
  }

  private def showNotFound(): Unit = {
    hideAll()
    notFoundSection.classList.remove("hidden")
  }

  private def showForm(user: AdminUserView): Unit = {
    hideAll()
    editUserForm.classList.remove("hidden")
    editUserForm.querySelector(".edit-user-email").asInstanceOf[html.Input].value = user.email
    editUserForm.querySelector(".edit-user-password").asInstanceOf[html.Input].value = ""
    editUserForm.querySelector(".edit-user-is-admin").asInstanceOf[html.Input].checked = user.isAdmin

    val isSelf = currentUserId.contains(user.id)
    editUserForm.querySelector(".edit-user-self-note").classList.toggle("hidden", !isSelf)
    deleteUserBtn.classList.toggle("hidden", isSelf)
    editUserForm.querySelector(".edit-user-is-admin").asInstanceOf[html.Input].disabled = isSelf
  }

  private def loadUser(): Unit =
    userId match {
      case None => showNotFound()
      case Some(id) =>
        val init = new dom.RequestInit { credentials = dom.RequestCredentials.`same-origin` }
        dom
          .fetch(s"${apiBase()}/admin/users/$id", init)
          .flatMap(res => res.text().map(text => (res.status, text)))
          .onComplete {
            case Success((200, text)) => showForm(upickle.default.read[AdminUserView](text))
            case Success((401, _))    => showAuth()
            case Success((403, _))    => showNotAdmin()
            case Success(_)           => showNotFound()
            case Failure(_)           => showNotFound()
          }
    }

  private def setAuthTab(tab: String): Unit = {
    val isLogin = tab == "login"
    loginForm.classList.toggle("hidden", !isLogin)
    signupForm.classList.toggle("hidden", isLogin)
    setTabActive(loginTab, isLogin)
    setTabActive(signupTab, !isLogin)
  }

  private def setTabActive(btn: html.Button, active: Boolean): Unit = {
    btn.classList.toggle("border-indigo-600", active)
    btn.classList.toggle("text-indigo-600", active)
    btn.classList.toggle("border-transparent", !active)
    btn.classList.toggle("text-slate-500", !active)
  }

  private def checkAuth(): Unit = {
    val init = new dom.RequestInit { credentials = dom.RequestCredentials.`same-origin` }
    dom
      .fetch(s"${apiBase()}/auth/me", init)
      .flatMap(res => res.text().map(text => (res.ok, text)))
      .onComplete {
        case Success((true, text)) =>
          val user = upickle.default.read[UserView](text)
          currentUserId = Some(user.id)
          if (user.isAdmin) loadUser() else showNotAdmin()
        case _ => showAuth()
      }
  }

  private def submitAuthJson(path: String, jsonBody: String, errorEl: dom.Element): Unit = {
    errorEl.textContent = ""
    val init = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = jsonHeaders()
      credentials = dom.RequestCredentials.`same-origin`
      body = jsonBody
    }
    dom
      .fetch(s"${apiBase()}$path", init)
      .flatMap(res => res.text().map(text => (res.ok, text)))
      .onComplete {
        case Success((true, _))     => checkAuth()
        case Success((false, text)) =>
          val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
          errorEl.textContent = err.error
        case Failure(_) => errorEl.textContent = "request failed"
      }
  }
}

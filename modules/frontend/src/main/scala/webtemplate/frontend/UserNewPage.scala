package webtemplate.frontend

import org.scalajs.dom
import org.scalajs.dom.{document, html}
import webtemplate.shared.{ApiError, CreateUserRequest, LoginRequest, SignupRequest, UserView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits._
import scala.util.{Failure, Success, Try}

object UserNewPage {

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

    createUserForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val errorEl = createUserForm.querySelector(".create-user-error")
        errorEl.textContent = ""
        val email = createUserForm.querySelector(".new-user-email").asInstanceOf[html.Input].value
        val password = createUserForm.querySelector(".new-user-password").asInstanceOf[html.Input].value
        val isAdmin = createUserForm.querySelector(".new-user-is-admin").asInstanceOf[html.Input].checked

        val init = new dom.RequestInit {
          method = dom.HttpMethod.POST
          headers = jsonHeaders()
          credentials = dom.RequestCredentials.`same-origin`
          body = upickle.default.write(CreateUserRequest(email, password, isAdmin))
        }
        dom
          .fetch(s"${apiBase()}/admin/users", init)
          .flatMap(res => res.text().map(text => (res.ok, text)))
          .onComplete {
            case Success((true, _)) => dom.window.location.href = "/scalajs/users/index.html"
            case Success((false, text)) =>
              val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
              errorEl.textContent = err.error
            case Failure(_) => errorEl.textContent = "request failed"
          }
      }
    )

    backendSelect.addEventListener("change", (_: dom.Event) => checkAuth())
    checkAuth()
  }

  private def backendSelect: html.Select = document.getElementById("backend-select").asInstanceOf[html.Select]
  private def authSection: dom.Element = document.getElementById("auth-section")
  private def notAdminSection: dom.Element = document.getElementById("not-admin-section")
  private def createUserForm: html.Form = document.getElementById("create-user-form").asInstanceOf[html.Form]
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
    createUserForm.classList.add("hidden")
  }

  private def showAuth(): Unit = {
    hideAll()
    authSection.classList.remove("hidden")
  }

  private def showNotAdmin(): Unit = {
    hideAll()
    notAdminSection.classList.remove("hidden")
  }

  private def showForm(): Unit = {
    hideAll()
    createUserForm.classList.remove("hidden")
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
          if (user.isAdmin) showForm() else showNotAdmin()
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

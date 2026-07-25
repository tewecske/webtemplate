package webtemplate.frontend

import org.scalajs.dom
import org.scalajs.dom.{document, html}
import webtemplate.shared.{ApiError, CreateEntryRequest, Entry, GoogleDevLoginRequest, LoginRequest, SignupRequest, UserView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Thenable.Implicits._
import scala.util.{Failure, Success, Try}

object EntriesPage {

  private val inputIds = List("note-a", "note-b", "note-c")

  def setup(): Unit = {
    inputIds.foreach(wireInput)

    loginTab.addEventListener("click", (_: dom.Event) => setAuthTab("login"))
    signupTab.addEventListener("click", (_: dom.Event) => setAuthTab("signup"))

    loginForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val email = loginForm.querySelector(".login-email").asInstanceOf[html.Input].value
        val password = loginForm.querySelector(".login-password").asInstanceOf[html.Input].value
        submitJson("/auth/login", upickle.default.write(LoginRequest(email, password)), loginForm.querySelector(".login-error"))
      }
    )

    signupForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val email = signupForm.querySelector(".signup-email").asInstanceOf[html.Input].value
        val password = signupForm.querySelector(".signup-password").asInstanceOf[html.Input].value
        submitJson(
          "/auth/signup",
          upickle.default.write(SignupRequest(email, password)),
          signupForm.querySelector(".signup-error")
        )
      }
    )

    googleDevForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val email = googleDevForm.querySelector(".google-dev-email").asInstanceOf[html.Input].value
        submitJson(
          "/auth/google-dev",
          upickle.default.write(GoogleDevLoginRequest(email)),
          googleDevForm.querySelector(".google-dev-error")
        )
      }
    )

    logoutBtn.addEventListener(
      "click",
      (_: dom.Event) => postNoBody(s"${apiBase()}/auth/logout").foreach(_ => showAuth())
    )

    backendSelect.addEventListener("change", (_: dom.Event) => checkAuth())
    checkAuth()
  }

  private def backendSelect: html.Select =
    document.getElementById("backend-select").asInstanceOf[html.Select]

  private def authSection: dom.Element = document.getElementById("auth-section")
  private def appSection: dom.Element = document.getElementById("app-section")
  private def currentUserEmailEl: dom.Element = document.getElementById("current-user-email")
  private def adminNavLink: dom.Element = document.getElementById("admin-nav-link")
  private def loginForm: html.Form = document.getElementById("login-form").asInstanceOf[html.Form]
  private def signupForm: html.Form = document.getElementById("signup-form").asInstanceOf[html.Form]
  private def googleDevForm: html.Form = document.getElementById("google-dev-form").asInstanceOf[html.Form]
  private def logoutBtn: html.Button = document.getElementById("logout-btn").asInstanceOf[html.Button]
  private def loginTab: html.Button = document.querySelector("[data-auth-tab='login']").asInstanceOf[html.Button]
  private def signupTab: html.Button = document.querySelector("[data-auth-tab='signup']").asInstanceOf[html.Button]

  private def apiBase(): String = s"/api/${backendSelect.value}"

  private def container(inputId: String): dom.Element =
    document.querySelector(s"""[data-input-id="$inputId"]""")

  private def wireInput(inputId: String): Unit = {
    val root = container(inputId)
    val input = root.querySelector(".entry-input").asInstanceOf[html.Input]
    val button = root.querySelector(".save-btn").asInstanceOf[html.Button]
    button.addEventListener(
      "click",
      (_: dom.Event) => {
        val value = input.value.trim
        if (value.nonEmpty) {
          save(inputId, value).foreach { _ =>
            input.value = ""
            refreshHistory(inputId)
          }
        }
      }
    )
  }

  private def jsonHeaders(): dom.Headers = {
    val h = new dom.Headers()
    h.append("Content-Type", "application/json")
    h
  }

  private def save(inputId: String, value: String): Future[Unit] = {
    val init = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = jsonHeaders()
      credentials = dom.RequestCredentials.`same-origin`
      body = upickle.default.write(CreateEntryRequest(inputId, value))
    }
    dom.fetch(s"${apiBase()}/entries", init).map { res =>
      if (res.status == 401) showAuth()
    }
  }

  private def refreshHistory(inputId: String): Unit = {
    val init = new dom.RequestInit {
      credentials = dom.RequestCredentials.`same-origin`
    }
    dom
      .fetch(s"${apiBase()}/entries/$inputId", init)
      .flatMap { res =>
        if (res.status == 401) {
          showAuth()
          Future.successful("[]")
        } else res.text()
      }
      .onComplete {
        case Success(json) => render(inputId, upickle.default.read[List[Entry]](json))
        case Failure(_)     => ()
      }
  }

  private def render(inputId: String, entries: List[Entry]): Unit = {
    val list = container(inputId).querySelector(".history-list")
    list.innerHTML = ""
    entries.foreach { e =>
      val li = document.createElement("li")
      li.textContent = e.value
      list.appendChild(li)
    }
  }

  private def showAuth(): Unit = {
    authSection.classList.remove("hidden")
    appSection.classList.add("hidden")
  }

  private def showApp(user: UserView): Unit = {
    authSection.classList.add("hidden")
    appSection.classList.remove("hidden")
    currentUserEmailEl.textContent = user.email
    adminNavLink.classList.toggle("hidden", !user.isAdmin)
    inputIds.foreach(refreshHistory)
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
        case Success((true, text)) => showApp(upickle.default.read[UserView](text))
        case _                      => showAuth()
      }
  }

  private def submitJson(path: String, jsonBody: String, errorEl: dom.Element): Unit = {
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
        case Success((true, text)) => showApp(upickle.default.read[UserView](text))
        case Success((false, text)) =>
          val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
          errorEl.textContent = err.error
        case Failure(_) => errorEl.textContent = "request failed"
      }
  }

  private def postNoBody(url: String): Future[Unit] = {
    val init = new dom.RequestInit {
      method = dom.HttpMethod.POST
      credentials = dom.RequestCredentials.`same-origin`
    }
    dom.fetch(url, init).map(_ => ())
  }
}

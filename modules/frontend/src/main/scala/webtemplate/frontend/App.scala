package webtemplate.frontend

import org.scalajs.dom
import org.scalajs.dom.{document, html}
import typings.universalRouter.universalRouterMod.{Route, RouteContext, RouteParams, Routes, RouterContext}
import webtemplate.shared.{AdminUserView, ApiError, CreateUserRequest, GoogleDevLoginRequest, LoginRequest, SignupRequest, UpdateUserRequest, UserView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits._
import scala.util.{Failure, Success, Try}

/** Single router-driven page for Scala.js. Auth/entries/page2/users markup live in their own HTML
  * fragments under web/public/pages/, fetched and injected on first need via PageLoader;
  * hash-based routes then just toggle which injected section is visible.
  */
object App {

  private val inputIds = List("note-a", "note-b", "note-c")
  private val contentSectionIds =
    List("not-admin-section", "not-found-section", "entries-section", "page2-section", "users-list-section", "users-new-section", "users-detail-section")

  private var currentUser: Option[UserView] = None
  private var currentDetailUserId: Option[String] = None

  // --- element refs ---
  private def authSection: dom.Element = document.getElementById("auth-section")
  private def userMenuEl: dom.Element = document.getElementById("user-menu")
  private def navUsersLinks: List[dom.Element] =
    List("nav-users-link-mobile", "nav-users-link-desktop").flatMap(id => Option(document.getElementById(id)))
  private def authMountEl: dom.Element = document.getElementById("auth-mount")
  private def contentMountEl: dom.Element = document.getElementById("content-mount")
  private def loginForm: html.Form = document.getElementById("login-form").asInstanceOf[html.Form]
  private def signupForm: html.Form = document.getElementById("signup-form").asInstanceOf[html.Form]
  private def googleDevForm: html.Form = document.getElementById("google-dev-form").asInstanceOf[html.Form]
  private def loginTab: html.Button = document.querySelector("[data-auth-tab='login']").asInstanceOf[html.Button]
  private def signupTab: html.Button = document.querySelector("[data-auth-tab='signup']").asInstanceOf[html.Button]

  private def usersTbody: dom.Element = document.getElementById("users-tbody")
  private def usersError: dom.Element = document.getElementById("users-error")
  private def createUserForm: html.Form = document.getElementById("create-user-form").asInstanceOf[html.Form]
  private def editUserForm: html.Form = document.getElementById("edit-user-form").asInstanceOf[html.Form]
  private def deleteUserBtn: html.Button = document.getElementById("delete-user-btn").asInstanceOf[html.Button]

  private def apiBase(): String = s"/api/zio"

  private def jsonHeaders(): dom.Headers = {
    val h = new dom.Headers()
    h.append("Content-Type", "application/json")
    h
  }

  private def updateNavAdminVisibility(): Unit =
    navUsersLinks.foreach(_.classList.toggle("hidden", !currentUser.exists(_.isAdmin)))

  private def showAuth(): Unit = {
    updateNavAdminVisibility()
    authSection.classList.remove("hidden")
    userMenuEl.classList.add("hidden")
    contentSectionIds.foreach(id => Option(document.getElementById(id)).foreach(_.classList.add("hidden")))
  }

  private def showAuthedSection(id: String): Unit = {
    authSection.classList.add("hidden")
    userMenuEl.classList.remove("hidden")
    contentSectionIds.foreach(sid => Option(document.getElementById(sid)).foreach(_.classList.toggle("hidden", sid != id)))
  }

  // --- entries ---

  private def entryContainer(inputId: String): dom.Element =
    document.querySelector(s"""[data-input-id="$inputId"]""")

  private def saveEntry(inputId: String, value: String): scala.concurrent.Future[Unit] = {
    val init = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = jsonHeaders()
      credentials = dom.RequestCredentials.`same-origin`
      body = upickle.default.write(webtemplate.shared.CreateEntryRequest(inputId, value))
    }
    dom.fetch(s"${apiBase()}/entries", init).map { res =>
      if (res.status == 401) showAuth()
    }
  }

  private def refreshHistory(inputId: String): Unit = {
    val init = new dom.RequestInit { credentials = dom.RequestCredentials.`same-origin` }
    dom
      .fetch(s"${apiBase()}/entries/$inputId", init)
      .flatMap { res =>
        if (res.status == 401) {
          showAuth()
          scala.concurrent.Future.successful("[]")
        } else res.text()
      }
      .onComplete {
        case Success(json) => renderEntryHistory(inputId, upickle.default.read[List[webtemplate.shared.Entry]](json))
        case Failure(_)     => ()
      }
  }

  private def renderEntryHistory(inputId: String, entries: List[webtemplate.shared.Entry]): Unit = {
    val list = entryContainer(inputId).querySelector(".history-list")
    list.innerHTML = ""
    entries.foreach { e =>
      val li = document.createElement("li")
      li.textContent = e.value
      list.appendChild(li)
    }
  }

  private def wireEntryInput(inputId: String): Unit = {
    val root = entryContainer(inputId)
    val input = root.querySelector(".entry-input").asInstanceOf[html.Input]
    val button = root.querySelector(".save-btn").asInstanceOf[html.Button]
    button.addEventListener(
      "click",
      (_: dom.Event) => {
        val value = input.value.trim
        if (value.nonEmpty) {
          saveEntry(inputId, value).foreach { _ =>
            input.value = ""
            refreshHistory(inputId)
          }
        }
      }
    )
  }

  // --- users list ---

  private def textCell(text: String): dom.Element = {
    val td = document.createElement("td")
    td.setAttribute("class", "py-2")
    td.textContent = text
    td
  }

  private def renderUsers(users: List[AdminUserView]): Unit = {
    usersTbody.innerHTML = ""
    users.foreach { u =>
      val tr = document.createElement("tr")
      tr.setAttribute("class", "border-t border-base-200")
      tr.appendChild(textCell(u.email))
      tr.appendChild(textCell(if (u.isAdmin) "Yes" else "No"))
      tr.appendChild(textCell(new js.Date(u.createdAt.toDouble).toLocaleDateString()))

      val linkTd = document.createElement("td")
      linkTd.setAttribute("class", "py-2 text-right")
      val link = document.createElement("a").asInstanceOf[html.Anchor]
      link.setAttribute("href", s"#/users/${u.id}")
      link.setAttribute("class", "text-primary hover:underline")
      link.textContent = "View / Edit"
      linkTd.appendChild(link)
      tr.appendChild(linkTd)

      usersTbody.appendChild(tr)
    }
  }

  private def loadUsersList(): Unit = {
    usersError.textContent = ""
    val init = new dom.RequestInit { credentials = dom.RequestCredentials.`same-origin` }
    dom
      .fetch(s"${apiBase()}/admin/users", init)
      .flatMap(res => res.text().map(text => (res.status, text)))
      .onComplete {
        case Success((200, text)) => renderUsers(upickle.default.read[List[AdminUserView]](text))
        case Success((401, _))    => showAuth()
        case Success((403, _))    => showAuthedSection("not-admin-section")
        case Success(_)           => usersError.textContent = "Failed to load users."
        case Failure(_)           => usersError.textContent = "Failed to load users."
      }
  }

  // --- create user ---

  private def resetCreateUserForm(): Unit = {
    createUserForm.asInstanceOf[js.Dynamic].reset()
    createUserForm.querySelector(".create-user-error").textContent = ""
  }

  // --- user detail ---

  private def fillDetailForm(user: AdminUserView): Unit = {
    editUserForm.querySelector(".edit-user-email").asInstanceOf[html.Input].value = user.email
    editUserForm.querySelector(".edit-user-password").asInstanceOf[html.Input].value = ""
    editUserForm.querySelector(".edit-user-is-admin").asInstanceOf[html.Input].checked = user.isAdmin

    val isSelf = currentUser.exists(_.id == user.id)
    editUserForm.querySelector(".edit-user-self-note").classList.toggle("hidden", !isSelf)
    deleteUserBtn.classList.toggle("hidden", isSelf)
    editUserForm.querySelector(".edit-user-is-admin").asInstanceOf[html.Input].disabled = isSelf
  }

  private def loadUserDetail(id: String): Unit = {
    currentDetailUserId = Some(id)
    val init = new dom.RequestInit { credentials = dom.RequestCredentials.`same-origin` }
    dom
      .fetch(s"${apiBase()}/admin/users/$id", init)
      .flatMap(res => res.text().map(text => (res.status, text)))
      .onComplete {
        case Success((200, text)) =>
          fillDetailForm(upickle.default.read[AdminUserView](text))
          showAuthedSection("users-detail-section")
        case Success((401, _)) => showAuth()
        case Success((403, _)) => showAuthedSection("not-admin-section")
        case Success(_)        => showAuthedSection("not-found-section")
        case Failure(_)        => showAuthedSection("not-found-section")
      }
  }

  // --- page2 ---

  val page2SectionId = "page2-section"
  private def select(inputId: String): dom.Element =
    document.querySelector(s"#$inputId")

  private def savePage2Item(name: String, nickname: String): scala.concurrent.Future[Unit] = {
    val init = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = jsonHeaders()
      credentials = dom.RequestCredentials.`same-origin`
      body = upickle.default.write(webtemplate.shared.CreatePage2ItemRequest(name, nickname))
    }
    dom.fetch(s"${apiBase()}/page2items", init).map { res =>
      if (res.status == 401) showAuth()
    }
  }

  private def refreshPage2Items(): Unit = {
    val init = new dom.RequestInit { credentials = dom.RequestCredentials.`same-origin` }
    dom
      .fetch(s"${apiBase()}/page2items", init)
      .flatMap { res =>
        if (res.status == 401) {
          showAuth()
          scala.concurrent.Future.successful("[]")
        } else res.text()
      }
      .onComplete {
        case Success(json) => renderPage2Items(upickle.default.read[List[webtemplate.shared.Page2Item]](json))
        case Failure(_)     => ()
      }
  }

  private def renderPage2Items(page2Items: List[webtemplate.shared.Page2Item]): Unit = {
    val list = select(page2SectionId).querySelector("#page2-names")
    list.innerHTML = ""
    page2Items.foreach { e =>
      val li = document.createElement("li")
      li.textContent = e.name + e.nickname
      list.appendChild(li)
    }
  }

  private def wirePage2(): Unit = {
    val root = select(page2SectionId)
    val nameInput = root.querySelector("#page2-name").asInstanceOf[html.Input]
    val nicknameInput = root.querySelector("#page2-nickname").asInstanceOf[html.Input]
    val button = root.querySelector("#page2-save-btn").asInstanceOf[html.Button]
    button.addEventListener(
      "click",
      (_: dom.Event) => {
        val nameValue = nameInput.value.trim
        val nicknameValue = nicknameInput.value.trim
        if (nameValue.nonEmpty && nicknameValue.nonEmpty) {
          savePage2Item(nameValue, nicknameValue).foreach { _ =>
            nameInput.value = ""
            nicknameInput.value = ""
            refreshPage2Items()
          }
        }
      }
    )
  }


  // --- router ---
  // Hash-based (not History API): each variant is an independent Vite entry, so path-based
  // routing would need a per-prefix server rewrite rule in both dev and prod. Hash routing
  // needs no server config at all.

  private def route(path: String)(action: RouteContext[js.Any, RouterContext] => Unit): Route[js.Any, RouterContext] = {
    val fn: js.Function2[RouteContext[js.Any, RouterContext], RouteParams, js.Any] =
      (ctx, _) => {
        action(ctx)
        true
      }
    js.Dynamic.literal(path = path, action = fn).asInstanceOf[Route[js.Any, RouterContext]]
  }

  private def homeAction(ctx: RouteContext[js.Any, RouterContext]): Unit = {
    PageLoader.ensureLoaded("/pages/entries.html", contentMountEl)(wireEntries()).foreach { _ =>
      showAuthedSection("entries-section")
      inputIds.foreach(refreshHistory)
    }
  }

  private def page2Action(ctx: RouteContext[js.Any, RouterContext]): Unit = {
    PageLoader.ensureLoaded("/pages/page2.html", contentMountEl)(wirePage2()).foreach { _ =>
      showAuthedSection("page2-section")
      refreshPage2Items()
    }
  }

  private val router = new UniversalRouterImpl[js.Any, RouterContext](
    js.Array(
      route("/")(homeAction),
      route("/home")(homeAction),
      route("/page2items")(page2Action),
      route("/users") { _ =>
        PageLoader.ensureLoaded("/pages/users.html", contentMountEl)(wireUsers()).foreach { _ =>
          if (!currentUser.exists(_.isAdmin)) showAuthedSection("not-admin-section")
          else {
            showAuthedSection("users-list-section")
            loadUsersList()
          }
        }
      },
      route("/users/new") { _ =>
        PageLoader.ensureLoaded("/pages/users.html", contentMountEl)(wireUsers()).foreach { _ =>
          if (!currentUser.exists(_.isAdmin)) showAuthedSection("not-admin-section")
          else {
            resetCreateUserForm()
            showAuthedSection("users-new-section")
          }
        }
      },
      route("/users/:id") { ctx =>
        PageLoader.ensureLoaded("/pages/users.html", contentMountEl)(wireUsers()).foreach { _ =>
          if (!currentUser.exists(_.isAdmin)) showAuthedSection("not-admin-section")
          else {
            val id = ctx.params.asInstanceOf[js.Dictionary[String]]("id")
            loadUserDetail(id)
          }
        }
      }
    ).asInstanceOf[Routes[js.Any, RouterContext]]
  )

  private def resolveRoute(): Unit = {
    val raw  = if (dom.window.location.hash.length > 1) dom.window.location.hash.substring(1) else "/"
    val path = if (raw.startsWith("/")) raw else "/" + raw
    router
      .resolve(path)
      .asInstanceOf[js.Promise[js.Any]]
      .toFuture
      .onComplete {
        case Failure(_) => showAuthedSection("not-found-section")
        case _          => ()
      }
  }

  // --- auth ---

  private def setAuthTab(tab: String): Unit = {
    val isLogin = tab == "login"
    loginForm.classList.toggle("hidden", !isLogin)
    signupForm.classList.toggle("hidden", isLogin)
    setTabActive(loginTab, isLogin)
    setTabActive(signupTab, !isLogin)
  }

  private def setTabActive(btn: html.Button, active: Boolean): Unit = {
    btn.classList.toggle("border-primary", active)
    btn.classList.toggle("text-primary", active)
    btn.classList.toggle("border-transparent", !active)
    btn.classList.toggle("text-base-content/60", !active)
  }

  private def afterAuthSuccess(): Unit = {
    updateNavAdminVisibility()
    UserMenu.mount(
      userMenuEl,
      currentUser.get,
      onManageUsers = () => { dom.window.location.hash = "#/users" },
      onLogout = () => {
        val init = new dom.RequestInit {
          method = dom.HttpMethod.POST
          credentials = dom.RequestCredentials.`same-origin`
        }
        dom.fetch(s"${apiBase()}/auth/logout", init).foreach { _ =>
          currentUser = None
          showAuth()
        }
      }
    )
    resolveRoute()
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
        case Success((true, text)) =>
          currentUser = Some(upickle.default.read[UserView](text))
          afterAuthSuccess()
        case Success((false, text)) =>
          val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
          errorEl.textContent = err.error
        case Failure(_) => errorEl.textContent = "request failed"
      }
  }

  private def checkAuth(): Unit = {
    val init = new dom.RequestInit { credentials = dom.RequestCredentials.`same-origin` }
    dom
      .fetch(s"${apiBase()}/auth/me", init)
      .flatMap(res => res.text().map(text => (res.ok, text)))
      .onComplete {
        case Success((true, text)) =>
          currentUser = Some(upickle.default.read[UserView](text))
          afterAuthSuccess()
        case _ =>
          currentUser = None
          showAuth()
      }
  }

  private def wireEntries(): Unit = {
    inputIds.foreach(wireEntryInput)
  }

  private def wireAuth(): Unit = {
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

    googleDevForm.addEventListener(
      "submit",
      (e: dom.Event) => {
        e.preventDefault()
        val email = googleDevForm.querySelector(".google-dev-email").asInstanceOf[html.Input].value
        submitAuthJson(
          "/auth/google-dev",
          upickle.default.write(GoogleDevLoginRequest(email)),
          googleDevForm.querySelector(".google-dev-error")
        )
      }
    )
  }

  private def wireUsers(): Unit = {
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
            case Success((true, _)) => dom.window.location.hash = "#/users"
            case Success((false, text)) =>
              val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
              errorEl.textContent = err.error
            case Failure(_) => errorEl.textContent = "request failed"
          }
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
          .fetch(s"${apiBase()}/admin/users/${currentDetailUserId.getOrElse("")}", init)
          .flatMap(res => res.text().map(text => (res.ok, text)))
          .onComplete {
            case Success((true, text)) => fillDetailForm(upickle.default.read[AdminUserView](text))
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
            .fetch(s"${apiBase()}/admin/users/${currentDetailUserId.getOrElse("")}", init)
            .flatMap(res => res.text().map(text => (res.ok, text)))
            .onComplete {
              case Success((true, _)) => dom.window.location.hash = "#/users"
              case Success((false, text)) =>
                val err = Try(upickle.default.read[ApiError](text)).getOrElse(ApiError("request failed"))
                editUserForm.querySelector(".edit-user-error").textContent = err.error
              case Failure(_) => editUserForm.querySelector(".edit-user-error").textContent = "request failed"
            }
        }
      }
    )
  }

  def setup(): Unit = {
    PageLoader.ensureLoaded("/pages/auth.html", authMountEl)(wireAuth()).onComplete { _ =>
      dom.window.addEventListener(
        "hashchange",
        (_: dom.Event) => {
          if (currentUser.isDefined) resolveRoute()
        }
      )
      checkAuth()
    }
  }
}

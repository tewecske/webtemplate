package webtemplate.frontend

import org.scalajs.dom
import org.scalajs.dom.{document, html}
import webtemplate.shared.{CreateEntryRequest, Entry}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Thenable.Implicits._
import scala.util.{Failure, Success}

object Main {

  private val inputIds = List("note-a", "note-b", "note-c")

  def main(args: Array[String]): Unit = setup()

  private def backendSelect: html.Select =
    document.getElementById("backend-select").asInstanceOf[html.Select]

  private def apiBase(): String = s"/api/${backendSelect.value}"

  private def setup(): Unit = {
    inputIds.foreach(wireInput)
    backendSelect.addEventListener("change", (_: dom.Event) => inputIds.foreach(refreshHistory))
    inputIds.foreach(refreshHistory)
  }

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

  private def save(inputId: String, value: String): Future[Unit] = {
    val reqHeaders = new dom.Headers()
    reqHeaders.append("Content-Type", "application/json")
    val init = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = reqHeaders
      body = upickle.default.write(CreateEntryRequest(inputId, value))
    }
    dom.fetch(s"${apiBase()}/entries", init).map(_ => ())
  }

  private def refreshHistory(inputId: String): Unit = {
    dom
      .fetch(s"${apiBase()}/entries/$inputId")
      .flatMap(_.text())
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
}

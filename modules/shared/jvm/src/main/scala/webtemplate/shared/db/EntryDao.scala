package webtemplate.shared.db

import webtemplate.shared.Entry

trait EntryDao {
  def insert(inputId: String, value: String): Entry
  def history(inputId: String): List[Entry]
  def close(): Unit
}

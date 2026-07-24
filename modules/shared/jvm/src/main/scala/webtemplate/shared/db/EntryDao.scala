package webtemplate.shared.db

import webtemplate.shared.Entry

trait EntryDao {
  def insert(userId: Long, inputId: String, value: String): Entry
  def history(userId: Long, inputId: String): List[Entry]
  def close(): Unit
}

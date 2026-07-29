package webtemplate.shared.db

import webtemplate.shared.Page2Item

trait Page2ItemDao {
  def insert(userId: Long, name: String, nickname: String): Page2Item
  def items(userId: Long): List[Page2Item]
  def close(): Unit
}

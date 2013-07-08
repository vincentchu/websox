package com.vincentchu.websox.example.echo

import com.vincentchu.websox.websocket._
import com.vincentchu.websox.message.StringMessage
import com.twitter.util.Future
import scala.slick.session.Database
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

/**
 * EchoServer
 *
 * This is a simple, modified echo server websocket app. When it receives a
 * message from a connected client, it echos the message back to the client,
 * except the message will be upper cased. If the server receives "closeme"
 * from the connected client, it will close the websocket from the server
 * side.
 */

object Users extends Table[(Int, String, String)]("users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def * = id ~ firstName ~ lastName

  def autoInc = firstName ~ lastName returning id
}

object EchoServer {
  def main(args: Array[String]) {

    println("hi")

    val db = Database.forURL("jdbc:mysql://localhost:3306/foo", user="root", driver="com.mysql.jdbc.Driver")

    db withSession {
//      Users.ddl.drop
//      Users.ddl.create
//      Users.insert(1, "john", "doe")
//      Users.insert(100, "vince", "chu")

      val id = Users.autoInc.insert("foo", "bar")
      println(s"INSERTED: ${id}")

      Query(Users) foreach { case (id, first, last) =>
        println(s"${id} - ${first} ${last}}")

      }
    }

    println("SQL", Users.ddl.createStatements.toSeq)

  }
}



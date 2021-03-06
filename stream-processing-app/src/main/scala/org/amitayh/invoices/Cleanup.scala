package org.amitayh.invoices

import java.util.Properties

import com.github.takezoe.scala.jdbc._
import org.amitayh.invoices.Config.Topics
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}

import scala.collection.JavaConverters._
import scala.util.Try

object Cleanup extends App {

  val admin = {
    val props = new Properties
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, Config.BootstrapServers)
    AdminClient.create(props)
  }

  println(s"Deleting topics...")
  val topicNames = Topics.map(_.name())
  val deleteResult = Try(admin.deleteTopics(topicNames.asJava).all().get())
  println(deleteResult)
  println("-")

  Thread.sleep(100)

  println(s"Creating topics...")
  val createResult = Try(admin.createTopics(Topics.asJava).all().get())
  println(createResult)
  println("-")

  println("Creating tables...")
  val db = Projector.connect()
  db.transaction {
    db.update(sql"DROP TABLE IF EXISTS invoices")
    db.update {
      sql"""
        CREATE TABLE invoices (
          id             TEXT PRIMARY KEY,
          customer_name  TEXT,
          customer_email TEXT,
          status         TEXT,
          issue_date     TEXT,
          due_date       TEXT,
          total          REAL
        );
      """
    }
  }

  admin.close()
  db.close()

}

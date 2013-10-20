package org.pblue.slickpools

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.specs2.mutable.Specification 

import scala.slick.session.Session
import scala.slick.driver.H2Driver.simple._

class WorkerSpec extends Specification with WorkerPoolProvider {

	private val testPool = newConfiguredPool("test")

	object FibTable extends Table[(Int, Int)]("fib") {
		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def value = column[Int]("value")

		def * = id ~ value
		def forInsert = value
	}

	private val data = {
		lazy val fibs: Stream[Int] = 
			0 #:: 1 #:: fibs.zip(fibs.tail).map(n => n._1 + n._2)
		fibs.take(20).toList
	}

	private def setupDb(implicit session: Session) = {
		FibTable.ddl.create
		data.foreach { record =>
			FibTable.forInsert.insert(record)
		}
	}

	"Test data" should {
		
		"should be stored and retrieved" in { 
			val storedFibs =
				testPool.execute { implicit session => 
					setupDb
					Query(FibTable).list
				}

			val result = Await.result(storedFibs, Duration("1 seconds")) 
			val control = data.zipWithIndex.map { case (x, y) => (y + 1, x) }
			
			result === control
		}

	}

}
package sbt

import testing.{Logger => TLogger, Event => TEvent, Status => TStatus}

class TestResultCounter extends TestsListener {

	import java.util.concurrent.atomic.AtomicInteger
	val skippedCount, errorsCount, passedCount, failuresCount = new AtomicInteger

	def getCounts = (skippedCount.get, errorsCount.get, passedCount.get, failuresCount.get)

	def startGroup(name: String) {}
	def testEvent(event: TestEvent): Unit = event.detail.foreach(count)
	def endGroup(name: String, t: Throwable) {}
	def endGroup(name: String, result: TestResult.Value) {}
	protected def count(event: TEvent): Unit =
	{
		val count = event.status match {
			case TStatus.Error => errorsCount
			case TStatus.Success => passedCount
			case TStatus.Failure => failuresCount
			case TStatus.Skipped => skippedCount
		}
		count.incrementAndGet()
	}
	def doInit
	{
		for (count <- List(skippedCount, errorsCount, passedCount, failuresCount)) count.set(0)
	}
	/** called once, at end of test group*/
	def doComplete(finalResult: TestResult.Value): Unit = {}
	override def contentLogger(test: TestDefinition): Option[ContentLogger] = None
}
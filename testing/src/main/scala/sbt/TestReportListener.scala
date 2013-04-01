/* sbt -- Simple Build Tool
 * Copyright 2008, 2009  Steven Blundy, Mark Harrah
 */

package sbt

	import testing.{Logger => TLogger, Event => TEvent, Status => TStatus}

trait TestReportListener
{
	/** called for each class or equivalent grouping */
  def startGroup(name: String)
	/** called for each test method or equivalent */
  def testEvent(event: TestEvent)
	/** called if there was an error during test */
  def endGroup(name: String, t: Throwable)
	/** called if test completed */
  def endGroup(name: String, result: TestResult.Value)
  /** Used by the test framework for logging test results*/
  def contentLogger(test: TestDefinition): Option[ContentLogger] = None
}

trait TestsListener extends TestReportListener
{
	/** called once, at beginning. */
  def doInit()
	/** called once, at end. */
  def doComplete(finalResult: TestResult.Value)
}

abstract class TestEvent extends NotNull
{
	def result: Option[TestResult.Value]
	def detail: Seq[TEvent] = Nil
}
object TestEvent
{
	def apply(events: Seq[TEvent]): TestEvent =
	{
		val overallResult = (TestResult.Passed /: events) { (sum, event) =>
			val status = event.status
			if(sum == TestResult.Error || status == TStatus.Error) TestResult.Error
			else if(sum == TestResult.Failed || status == TStatus.Failure) TestResult.Failed
			else TestResult.Passed
		}
		new TestEvent {
			val result = Some(overallResult)
			override val detail = events
		}
	}
}

object TestLogger
{
	def apply(logger: sbt.Logger, logTest: TestDefinition => sbt.Logger, buffered: Boolean): TestLogger =
		new TestLogger(new TestLogging(wrap(logger), tdef => contentLogger(logTest(tdef), buffered)) )

	def contentLogger(log: sbt.Logger, buffered: Boolean): ContentLogger =
	{
		val blog = new BufferedLogger(FullLogger(log))
		if(buffered) blog.record()
		new ContentLogger(wrap(blog), () => blog.stopQuietly())
	}
	def wrap(logger: sbt.Logger): TLogger =
		new TLogger
		{
			def error(s: String) = log(Level.Error, s)
			def warn(s: String) = log(Level.Warn, s)
			def info(s: String) = log(Level.Info, s)
			def debug(s: String) = log(Level.Debug, s)
			def trace(t: Throwable) = logger.trace(t)
			private def log(level: Level.Value, s: String) = logger.log(level, s)
			def ansiCodesSupported() = logger.ansiCodesSupported
		}
}
final class TestLogging(val global: TLogger, val logTest: TestDefinition => ContentLogger)
final class ContentLogger(val log: TLogger, val flush: () => Unit)
class TestLogger(val logging: TestLogging) extends TestsListener
{
	import logging.{global => log, logTest}
	
	def startGroup(name: String) {}
	def testEvent(event: TestEvent): Unit = {}
	def endGroup(name: String, t: Throwable)
	{
		log.trace(t)
		log.error("Could not run test " + name + ": " + t.toString)
	}
	def endGroup(name: String, result: TestResult.Value) {}
	def doInit {}
	/** called once, at end of test group. */
	def doComplete(finalResult: TestResult.Value): Unit = {}
	override def contentLogger(test: TestDefinition): Option[ContentLogger] = Some(logTest(test))
}

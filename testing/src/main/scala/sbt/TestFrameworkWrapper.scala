package sbt

import org.scalatools.testing.{Framework => OldFramework, 
                               SubclassFingerprint => OldSubclassFingerprint, 
                               AnnotatedFingerprint => OldAnnotatedFingerprint, 
                               Runner => OldRunner, 
                               Runner2 => OldRunner2, 
                               Event => OldEvent, 
                               EventHandler => OldEventHandler, 
                               TestFingerprint, 
                               Logger => OldLogger}
import org.scalasbt.testing.{Framework, 
                             Fingerprint, 
                             SubclassFingerprint, 
                             AnnotatedFingerprint, 
                             Runner, 
                             Event, 
                             EventHandler, 
                             Selector, 
                             TestSelector, 
                             Logger => NewLogger, 
                             Status, 
                             Task}

class SubclassFingerprintWrapper(val oldFingerprint: OldSubclassFingerprint) extends SubclassFingerprint {
  def isModule: Boolean = oldFingerprint.isModule
  def superclassName: String = oldFingerprint.superClassName
}

class AnnotatedFingerprintWrapper(val oldFingerprint: OldAnnotatedFingerprint) extends AnnotatedFingerprint {
  def isModule: Boolean = oldFingerprint.isModule
  def annotationName: String = oldFingerprint.annotationName
}

class TestFingerprintWrapper(val oldFingerprint: TestFingerprint) extends SubclassFingerprint {
  def isModule: Boolean = oldFingerprint.isModule
  def superclassName: String = oldFingerprint.superClassName
}

class EventWrapper(oldEvent: OldEvent, className: String, classIsModule: Boolean) extends Event {
  
  def fullyQualifiedName: String = className

  def isModule: Boolean = classIsModule 

  def selector: Selector = new TestSelector(oldEvent.testName)

  def status: Status = {
    import org.scalatools.testing.Result._
    oldEvent.result match {
      case Success => Status.Success
      case Error => Status.Error
      case Failure => Status.Failure
      case Skipped => Status.Skipped
    }
  }

  def throwable: Throwable = oldEvent.error
  
}

class EventHandlerWrapper(newEventHandler: EventHandler, fullyQualifiedName: String, isModule: Boolean) extends OldEventHandler {
  
  def handle(oldEvent: OldEvent) {
    newEventHandler.handle(new EventWrapper(oldEvent, fullyQualifiedName, isModule))
  }
  
}

class RunnerWrapper(oldFramework: OldFramework, testClassLoader: ClassLoader, args: Array[String]) extends Runner {
  
  def task(fullyQualifiedName: String, fingerprint: Fingerprint, eventHandler: EventHandler, loggers: Array[NewLogger]): Task = 
    new Task {
      def tags: Array[String] = Array.empty  // Old framework does not support tags
      def execute: Array[Task] = {
        val (oldFingerprint, isModule) = 
          fingerprint match {
            case test: TestFingerprintWrapper => (test.oldFingerprint, test.isModule)
          }
        oldFramework.testRunner(testClassLoader, 
                                loggers.map { newLogger => 
                                  new OldLogger {
                                    def ansiCodesSupported = newLogger.ansiCodesSupported 
                                    def error(msg: String) { newLogger.error(msg) }
	                                def warn(msg: String) { newLogger.warn(msg) }
	                                def info(msg: String) { newLogger.info(msg) }
	                                def debug(msg: String) { newLogger.debug(msg) }
	                                def trace(t: Throwable) { newLogger.trace(t) }
                                  }
                                }) match {
            case runner2: OldRunner2 => runner2.run(fullyQualifiedName, oldFingerprint, new EventHandlerWrapper(eventHandler, fullyQualifiedName, isModule), args)
            case runner: OldRunner => runner.run(fullyQualifiedName, oldFingerprint, new EventHandlerWrapper(eventHandler, fullyQualifiedName, isModule), args)
          }
        Array.empty
      }
    }
    
  def task(fullyQualifiedName: String, isModule: Boolean, selectors: Array[Selector], eventHandler: EventHandler, loggers: Array[NewLogger]): Task = 
    throw new UnsupportedOperationException("Old framework does not support selector.")
    
  def done: Boolean = false
}

class FrameworkWrapper(oldFramework: OldFramework) extends Framework {
  
  def name: String = oldFramework.name
  
  def fingerprints: Array[Fingerprint] = oldFramework.tests.map { oldFingerprint => 
    oldFingerprint match {
      case test: TestFingerprint => new TestFingerprintWrapper(test)
      case subClass: OldSubclassFingerprint => new SubclassFingerprintWrapper(subClass)
      case annotated: OldAnnotatedFingerprint => new AnnotatedFingerprintWrapper(annotated)
    }
  }.toArray
  
  def runner(args: Array[String], testClassLoader: ClassLoader): Runner = 
    new RunnerWrapper(oldFramework, testClassLoader, args)
}
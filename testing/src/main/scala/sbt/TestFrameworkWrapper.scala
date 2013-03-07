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

class RunnerWrapper(oldRunner: OldRunner, eventHandler: EventHandler, args: Array[String]) extends Runner {
  
  def task(fullyQualifiedName: String, fingerprint: Fingerprint): Task = 
    new Task {
      def tags: Array[String] = Array.empty  // Old framework does not support tags
      def execute: Array[Task] = {
        val (oldFingerprint, isModule) = 
          fingerprint match {
            case test: TestFingerprintWrapper => (test.oldFingerprint, test.isModule)
          }
        oldRunner.run(fullyQualifiedName, oldFingerprint, new EventHandlerWrapper(eventHandler, fullyQualifiedName, isModule), args)
        Array.empty
      }
    }
    
  def task(fullyQualifiedName: String, isModule: Boolean, selectors: Array[Selector]): Task = 
    throw new UnsupportedOperationException("Old framework does not support selector.")
    
  def done: Boolean = false
}

class Runner2Wrapper(oldRunner: OldRunner2, eventHandler: EventHandler, args: Array[String]) extends Runner {
  
  def task(fullyQualifiedName: String, fingerprint: Fingerprint): Task = 
    new Task {
      def tags: Array[String] = Array.empty  // Old framework does not support tags
      def execute: Array[Task] = {
        val (oldFingerprint, isModule) = 
          fingerprint match {
            case subClass: SubclassFingerprintWrapper => (subClass.oldFingerprint, subClass.isModule)
            case test: TestFingerprintWrapper => (test.oldFingerprint, test.isModule)
            case annotated: AnnotatedFingerprintWrapper => (annotated.oldFingerprint, annotated.isModule)
          }
        oldRunner.run(fullyQualifiedName, oldFingerprint, new EventHandlerWrapper(eventHandler, fullyQualifiedName, isModule), args)
        Array.empty
      }
    }
    
  def task(fullyQualifiedName: String, isModule: Boolean, selectors: Array[Selector]): Task = 
    throw new UnsupportedOperationException("Old framework does not support selector.")
    
  def done: Boolean = false
  
}

class FrameworkWrapper(oldFramework: OldFramework) extends Framework {
  
  def name: String = oldFramework.name
  
  def fingerprints: Array[Fingerprint] = oldFramework.tests.map { oldFingerprint => 
    oldFingerprint match {
      case subClass: OldSubclassFingerprint => new SubclassFingerprintWrapper(subClass)
      case annotated: OldAnnotatedFingerprint => new AnnotatedFingerprintWrapper(annotated)
      case test: TestFingerprint => new TestFingerprintWrapper(test)
    }
  }.toArray
  
  def runner(args: Array[String], testClassLoader: ClassLoader, eventHandler: EventHandler, loggers: Array[NewLogger]): Runner = 
    oldFramework.testRunner(testClassLoader, loggers.map { newLogger => 
      new OldLogger {
        def ansiCodesSupported = newLogger.ansiCodesSupported 
        def error(msg: String) { newLogger.error(msg) }
	    def warn(msg: String) { newLogger.warn(msg) }
	    def info(msg: String) { newLogger.info(msg) }
	    def debug(msg: String) { newLogger.debug(msg) }
	    def trace(t: Throwable) { newLogger.trace(t) }
      }
    }) match {
      case runner2: OldRunner2 => new Runner2Wrapper(runner2, eventHandler, args)
      case runner: OldRunner => new RunnerWrapper(runner, eventHandler, args)
    }
}
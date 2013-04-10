package custom

import java.io._
import org.scalatest._
import events._

class CustomReporter extends ResourcefulReporter {

	private def writeFile(filePath: String, content: String) {
		val file = new File(filePath)
		val writer = new FileWriter(new File(filePath))
		writer.write(content)
		writer.flush()
		writer.close()
	}

	def apply(event: Event) {
		event match {
			case TestSucceeded(_, _, _, _, testName, _, _, _, _, _, _, _, _, _) => writeFile("target/" + testName, testName)
			case _ =>
		}
	}

	def dispose() {
		val file = new File("target/dispose")
		val filePath = 
			if (file.exists)
				"target/dispose2"
			else
				"target/dispose"
		writeFile(filePath, "dispose")
	}
}
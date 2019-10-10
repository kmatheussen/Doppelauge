package no.samordnaopptak.test.TestByAnnotation


/**
  * Do unit testing by annotation. These annotations also function as documentation.
  * 
  * @see Almost all source files in [[https://github.com/kmatheussen/doppelauge/tree/master/app/no/samordnaopptak]] uses TestByAnnotation for unit tests
  * 
  *  @example
  * {{{

// Production code:

import no.samordnaopptak.TestByAnnotation.Test

object StringModifier{
  @Test(code="""
     self.appendUnderscore("a")  =/= "a"
     self.appendUnderscore("a")  === "a_"
     self.appendUnderscore("a2") === "a2_"
  """)
  private def appendUnderscore(input: String): String =
    input+"_"
}


// Test code:

import org.specs2.mutable._
import no.samordnaopptak.TestByAnnotation

class TestByAnnotationSpec extends Specification {
  "TestByAnnotation" should {
    "Test by annotation the 'StringModifier' object" in {
       TestByAnnotation.TestObject(StringModifier)
    }
  }
}
  * }}}
  */
object TestObject{
  import scala.reflect.runtime._
  import scala.tools.reflect.ToolBox

  private val cm = universe.runtimeMirror(getClass.getClassLoader)
  private val tb = cm.mkToolBox()

  private case class AnnotatedTest(method: java.lang.reflect.Method, code: String, methodName: String)

  private def getAnnotatedTests[T](o: T): List[AnnotatedTest] = {
    val class_ = o.getClass()
    //println("class: "+class_)

    val annotatedTests = class_.getDeclaredMethods().toList.filter(method => {
      val annotations = method.getAnnotations()
      annotations.find(_.isInstanceOf[Test]) != None
    }).map(method => {
      val annotations = method.getAnnotations()
      val testAnnotation = annotations.find(_.isInstanceOf[Test]).get
      val code = testAnnotation.asInstanceOf[Test].code()
      AnnotatedTest(method, code, method.getName())
    })

    //println("annotatedTests: "+annotatedTests)

    annotatedTests
  }

  private def isLegalVarnameFirstChar(c: Char) =
    c.isLetter || c=='_'

  private def isLegalVarnameChar(c: Char) =
    isLegalVarnameFirstChar(c) || c.isDigit

  @Test(code="""
    self.splitSelfCallFunctionNameAndRest("ai") === ("ai", "")
    self.splitSelfCallFunctionNameAndRest("ai 50") === ("ai", " 50")
    self.splitSelfCallFunctionNameAndRest("ai b ") === ("ai", " b ")
    self.splitSelfCallFunctionNameAndRest("ai+b")  === ("ai", "+b")
    self.splitSelfCallFunctionNameAndRest("ai5b")  === ("ai5b", "")
    self.splitSelfCallFunctionNameAndRest("ai() hello hello hello")   === ("ai", "() hello hello hello")
    self.splitSelfCallFunctionNameAndRest("ai ()")  === ("ai", " ()")
    self.splitSelfCallFunctionNameAndRest("ai  \n\t()")  === ("ai", "  \n\t()")
  """)
  private def splitSelfCallFunctionNameAndRest(code: String): (String, String) = {

    def inner(varname: String, code: String): (String, String) =
      if (code=="")
        (varname, "")
      else if (isLegalVarnameChar(code(0)))
        inner(varname+code.take(1), code.drop(1))
      else
        (varname, code)

    inner(code.take(1), code.drop(1))
  }

  @Test(code="""
    self.splitArgumentsAndRest("")    === ("", "")
    self.splitArgumentsAndRest(" ")   === ("", " ")
    self.splitArgumentsAndRest("  \n\t() rest")  === ("", " rest")
    self.splitArgumentsAndRest("  \n\t(2\n) rest")  === ("2\n", " rest")
    self.splitArgumentsAndRest(" 50") === ("", " 50")
    self.splitArgumentsAndRest(" b")  === ("", " b")
    self.splitArgumentsAndRest("+b")  === ("", "+b")

    self.splitArgumentsAndRest("() rest")     === ("", " rest")
    self.splitArgumentsAndRest(" ()rest")    === ("", "rest")
    self.splitArgumentsAndRest(" 5 ()")  === ("", " 5 ()")
    self.splitArgumentsAndRest(" 5 (2)") === ("", " 5 (2)")

    self.splitArgumentsAndRest("(2) rest")          === ("2", " rest")
    self.splitArgumentsAndRest(" (2) rest")         === ("2", " rest")
    self.splitArgumentsAndRest(" (2 ,3) rest")      === ("2 ,3", " rest")
    self.splitArgumentsAndRest(" (2 , (43)) rest")  === ("2 , (43)", " rest")
    self.splitArgumentsAndRest(" (2) (43) rest")    === ("2", " (43) rest")
    self.splitArgumentsAndRest(" (2, ((43))) rest") === ("2, ((43))", " rest")
  """)
  private def splitArgumentsAndRest(code: String): (String, String) = {
    val allCode = code

    def inner(parBalance: Int, arguments: String, code: String): (String, String) = {
      if (code=="") {

        if (parBalance>0)
          throw new Exception("Missing right parenthesis in \"" + allCode + "\"")

        (arguments, code)

      } else if (code(0)==')') {

        if (parBalance==0)
          throw new Exception("Superfluous left parenthesis in \"" + allCode + "\"")

        if (parBalance==1)
          (arguments, code.drop(1))
        else
          inner(parBalance-1, arguments+")", code.drop(1))

      } else if (code(0)=='(') {

        inner(parBalance+1, arguments+code.take(1), code.drop(1))

      } else {

        inner(parBalance, arguments+code.take(1), code.drop(1))

      }
    }

    if (code.trim=="")
      ("", code)

    else if (code.trim.take(1)=="(")
      inner(1, "", code.trim.drop(1))

    else
      ("", code)
  }

  @Test(code="""
    self.splitSelfCallCode("ai")    === ("ai", "", "")
    self.splitSelfCallCode("ai ")   === ("ai", "", " ")
    self.splitSelfCallCode("ai 50") === ("ai", "", " 50")
    self.splitSelfCallCode("ai b")  === ("ai", "", " b")
    self.splitSelfCallCode("ai+b")  === ("ai", "", "+b")
    self.splitSelfCallCode("aicb")  === ("aicb", "", "")

    self.splitSelfCallCode("ai() rest")     === ("ai", "", " rest")
    self.splitSelfCallCode("ai ()rest")    === ("ai", "", "rest")
    self.splitSelfCallCode("ai 5 ()")  === ("ai", "", " 5 ()")
    self.splitSelfCallCode("ai 5 (2)") === ("ai", "", " 5 (2)")

    self.splitSelfCallCode("ai(2) rest")          === ("ai", "2", " rest")
    self.splitSelfCallCode("ai (2) rest")         === ("ai", "2", " rest")
    self.splitSelfCallCode("ai (2 ,3) rest")      === ("ai", "2 ,3", " rest")
    self.splitSelfCallCode("ai (2 , (43)) rest")  === ("ai", "2 , (43)", " rest")
    self.splitSelfCallCode("ai (2) (43) rest")    === ("ai", "2", " (43) rest")
    self.splitSelfCallCode("ai (2, ((43))) rest") === ("ai", "2, ((43))", " rest")
  """)
  private def splitSelfCallCode(code: String): (String, String, String) = {
    val (methodName, argumentsAndRest) = splitSelfCallFunctionNameAndRest(code)
    val (arguments, rest) = splitArgumentsAndRest(argumentsAndRest)
    (methodName, arguments, rest)
  }

  // Sometimes the scala method names contains the full package path, for some reason.
  private def getMethodName(method: java.lang.reflect.Method) = {
    val name = method.getName
    val dollardollarindex = name.indexOf("$$")
    if (dollardollarindex == -1)
      name
    else
      name.drop(dollardollarindex+2)
  }

  /**
    * privateMethodCaller is copied from https://gist.github.com/jorgeortiz85/908035 (slightly modified)
    * @note Used internally. The function can not be private since calls to it are generated by the [[apply]] method.
    */
  def privateMethodCaller(x: AnyRef, methodName: String, _args: Any*): Any = {
    val args = _args.map(_.asInstanceOf[AnyRef])
    def _parents: Stream[Class[_]] = Stream(x.getClass) #::: _parents.map(_.getSuperclass)
    val parents = _parents.takeWhile(_ != null).toList
    val methods = parents.flatMap(_.getDeclaredMethods)
    /*
    println("methods names: ")
    methods.foreach(m => println(getMethodName(m)))
     */
    val method = methods.find(getMethodName(_) == methodName).getOrElse(throw new IllegalArgumentException("Method " + methodName + " not found"))
    method.setAccessible(true)
    method.invoke(x, args : _*)
  }

  @Test(code="""
    self.nextSelfCallPos("", "salf") === -1
    self.nextSelfCallPos("salf.ai", "salf") === 0
    self.nextSelfCallPos("+salf.ai", "salf") === 1
    self.nextSelfCallPos("salf.ai()", "salf") === 0
    self.nextSelfCallPos("  salf.ai()", "salf") === 2
    self.nextSelfCallPos("asalf.ai()", "salf") === -1
    self.nextSelfCallPos("_salf.ai", "salf") === -1
    self.nextSelfCallPos("a_salf.ai", "salf") === -1
    self.nextSelfCallPos("a9salf.ai", "salf") === -1
    self.nextSelfCallPos("bbbasalf.ai()", "salf") === -1
    self.nextSelfCallPos("val TestByAnnotation___comp_value_a = salf.argsStringIsEmpty(\"\")", "salf") === 38
  """)
  private def nextSelfCallPos(code: String, selfName: String): Int = {
    def inner(pos: Int, mayWork: Boolean, code: String): Int =
      if (code=="")
        -1

      else if (mayWork==false) {
        if (isLegalVarnameChar(code(0)))
          inner(pos+1, false, code.drop(1))
        else
          inner(pos+1, true, code.drop(1))
      }

      else if (code.startsWith(selfName+"."))
        pos

      else if (isLegalVarnameFirstChar(code(0)))
        inner(pos+1, false, code.drop(1))

      else
        inner(pos+1, true, code.drop(1))

    inner(0, true, code)
  }

  @Test(code="""
    self.argsStringIsEmpty("") === true
    self.argsStringIsEmpty(" ") === true
    self.argsStringIsEmpty("a") === false
    self.argsStringIsEmpty(" a ") === false
    self.argsStringIsEmpty(" a,b ") === false
  """)
  private def argsStringIsEmpty(args: String) =
    args.trim == ""


    // (probably not the best example on how simple annotation tests are...)
  @Test(code="""
    self.convertToUsePrivateMethodCaller("", "salv") === ""
    self.convertToUsePrivateMethodCaller("a", "salv") === "a"
    self.convertToUsePrivateMethodCaller("a\nb\nc", "salv") === "a\nb\nc"
    self.convertToUsePrivateMethodCaller("salv.ai()", "salv") === "no.samordnaopptak.test.TestByAnnotation.TestObject.privateMethodCaller(instance, \"ai\")"
    self.convertToUsePrivateMethodCaller("salv.ai(a)", "salv") === "no.samordnaopptak.test.TestByAnnotation.TestObject.privateMethodCaller(instance, \"ai\", a)"
    self.convertToUsePrivateMethodCaller("salv.ai(a, 2, \"b\")", "salv") === "no.samordnaopptak.test.TestByAnnotation.TestObject.privateMethodCaller(instance, \"ai\", a, 2, \"b\")"
    self.convertToUsePrivateMethodCaller("  salv.ai(a, 2, \"b\") == 50", "salv") === "  no.samordnaopptak.test.TestByAnnotation.TestObject.privateMethodCaller(instance, \"ai\", a, 2, \"b\") == 50"
    self.convertToUsePrivateMethodCaller("  salv.ai(a, 2, \"b\") + salv.b() == 50", "salv") === "  no.samordnaopptak.test.TestByAnnotation.TestObject.privateMethodCaller(instance, \"ai\", a, 2, \"b\") + no.samordnaopptak.test.TestByAnnotation.TestObject.privateMethodCaller(instance, \"b\") == 50"
  """)
  private def convertToUsePrivateMethodCaller(code: String, selfName: String = "self"): String = {
    val selfStart = nextSelfCallPos(code, selfName)
    if (selfStart == -1)
      return code

    val methodStartPos = selfStart + selfName.size + 1

    val codeBefore = code.take(selfStart)

    val (methodName,arguments,codeAfter) = splitSelfCallCode(code.drop(methodStartPos))

    /*
     println("codeBefore: "+codeBefore)
     println("methodName: "+methodName)
     println("arguments: "+arguments)
     println("codeAfter: "+codeAfter)
     println("codeAfter pos: "+nextSelfCallPos(codeAfter, selfName))
     println("codeAfter converted: "+convertToUsePrivateMethodCaller(codeAfter, selfName))
     */

    codeBefore +
    "no.samordnaopptak.test.TestByAnnotation.TestObject.privateMethodCaller(" + (
      "instance, " +
      "\"" + methodName + "\"" +
      (if (argsStringIsEmpty(arguments))
        ""
      else
        ", " + arguments
      )
    ) +
    ")" +
    convertToUsePrivateMethodCaller(codeAfter, selfName)
  }


  private def createExceptionString(linenum: Int, methodName: String, message: String) =
    s"""Failed input/Output test #$linenum for method '$methodName': $message"""

  private def splitLine(linenum: Int, methodName: String, line: String) =
    if (line.contains("===")) {
      val splitted = line.split("===")
      ("==", splitted(0).trim, splitted(1).trim)
    } else if (line.contains("=/=")) {
      val splitted = line.split("=/=")
      ("!=", splitted(0).trim, splitted(1).trim)
    } else
      throw new Exception(createExceptionString(linenum, methodName, s""""$line" does not contain "===" or "=/=")"""))

  private def createAssertionCodeLine(linenum: Int, methodName: String, line: String, comparitor: String, a: String, b: String) = {
    val exceptionString =
      createExceptionString(
        linenum,
        methodName,
        s"""Assertion failed for the assertion " $line \".\n\n""" +
          "This is not correct: " +
          "\"\"\" + TestByAnnotation___comp_value_a + \"\"\" " +
          comparitor +
          " \"\"\" + TestByAnnotation___comp_value_b + \"\"\" \n\n "
      )

    s"""
    {
      val TestByAnnotation___comp_value_a = $a
      val TestByAnnotation___comp_value_b = $b
      if (!(TestByAnnotation___comp_value_a $comparitor TestByAnnotation___comp_value_b)){
         println(Console.RED+"TEST FAILED: " + Console.RESET + \"\"\" """ + exceptionString + s""" \"\"\" )
         false
      } else
         true
    }
    """
  }

  private def printTest(linenum: Int, methodName: String, line: String, comparitor: String, a: String, b: String) =
    println(Console.GREEN+s"TestByAnnotitation '$methodName'/#${linenum}: "+Console.RESET + a + Console.GREEN + " " + (if (comparitor=="==") "===" else "=/=") + Console.RESET + " " + b)

  /**
    * Returns a string containing the stack trace.
    * @note Used internally. The function can not be private since calls to it are generated by the [[apply]] method.
    */
  def getStackTraceString(t: Throwable) = {
    val sw = new java.io.StringWriter()
    t.printStackTrace(new java.io.PrintWriter(sw))
    sw.toString
  }

  import scala.reflect.runtime.universe._

  private def evalCode[T: TypeTag](o: T, methodName: String, code: String) = {
    def evalLine(linenum: Int, line: String) = {

      val (comparitor, a, b) = splitLine(linenum, methodName, line)
      printTest(linenum, methodName, line, comparitor, a, b)

      val assertionCodeLine = convertToUsePrivateMethodCaller(createAssertionCodeLine(linenum, methodName, line, comparitor, a, b))

      //println("assertionCodeLine: "+assertionCodeLine)

      val tree = try{
        tb.parse(assertionCodeLine)
      } catch {
        case e: Throwable => throw new Exception(createExceptionString(linenum, methodName, s""""$line" failed while parsing "$assertionCodeLine": ${e.getMessage()}""""))
      }
      /*
      println()
      println("a: "+a)
      println("assertionCodeLine: "+assertionCodeLine)
      println("tree: "+tree)
      println()
       */

      val result =
        try{
          tb.eval(
            Block(
              List(
                ValDef(Modifiers(), TermName("instance"), TypeTree(), reify{o.asInstanceOf[T]}.tree)
              ),
              //reify{import Gakk._}.tree,
              tree
            )
          )
        } catch {
          case e: Throwable => throw new Exception(Console.RED+"Test #"+linenum+" failed.\nbacktrace: "+getStackTraceString(e))
        }

      if (!result.asInstanceOf[Boolean])
        throw new Exception
    }

    def evalLines(lineNum: Int, codeLines: List[String]): Unit =
      if (codeLines.nonEmpty) {
        val trimmedLine = codeLines.head.trim
        if (trimmedLine != "" && !trimmedLine.startsWith("//"))
          evalLine(lineNum, codeLines.head.trim)

        evalLines(lineNum+1, codeLines.tail)
      }

    evalLines(0, code.split("\n").toList)
  }

  def apply[T: TypeTag](o: T): Boolean = {
    val annotatedTests = getAnnotatedTests[T](o)

    annotatedTests.foreach(annotatedTest => {
      evalCode(o, annotatedTest.methodName, annotatedTest.code)
    })

    true
  }
}


TestByAnnotation
================



Problem
-------

Unit tests often become too detailed, and they often break too easily.


Solution
--------

By putting tests directly above the method themselves, it becomes
natural to update the tests whenever the code is changed. The tests
also start to serve as documentation for the methods.


Example
-------

```scala
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
```


Workaround for limitations in TestByAnnotation
-----------------------------------------------
* Testing the content of a private class:

```scala
object Math{
  @Test(code="""
    self.testAdder() === true
  """)
  private def testAdder() = {
    implicit class Hepp[T](val a: T) {
      def ===(b: T) = if (a!=b) throw new Exception(s"$a != $b") else true
    }

    Adder(5).one === 6
    Adder(5).two === 7
  }

  private case class Adder(value: Int){
    def one = value+1
    def two = value+2
  }
}
```


Scala-doc
---------
http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.test.TestByAnnotation.TestObject$


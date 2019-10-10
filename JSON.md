
J
===

* Similar to Play Framework's 'Json' object, but quicker to use for testing and parsing.
* Values can easily be converted between the "J" format (called 'JValue') and the play Framework format (called 'JsValue').
* J is safer than Play Framework's 'Json' object since it doesn't allow key clash when creating and merging objects.
* J guarantees to maintain object field order. Play Framework's 'JsObject' type does not.
* J keeps track of the fields which are read out of a json object, and can validate automatically that it contained what you expected.
* JValue has some support dynamic typing. It's not always the simplest option to convert back and forth between json and native types
  (for instance ```J(5) + 9 === J(14)```). See also [JsonMatcher](JSON.md#jsonmatcher) and [JsonChanger](JSON.md#jsonchanger).
* Scaladoc: [J](http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.json.J$)


```scala
val json = J.obj(
  "a" -> 1,
  "b" -> J.arr(2,3)
)

json("a").asInt === 1
json("b")(0).asInt === 2
json("b")(1).asInt === 3

val json2 = J.parse("[4,5,6]")
json2(0).asInt === 4
json2(1).asInt === 5
json2(2) === JNumber(6)
```


JsonMatcher
===========

* Patternmatcher for json values.
* Several custom matchers.
* When matching fails, the error messages contain description with full path.
* ScalaDoc: [JsonMatcher](http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.json.JsonMatcher$)


```scala
package test

import org.specs2.mutable._

import no.samordnaopptak.json.J
import no.samordnaopptak.json.JsonMatcher._

class JsonMatcherSpec extends Specification {
  "Json matcher" should {
    "match object with object" in {
      matchJson(
        J.obj(
          "a" -> J.obj(
            "c" -> ___anyString
            "e" -> ___anyArray
          )
        ),
        J.obj(
          "a" -> J.obj(
            "d" -> "astring",
            "e" -> J.arr(2,3,4,5)
          )
        )
      )
    }
  }
}
```


JsonChanger
===========
Change the content of a json value in a safe and descriptive manner.
  
Firstly, the name is misleading. JsonChanger is not *changing* (i.e mutating) the values. It returns a new value.

The main function, [JsonChanger.apply](http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.json.JsonChanger$@apply%28json_value:Any,changer:Any,path:String,allow_mismatched_types:Boolean%29:no.samordnaopptak.json.JValue) takes two arguments: **json_value** and **changer**.
The **changer** variable has similarities to the **matcher** variable used in [JsonMatcher](http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.json.JsonMatcher$), but while
JsonMatcher only returns **true** or **false**, JsonChanger returns a new Json value.
   
The most important similarity between JsonChanger and JsonMatcher is that they both do pattern matching.
When changing a json value with JsonChanger, it also pattern matches the **json_value** against the **changer**. This pattern matching
should make bugs appear earlier than they would have been othervice.
(You might argue that it would be better to first validate the json against a schema, but this way you get validation for free,
plus that the validation schema maintains itself automatically.)
   
The pattern matcher in JsonChanger checks that:

  1. A json value doesn't change type (unless we tell it to)
  2. We don't add or remove fields to objects (unless we tell it to)
  3. We don't add or remove values to arrays (unless we tell it to)

When matching fails, the error messages contain description with full path.

There are several custom changers such as *___identity*, *Replace*, *Func*, *Map*, *MapChanger*, etc. See [scala doc](http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.json.JsonChanger$) for examples.
Custom changers can be also be created from the outside by implementing the *Changer* trait.

ScalaDoc: [JsonChanger](http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.json.JsonChanger$)

Example:

```scala
import org.specs2.mutable._

import no.samordnaopptak.json.J
import no.samordnaopptak.json.JsonChanger
import no.samordnaopptak.json.JsonChanger._

object {
  val json = J.obj(
    "aaa" -> J.obj(
      "a1" -> 60,
      "a2" -> 70
    ),
    "bbb" -> 80
  )

  // Add 5 to the value of "aaa.a1" by using JsonChanger:
  JsonChanger(
    json,
    J.obj(
      "aaa" -> J.obj(
         "a1" -> JsonChanger.Func.number.number(_ + 5),
         "a2" -> ___identity.number
      ),
      "bbb" -> ___identity.number
    )
  )

  // Add 5 to the value of "aaa.a1" manually:
  json - "aaa" ++ J.obj(
    "aaa" -> (
      json("aaa") - "a1" ++ J.obj(
        "a1" -> (json("aaa")("a1") + 5)
      )
    )
  )
}
```

In the above example, not only is the JsonChanger code simpler to read, but the code is also safer,
and you get error messages with full path if the json value has incompatible data.


JsonDiff
========
Simple Json Diff algorithm. Can be used to display the difference between two json values. An example function
for creating HTML table is included.
* ScalaDoc: [JsonDiff](http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.json.JsonDiff$)


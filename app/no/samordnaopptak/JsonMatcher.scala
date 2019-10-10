package no.samordnaopptak.json

import play.api.libs.json._



/** 
  *  Patternmatcher for json values. Use the [[matchJson]] method to match json values.
  * 
  *  @example {{{
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
  *  }}}
  * 
  *  @note The matcher and json values do not have to be [[JObject]] valuess (the result of calling 'J.obj'). [[matchJson]] understands a number of different types such as [[JValue]], JsValue (The play framework version of [[JValue]]), Map, String, Boolean, etc.
  * 
  * @note JsonMatcher provides several custom matchers such as [[___anyString]], [[___anyNumber]], [[RegExp]], [[Custom]], etc. All of these matchers have examples.
  * 
  * @see [[https://github.com/kmatheussen/doppelauge/blob/master/test/JsonMatcherSpec.scala]] for more examples
  */
object JsonMatcher{

  /** 
    * Prints out more info during json matching to help debugging
    */
  var very_verbose = false

  /** 
    * Prints out info during json matching to help debugging.
    */
  var verbose = very_verbose || false

  class JsonMatcherException(message: String, val path: String) extends Exception(message)

  private val ___allowOtherJsonsKey = "________allowothers________"
  private val ___ignoreOrderJsonKey = "________ignoreOrder________"
  private val ___maybeFieldPrefix = "________maybe_______"

  /** Checks that the number of elements in an object or array matches.
    * @example
    * {{{
      matchJson(
        Json.obj(___numElements -> 3),
        Json.obj("a" -> 1, "b" -> 2, "c" -> 3)
      )
      matchJson(
        Json.obj("a" -> 1, ___numElements -> 3),
        Json.obj("a" -> 1, "b" -> 2, "c" -> 3)
      )

      matchJson(
        Json.arr(___numElements, 3),
        Json.arr("a","b","c")
      )
      matchJson(
        Json.arr("a", ___numElements, 3),
        Json.arr("a","b","c")
      )
    * }}}
    */
  val ___numElements = "________numFields________"

  /** Matches any string.
    * @example
    * {{{
    * matchJson(
    *   J.obj("a" -> ___anyString),
    *   Map("a" -> "b")
    * )
    * matchJson(
    *   J.obj("a" -> ___anyString),
    *   J.obj("a" -> 5)
    * ) should throwA[JsonMatcherException] 
    * }}}
    */
  val ___anyString  = JsString("________anyString________")

  /** Matches any number.
    * @example
    * {{{
      matchJson(
        J.obj("a" -> ___anyNumber),
        J.obj("a" -> 5)
      )
      matchJson(
        J.obj("a" -> ___anyNumber),
        J.obj("a" -> "b")
      ) should throwA[JsonMatcherException]
    * }}}
    */
  val ___anyNumber  = JsString("________anyNumber________")

  /** Matches any object.
    * @example
    * {{{
      matchJson(
        J.obj("a" -> ___anyObject),
        J.obj("a" -> J.obj())
      )
      matchJson(
        J.obj("a" -> ___anyObject),
        J.obj("a" -> 5)
      ) should throwA[JsonMatcherException] 
    * }}}
    */
  val ___anyObject  = JsString("________anyObject________")


  /** Matches any array.
    * @example
    * {{{
      matchJson(
        J.obj("a" -> ___anyArray),
        J.obj("a" -> J.arr())
      )
      matchJson(
        J.obj("a" -> ___anyArray),
        J.obj("a" -> 5)
      ) should throwA[JsonMatcherException] 
    * }}}
    */
  val ___anyArray   = JsString("________anyArray_________")


  /** Matches any boolean.
    * @example
    * {{{
      matchJson(
        J.obj("a" -> ___anyBoolean),
        J.obj("a" -> true)
      )
      matchJson(
        J.obj("a" -> ___anyBoolean),
        J.obj("a" -> 5)
      ) should throwA[JsonMatcherException] 

    * }}}
    */
  val ___anyBoolean = JsString("________anyBoolean_______")


  class Custom(val func: JValue => Boolean, val name: String = "") extends JsString("Custom: "+name) with JValue {
    override def pp() = "Custom: "+name
    override def asJsValue = this
  }

  /** 
    * Creates a custom matcher.
    * @example
    * {{{
      matchJson(
        Custom(_ => true),
        JsNull
      )
      matchJson(
        Custom(_ => false),
        JsNull
      ) should throwA[JsonMatcherException]

      matchJson(
        Custom(_.asNumber > 0),
        50
      )
      matchJson(
        Custom(_.asNumber > 0),
        0
      ) should throwA[JsonMatcherException]
    * }}}
    * @param name can be set to help identify what went wrong if matching fails
    */
  def Custom(func: JValue => Boolean, name: String = "") = new Custom(func, name)

  class RegExp(val pattern: String) extends JsString("RegExp: "+pattern) with JValue {
    val regexp = new scala.util.matching.Regex(pattern)

    def check(string: String): Boolean =
      regexp.findFirstIn(string).isDefined

    override def asJsValue = this
  }

  /**
    * Creates a RegExp matcher.
    * @example
    * {{{
      matchJson(
        J.obj("a" -> RegExp("b")),
        J.obj("a" -> "cbh")
      )

      matchJson(
        J.obj("a" -> RegExp("^b")),
        J.obj("a" -> "bh")
      )

      matchJson(
        J.obj("a" -> RegExp("b")),
        J.obj("a" -> "c")
      ) should throwA[JsonMatcherException] 
    * }}}
    */
  def RegExp(pattern: String) = new RegExp(pattern)


  class Or(orMatchers: Any*) extends JsString("Or: "+orMatchers.map(_.toString).mkString(",")) with JValue{
    val matchers = orMatchers.map(J(_))
    override def asJsValue = this
  }

  /**
    * Creates an Or matcher.
    * @example
    * {{{
      matchJson(
        Or("a", 50),
        "a"
      )
      matchJson(
        Or("a", 50),
        50
      )
      matchJson(
        Or("a", 50),
        false
      ) should throwA[JsonMatcherException]
    * }}}
    */
  def Or(orMatchers: Any*) = new Or(orMatchers :_*)


  class And(andMatchers: Any*) extends JsString("And: "+andMatchers.map(_.toString).mkString(",")) with JValue{
    val matchers = andMatchers.map(J(_))
    override def asJsValue = this
  }

  /**
    * Creates an And matcher.
    * @example
     {{{
      matchJson(
        And(___anyNumber, 50),
        JsNumber(50)
      )
      matchJson(
        And(
          ___anyNumber,
          Custom(_.asNumber > 0),
          Custom(_.asNumber < 100)
        ),
        JsNumber(50)
      )
    * }}}
    */
  def And(andMatchers: Any*) = new And(andMatchers :_*)


  class Maybe(maybeMatcher: Any) extends JsString("Maybe: "+maybeMatcher.toString) with JValue {
    val matcher = Or(
      JsNull,
      maybeMatcher
    )
    override def asJsValue = this
  }


  /**
    * Creates a Maybe matcher.
    * @example
    * {{{
      matchJson(
        J.obj(
          "a" -> Maybe(___anyNumber)
        ),
        J.obj()
      )

      matchJson(
        J.obj(
          "a" -> Maybe(___anyNumber)
        ),
        J.obj(
          "a" -> JNull
        )
      )

      matchJson(
        J.obj(
          "a" -> Maybe(___anyNumber)
        ),
        J.obj(
          "a" -> None
        )
      )

      matchJson(
        J.obj(
          "a" -> Maybe(___anyNumber)
        ),
        J.obj(
          "a" -> 50
        )
      )
      matchJson(
        J.obj(
          "a" -> Maybe(2)
        ),
        J.obj(
          "a" -> 1
        )
      ) must throwA[JsonMatcherException]
    * }}}
    */
  def Maybe(maybeMatcher: Any) = new Maybe(maybeMatcher)

  /**
    * Allow other values when matching arrays.
    * @example
    * {{{
      matchJson(
        Json.arr("a", "b", ___allowOtherValues),
        Json.arr("a", "b", "c")
      )
      matchJson(
        Json.arr("a", "b"),
        Json.arr("a", "b", "c")
      ) should throwA[JsonMatcherException]
    * }}}
    */
  val ___allowOtherValues: JsString = JsString(___allowOtherJsonsKey)

  /**
    * Ignore order when matching arrays.
    * @example
    * {{{
      matchJson(
        Json.arr(3,5,2, ___ignoreOrder),
        Json.arr(2,3,5)
      )
      matchJson(
        Json.arr(3,5,2),
        Json.arr(2,3,5)
      ) should throwA[JsonMatcherException]
    * }}}
    */
  val ___ignoreOrder: JsString = JsString(___ignoreOrderJsonKey)

  /**
    * Allow other fields when matching objects.
    * @example
    * {{{
      matchJson(
        Json.obj("a" -> 1, "b" -> 2, ___allowOtherFields),
        Json.obj("a" -> 1, "b" -> 2, "c" -> 3)
      )
      matchJson(
        Json.obj("a" -> 1, "b" -> 2),
        Json.obj("a" -> 1, "b" -> 2, "c" -> 3)
      ) should throwA[JsonMatcherException]
    * }}}
    */
  val ___allowOtherFields: (String,Json.JsValueWrapper) = ___allowOtherJsonsKey -> Json.toJsFieldJsValueWrapper(___allowOtherJsonsKey)


  private def matchJsonFailed(message: String, throwException: Boolean, path: String): Boolean =
    if (throwException)
      throw new JsonMatcherException(message+"\n\npath: "+path+"\n\n *** Set JsonMatcher.verbose (or JsonMatcher.very_verbose) to true to get more details. ***\n\n ", path)
//        throw new Exception(message+"\n\npath: "+path)
    else
      false

  private def getWantedNumArrayElements(matcher: JArray): Int =
    matcher.value.dropWhile(_ != JString(___numElements)).tail.head.asInt

  private def getWantedNumObjectElements(matcher: Map[String, JValue]): Int =
    matcher(___numElements).asInt

  // Seq(1, ___numElements, 2, __ignoreOrder, 3) -> Seq(1,3)
  private def getValuesWithoutNumsAndIgnores(values: Seq[JValue]): Seq[JValue] =
    if (values.isEmpty)
      values
    else if (values.head.asJsValue==JsString(___numElements))
      values.tail.tail
    else if (values.head.asJsValue==___ignoreOrder)
      getValuesWithoutNumsAndIgnores(values.tail)
    else
      Seq(values.head) ++ getValuesWithoutNumsAndIgnores(values.tail)

  private def matchOrderedJsonArrays(json: JArray, allowOthers: Boolean, matchers: Seq[JValue], pos: Int, values: Seq[JValue], throwException: Boolean, path: String, errorMessage: Option[String]): Boolean = {
    assert(errorMessage.isEmpty || !allowOthers)

    if (matchers.isEmpty && values.nonEmpty && errorMessage.isDefined)
      matchJsonFailed(errorMessage.get + s"First non-matching element found at position $pos, with value ${values.head.pp()}. The matcher doesn't have that many elements.", throwException, path)

    else if (matchers.isEmpty)
      true

    else if (matchers.head.asJsValue == ___allowOtherValues)
      matchOrderedJsonArrays(json, allowOthers, matchers.tail, pos+1, values, throwException, path, errorMessage)

    else if (values.isEmpty)
      matchJsonFailed(s"${matchers.head.pp()} is not present in the array ${json.pp()}", throwException, path)

    else if (allowOthers) {
      val matches = matchJson(matchers.head, values.head, throwException = false, ignoreArrayOrder = false, path+"("+pos+")")
      if (!matches)
        matchOrderedJsonArrays(json, allowOthers, matchers, pos, values.tail, throwException, path, errorMessage)
      else
        matchOrderedJsonArrays(json, allowOthers, matchers.tail, pos+1, values.tail, throwException, path, errorMessage)

    } else {
      val newPath = path+"("+pos+")"
      val matches = matchJson(matchers.head, values.head, throwException, ignoreArrayOrder = false, newPath)
      if (!matches && errorMessage.isDefined) {
        matchJsonFailed(errorMessage.get + s"First non-matching element found at position $pos. Expected ${matchers.head.pp()}, found ${values.head.pp()}.", throwException, newPath)
      } else if (!matches) {
        if (verbose && values.size==1)
          matchJson(matchers.head, values.head, throwException = true, ignoreArrayOrder = false, path)
        matchJsonFailed(s"${values.head.pp()} in the array ${json.pp()} isn't ${matchers.head.pp()}", throwException, newPath)
      } else
        matchOrderedJsonArrays(json, allowOthers, matchers.tail, pos+1, values.tail, throwException, path, errorMessage)
    }
  }

  private def matchJsonArrays(matcher: JArray, json: JArray, throwException: Boolean, ignoreArrayOrder: Boolean, path: String): Boolean = {
    val hasAllowOthers = matcher.value.contains(J(___allowOtherValues))
    val hasIgnoreOrder = matcher.value.contains(J(___ignoreOrder)) || ignoreArrayOrder
    val hasNumElements = matcher.value.contains(J(___numElements))

    if (hasAllowOthers && hasNumElements)
      throw new Exception("Matcher contains both ___allowOthers and ___numElements")

    val cleanMatcher = getValuesWithoutNumsAndIgnores(matcher.value)
    val cleanJson = getValuesWithoutNumsAndIgnores(json.value)

    if (hasNumElements && getWantedNumArrayElements(matcher)!=cleanJson.size)
      matchJsonFailed(s"${json.pp()} contains wrong number of elements. Should contain ${getWantedNumArrayElements(matcher)}.", throwException, path)

    else if (!hasAllowOthers && !hasNumElements && cleanMatcher.size > cleanJson.size)
      matchJsonFailed(s"${json.pp()}\n      *** contains less fields than ***\n ${matcher.pp()}.", throwException, path)

    else if (!hasAllowOthers && !hasNumElements && cleanMatcher.size < cleanJson.size)
      matchOrderedJsonArrays(json, allowOthers = false, cleanMatcher, 0, cleanJson, throwException = throwException, path,
                             Some(s"Array contains more fields than matcher. Expected ${cleanMatcher.size}, found ${cleanJson.size}.\nMaybe you forgot to add an ___allowOtherValues value to the matcher?\n")
                            )

    else if (hasIgnoreOrder)
      cleanMatcher.forall( (matchValue: JValue) =>
        if (matchValue.asJsValue == ___allowOtherValues)
          true
        else if (!cleanJson.exists(matchJson(matchValue, _, throwException = false, ignoreArrayOrder = ignoreArrayOrder, path))) {
          if (verbose && json.value.size==1)
            matchJson(cleanJson.head, matchValue, throwException = true, ignoreArrayOrder = ignoreArrayOrder, path)
          matchJsonFailed(s"""${json.pp()} doesn't contain the value "${matchValue.pp()}"""", throwException, path)
        }else
          true
      )
    else
      matchOrderedJsonArrays(json, hasAllowOthers, cleanMatcher, 0, cleanJson, throwException, path, None)
  }

  private def matchJsonObjects(matcher: JValue, jsonWithoutMaybes: JValue, throwException: Boolean, ignoreArrayOrder: Boolean, path: String): Boolean = {
    val maybeKeys = matcher.asMap.filter{
      case (_, value: Maybe) => true
      case _                 => false
    }.keys
    val unusedMaybeKeys = maybeKeys.filter( key => !jsonWithoutMaybes.keys.contains(key))
    val maybes = JObject(
      unusedMaybeKeys.map(key =>
        key -> JNull
      ).toMap
    )

    val json = jsonWithoutMaybes ++ maybes

    val hasAllowOthers = matcher.keys.contains(___allowOtherJsonsKey)
    val hasNumElements = matcher.keys.contains(___numElements)
    val matcherSize = if (hasNumElements) matcher.size-1 else matcher.size

    if (hasAllowOthers && hasNumElements)
      throw new Exception("Matcher contains both ___allowOthers and ___numElements")

    if (hasNumElements && getWantedNumObjectElements(matcher.asMap) != json.size)
      matchJsonFailed(s"${matcher.pp()} contains wrong number of elements. Should contain ${getWantedNumObjectElements(matcher.asMap)}, contains ${json.size}.", throwException, path)

    else if (!hasAllowOthers && !hasNumElements && matcher.size > json.size) {
      val keys1 = json.keys
      val keys2 = matcher.keys
      val diff = keys2 -- keys1

      matchJsonFailed(s"In ${matcher.pp()}, the following fields are added: ${diff.mkString(", ")}\nMaybe you forgot to add an ___allowOtherFields value to the matcher.", throwException, path)

    } else if (!hasAllowOthers && !hasNumElements && matcher.size < json.size) {

      val keys1 = json.keys
      val keys2 = matcher.keys
      val diff = keys1 -- keys2

      matchJsonFailed(s"In ${matcher.pp()}, the following fields are missing: ${diff.mkString(", ")}", throwException, path)

    } else
      matcher.asMap.forall {
        case (key: String, value: JValue) =>
          if (key == ___numElements)
            true
          else if (key == ___allowOtherJsonsKey)
            true
          else if (!json.keys.contains(key))
            matchJsonFailed(s"""${json.pp()} doesn't contain the key "$key", which is defined in the matcher: ${matcher.pp()}""", throwException, path)
          else
            matchJson(value, json(key), throwException, ignoreArrayOrder, if (path == "") key else path + "." + key)
      }
  }

  private def matchJsonObjects(matcher: JsObject, jsonWithoutMaybes: JsObject, throwException: Boolean, ignoreArrayOrder: Boolean, path: String): Boolean =
    matchJsonObjects(J(matcher), J(jsonWithoutMaybes), throwException, ignoreArrayOrder, path)

  private def getStackTraceString(t: Throwable) = {
    val sw = new java.io.StringWriter()
    t.printStackTrace(new java.io.PrintWriter(sw))
    sw.toString
  }

  private def matchCustom(custom: Custom, json: JValue, throwException: Boolean, path: String): Boolean = {
    val result = try{
      custom.func(json)
    } catch {
      case e: Throwable =>
        matchJsonFailed(s""""The custom matcher $custom threw an exception: "${getStackTraceString(e)}" when called with "${json.pp()}".""", throwException, path)
    }

    if (result)
      true
    else
      matchJsonFailed(s""""The custom matcher function $custom returned false when called with "${json.pp()}".""", throwException, path)
  }

  private def matchOr(or: Or, json: JValue, throwException: Boolean, ignoreArrayOrder: Boolean, path: String): Boolean = {
    def inner(matchers: Seq[JValue]): Boolean =
      if (matchers.isEmpty && throwException)
        matchJsonFailed(s"""Doesn't match:\n${or.matchers.map(_.pp()).mkString("Or(", ", ", ")")}\n VS. ${json.pp()}""", throwException, path)
      else if (matchers.isEmpty)
        false
      else if (matchJson(matchers.head, json, throwException = false, ignoreArrayOrder = ignoreArrayOrder, path))
        true
      else
        inner(matchers.tail)

    inner(or.matchers)
  }

  private def matchAnd(and: And, json: JValue, throwException: Boolean, ignoreArrayOrder: Boolean, path: String): Boolean = {
    def inner(matchers: Seq[JValue]): Boolean =
      if (matchers.isEmpty)
        true
      else if (matchJson(matchers.head, json, false, ignoreArrayOrder, path))
        inner(matchers.tail)
      else if (throwException)
        matchJsonFailed(s"""Doesn't match:\n${and.matchers.map(_.pp()).mkString("And(", ", ", ")")}\n VS. ${json.pp()}""", throwException, path)
      else
        false

    inner(and.matchers)
  }

  private def matchJson(matcher: JValue, json: JValue, throwException: Boolean, ignoreArrayOrder: Boolean, path: String): Boolean = {

    if (verbose && matcher.isString && json.isString)
      println("matching "+matcher.asString+", vs. "+json.asString)
    else if (very_verbose)
      println("matching "+matcher.pp()+", vs. "+json.pp())

    val success = matcher match {
      case (m: Or      ) => matchOr(m, json, throwException, ignoreArrayOrder, path)
      case (m: And     ) => matchAnd(m, json, throwException, ignoreArrayOrder, path)
      case (m: Maybe   ) => matchJson(m.matcher, json,  throwException, ignoreArrayOrder, path)
      case (c: Custom  ) => matchCustom(c, json, throwException, path)
      case (r: RegExp  ) if json.isString => if (r.check(json.asString)) true else matchJsonFailed(s""""${json.asString}" doesn't match the regexp "${r.pattern}".""", throwException, path)
      case (r: RegExp  ) => matchJsonFailed(s"""RegExp matcher expected a string. Not-string value: "${json.pp()}"""", throwException, path)
      case _ => (matcher.asJsValue, json) match {
        case (`___anyString`,  j: JString) => true
        case (`___anyNumber`,  j: JNumber) => true
        case (`___anyObject`,  j: JObject) => true
        case (`___anyArray`,   j: JArray) => true
        case (`___anyBoolean`, j: JBoolean) => true
        case (m: JsObject,     j: JObject) => matchJsonObjects(matcher.asInstanceOf[JObject],j,throwException,ignoreArrayOrder,path)
        case (m: JsArray,      j: JArray)  => matchJsonArrays(matcher.asInstanceOf[JArray],j,throwException,ignoreArrayOrder,path)
        case (m, j) => if (m==j.asJsValue) true else matchJsonFailed(s"Doesn't match. Expected ${matcher.pp()}. Got ${json.pp()}", throwException, path)
      }
    }

    if (verbose && success)
      println("...success")
    if (verbose && !success)
      println("...failed")

    success
  }

  private def matchJson(any_matcher: Any, any_json: Any, throwException: Boolean, ignoreArrayOrder: Boolean, path: String): Boolean = {
    val matcher = J(any_matcher)
    val json = J(any_json)
    matchJson(matcher, json, throwException, ignoreArrayOrder, path)
  }


  /** Matches two json values.
    * @param matcher This json value can contain special values such as [[___anyString]] or [[___anyBoolean]]
    * 
    * The value does not have to be a [[JValue]]. 'matchJson' understands a number of different types such as JsValue (The play framework version of [[JValue]]), Map, String, etc.
    * 
    * @param json This is the json value we want to check against the pattern of 'matcher'
    * 
    * The value does not have to be a [[JValue]]. 'matchJson' understands a number of different types such as JsValue (The play framework version of [[JValue]]), Map, String, etc.
    * 
    * @param throwException If true, a JsonMatcherException exception will be thrown if matching fails.
    *
    * @param ignoreArrayOrder  When matching arrays, the order within the arrays will not be checked.
    * 
    *                         The difference between 'ignoreArrayOrder' and the [[___ignoreOrder]]
    *                         matcher is that 'ignoreArrayOrder' works recursively, while [[___ignoreOrder]] only works for the currently matched array.
    * 
    * @note Using the [[___ignoreOrder]] matcher will have no effect if 'ignoreArrayOrder' is true.
    * 
    * @return true if the patterns matched, false if not
    */
  def matchJson(matcher: Any, json: Any, throwException: Boolean = true, ignoreArrayOrder: Boolean = false): Boolean =
    matchJson(matcher, json, throwException, ignoreArrayOrder, "")


}

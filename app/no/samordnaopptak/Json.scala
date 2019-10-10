package no.samordnaopptak.json

import scala.collection.immutable.ListMap

import play.api.libs.json._

import no.samordnaopptak.test.TestByAnnotation.Test


class JsonException(val message: String) extends Exception(message)
class JsonMergeObjectsException(message: String) extends JsonException(message)
class JsonIllegalConversionException(message: String) extends JsonException(message)
class JsonParseException(message: String) extends JsonException(message)


/**
  * Similar to Play Framework's "JsValue", but quicker to use for testing and parsing.
  * @note To convert a JsValue into a [[JValue]], use [[J.apply]].
  * @note To convert a [[JValue]] into a JsValue, use [[asJsValue]].
  * @see [[https://github.com/kmatheussen/doppelauge/blob/master/test/JsonSpec.scala]] for examples
  */
trait JValue {

  private var visitedKeys: Set[String] = Set()

  /**
    Pretty print
    */
  def pp() = Json.prettyPrint(this.asJsValue)

  override def toString =
    "JValue(\n"+pp()+"\n)"

  def asDouble = asNumber.toDouble
  def asInt = asNumber.toInt
  def asLong = asNumber.toLong

  protected def error =
    throw new JsonException(s"""Trying to access something illegal in JValue (see backtrace). value: ${pp()}""")

  protected def illegalConversionError(t: String) =
    throw new JsonIllegalConversionException(s"""Can not convert ${pp()} to $t""")

  def asJsValue: JsValue
  def asJsObject: JsObject = illegalConversionError("object")
  def asJsArray: JsArray = illegalConversionError("array")
  def asMap: ListMap[String, JValue] = illegalConversionError("map")
  def asArray: Seq[JValue] = illegalConversionError("list")
  def asString: String = illegalConversionError("string")
  def asNumber: BigDecimal = illegalConversionError("number")
  def asBoolean: Boolean = illegalConversionError("boolean")

  /**
    * Less verbose way to map content of a json object
    * 
    * @example
    {{{

      // Example 1:

      json.change {
        case ("adouble", value: JValue) => "adouble" -> 9.87
      }

      // Is the same as:

      J(
        json.asMap.map {
          case ("adouble",   value: JValue) => "adouble" -> 9.87
          case (key: String, value: JValue) => key       -> value
        }
      )


      // Example 2:

      json.change {
        case ("anint", _)               => J.___removeThisField
        case ("adouble", value: JValue) => "adouble" -> 9.87
      }

     // is the same as:

      J(
        json.asMap.map {
          case ("anint", _)                 => J.___removeThisField
          case ("adouble",   value: JValue) => "adouble" -> 9.87
          case (key: String, value: JValue) => key       -> value
        }.filter(_ != J.___removeThisField)
      )

    }}}
    * 
    * @note There is no warning or exception thrown if trying to change a non-existing field.
    * @see [[JsonChanger]] for a much safer way to change json values.
    */
  def change(converter: PartialFunction[(String, JValue), (String, Any)]): JValue =
    illegalConversionError("object")

  /**
    * @note object must be an array
    */
  def map(converter: JValue => JValue): JValue =
    illegalConversionError("array")

  /**
    * @note object must be an array
    */
  def foreach(func: JValue => Unit): Unit =
    illegalConversionError("array")

  /*
  /**
    * @note object must be an array
    */
  def filter(filterFunc: JValue => Boolean): JValue =
    illegalConversionError("array")
   */

  /**
    * @note object must be an array, and the function must return a JBoolean
    */
  def filter(filterFunc: JValue => JValue): JValue =
    illegalConversionError("array")

  def isArray: Boolean = false
  def isObject: Boolean = false
  def isNumber: Boolean = false
  def isBoolean: Boolean = false
  def isString: Boolean = false
  def isNull: Boolean = false

  def isDefined: Boolean = true
  def hasKey(key: String): Boolean = illegalConversionError(s"""object with key "$key"""")  // it might be undefined even if it has key. (i.e. when the value is null)
  def keys: Set[String] = illegalConversionError("object")
  def size: Int = illegalConversionError("object or array")

  def ++(other: JValue): JObject = illegalConversionError("object")
  def -(key: String): JValue     = illegalConversionError("object")
  def +(other: String): JValue   = illegalConversionError("string")
  def +(other: BigDecimal): JValue   = illegalConversionError("number")
  def -(other: BigDecimal): JValue   = illegalConversionError("number")
  def *(other: BigDecimal): JValue   = illegalConversionError("number")
  def /(other: BigDecimal): JValue   = illegalConversionError("number")
  def +(other: JValue): JValue   = illegalConversionError("number or string")
  def -(other: JValue): JValue   = illegalConversionError("number")
  def *(other: JValue): JValue   = illegalConversionError("number")
  def /(other: JValue): JValue   = illegalConversionError("number")

  def ==(other: BigDecimal): JValue   = illegalConversionError("number")
  def !=(other: BigDecimal): JValue   = illegalConversionError("number")
  def >(other: BigDecimal): JValue   = illegalConversionError("number")
  def <(other: BigDecimal): JValue   = illegalConversionError("number")
  def >=(other: BigDecimal): JValue   = illegalConversionError("number")
  def <=(other: BigDecimal): JValue   = illegalConversionError("number")

  def &&(other: JValue): JValue   = illegalConversionError("boolean")
  def ||(other: JValue): JValue   = illegalConversionError("boolean")

  def asLongArray = asArray.map(_.asLong)
  def asIntArray = asArray.map(_.asInt)
  def asDoubleArray = asArray.map(_.asDouble)
  def asStringArray = asArray.map(_.asString)
  def asBooleanArray = asArray.map(_.asBoolean)

  /**
    * @example
    * {{{
    val json = J.obj("a" -> 1)

    json("a").asOption(_.asInt) === Some(1)
    json("b").asOption(_.asInt) === None
    * }}}
    */
  def asOption[R](command: JValue => R): Option[R] =
    Some(command(this))

  def getOrElse(orElseValue: Any): JValue =
    this


  /**
    * @example
    * {{{
    val json = J.obj(
      "a" -> J.arr(5,2,J.obj("b" -> 9)),
      "b" -> 10
    )

    json.getRecursively("b").asIntArray === List(9,10)
    * }}}
    * @see [[pick]]
    */
  def getRecursively(key: String): Seq[JValue] =
    if (isObject)
      asMap.flatMap {
        case (`key`, json) => Seq(json)
        case (_,     json) => json.getRecursively(key)
      }.toSeq
    else if (isArray)
      asArray.flatMap(_.getRecursively(key))
    else
      Seq()

  /**
    * @example
    * {{{
    val json = J.obj(
      "a" -> J.arr(5,2,J.obj("b" -> 9)),
      "b" -> 10
    )

    json("a")(2)("b").asInt === 9
    * }}}
    * @see [[pick]]
    */
  def apply(key: String): JValue =
    asMap.get(key) match {
      case Some(json) => {
        visitedKeys = visitedKeys + key
        json
      }
      case None =>
        JUndefined(key, this)
    }

  /**
    * @example
    * {{{
    val json = J.arr(2,3,4)

    json(0) === 2
    json(1) === 3
    json(2) === 4
    * }}}
    * @see [[pick]]
    */
  def apply(index: Int): JValue =
    try{
      asArray(index)
    }catch{
      case e: java.lang.IndexOutOfBoundsException =>
        throw new JsonException("index "+index+" not found in "+pp()+" ("+e.getMessage+")")
    }


  /*
   Direct and less readable implemention of pick:

  private def pick(orgPickers: Any, pickers: Any): JValue = pickers match {
    case p: String           => apply(p)
    case p: Int              => apply(p)
    case (r: Any, p: Any)    => getPickArguments2(orgPickers, r).getPickArguments2(orgPickers, p)
    case _                   => throw new IllegalArgumentException("Illegal argument when calling pick("+orgPickers+"): "+pickers+". The argument for pick must be a string, a number or a tuple")
  }

  def pick(pickers: Any): JValue = pick(pickers, pickers)
   */


  protected def pick2(pickers: Seq[Any]): JValue =
    pickers match {

      case Nil                 => this

      case (p: String) :: rest if p.endsWith("(recursively)") =>
        val realKey = p.take(p.length - "(recursively)".length).trim
        JArray(getRecursively(realKey)).pick2(rest)

      case (p: String) :: rest => apply(p).pick2(rest)
      case (p: Int)    :: rest => apply(p).pick2(rest)
    }

  private def getPickArguments(orgPickers: Any, pickers: Any): Seq[Any] = pickers match {
    case p: String           => Seq(p)
    case p: Int              => Seq(p)
    case (r: Any, p: Any)    => getPickArguments(orgPickers, r) ++ Seq(p)
    case _                   => throw new IllegalArgumentException("Illegal argument when calling pick("+orgPickers+"): "+pickers+". The argument for pick must be a string, a number or a tuple")
  }

  /**
    * Less cluttering way to pick out data than calling apply() several times.
    * @example
    * {{{
    val json = J.obj(
      "a" -> J.arr(5,2,J.obj("b" -> 9)),
      "b" -> 10
    )

    json.pick("a" -> 2 -> "b").asInt === 9
    json.pick("a" -> "b (recursively)").asIntArray === List(9)
    * }}}
    */
  def pick(pickers: Any): JValue =
    pick2(getPickArguments(pickers, pickers))
  

  /**
    * Throws exception if there are unread fields in the json object.
    * Also throws exception if '''this''' is not a json object.
    * 
    * @example
    * {{{
    val json = J.parse("""{ "a" : 1}""")
    json.validateRemaining()  must throwA[JsonParseException]
    json("a")
    json.validateRemaining()

    val json2 = J.parse("""{ "b" : 1}""")
    json2.validateRemaining(Set("b"))
    json2.validateRemaining()  must throwA[JsonParseException]   
    * }}}
    * 
    * @see [[J.parseAndValidate]]
    */
  def validateRemaining(ignoreKeys: Set[String]): Unit = {
    val visitedKeys = this.visitedKeys ++ ignoreKeys
    val diff = asMap.keys.toSet.diff(visitedKeys)

    if (diff.nonEmpty)
      throw new JsonParseException(s"""Unknown field(s): ${diff.mkString("\"", "\", \"", "\"")}""")
  }

  /**
    * Implementation:
    * {{{
  def validateRemaining(ignoreKeys: String*): Unit =
    validateRemaining(ignoreKeys.toSet)
    * }}}
    */
  def validateRemaining(ignoreKeys: String*): Unit =
    validateRemaining(ignoreKeys.toSet)
}


case class JNumber(value: BigDecimal) extends JValue{
  override def asNumber = value
  override def isNumber = true
  override def asJsValue = JsNumber(value)

  // dynamic typing with JValue:

  override def +(other: BigDecimal): JValue =
    JNumber(value + other)

  override def -(other: BigDecimal): JValue =
    JNumber(value - other)

  override def *(other: BigDecimal): JValue =
    JNumber(value * other)

  override def /(other: BigDecimal): JValue =
    JNumber(value / other)

  override def ==(other: BigDecimal): JValue =
    JBoolean(value == other)

  override def !=(other: BigDecimal): JValue =
    JBoolean(value != other)

  override def >(other: BigDecimal): JValue =
    JBoolean(value > other)

  override def <(other: BigDecimal): JValue =
    JBoolean(value < other)

  override def <=(other: BigDecimal): JValue =
    JBoolean(value <= other)

  override def >=(other: BigDecimal): JValue =
    JBoolean(value >= other)

  override def +(other: JValue): JValue =
    JNumber(value + other.asNumber)

  override def -(other: JValue): JValue =
    JNumber(value - other.asNumber)

  override def *(other: JValue): JValue =
    JNumber(value * other.asNumber)

  override def /(other: JValue): JValue =
    JNumber(value / other.asNumber)
}

case class JString(value: String) extends JValue{
  override def asString = value
  override def isString = true
  override def asJsValue = JsString(value)

  override def +(other: String): JValue =
    JString(value + other)

  override def +(other: JValue): JValue =
    JString(value + other.asString)
}

case class JBoolean(value: Boolean) extends JValue{
  override def asBoolean = value
  override def isBoolean = true
  override def asJsValue = JsBoolean(value)

  override def &&(other: JValue): JValue =
    JBoolean(value && other.asBoolean)

  override def ||(other: JValue): JValue =
    JBoolean(value || other.asBoolean)
}

/**
  Similar to Play Framework's "JsObject", except that the fields order is preserved, plus that '++' does not allow field name clash.
  */
case class JObject(value: ListMap[String, JValue]) extends JValue{
  override def asMap: ListMap[String, JValue] = value
  override def isObject = true
  override def asJsValue = asJsObject
  override def asJsObject = JsObject(
    value.map{
      case (k: String, v: JValue) => k -> v.asJsValue
    }
  )
  override def size = value.size
  override def keys = value.keys.toSet
  override def hasKey(key: String) = keys.contains(key)

  override def change(converter: PartialFunction[(String, JValue), (String, Any)]): JValue = {
    J(
      value.map(
        converter
          andThen {
            case (a, b) => a -> b
          }
          orElse {
            case (a, b) => a -> b
          }
      ).filter(_ != J.___removeThisField)
    )
  }

  /**
    * Throws exception if key clash
    */
  override def ++(other: JValue): JObject = {
    val otherValue = other.asMap
    val result = value ++ otherValue

    if (result.size != value.size+otherValue.size)
      throw new JsonMergeObjectsException("JObject.++: objects intersects :" + value.keys.toSet.intersect(otherValue.keys.toSet))

    JObject(result)
  }

  /**
    * Throws exception if key doesn't exist in the object
    */
  override def -(key: String): JObject =
    if (!value.contains(key))
      throw new JsonMergeObjectsException("JObject.-: key not found: "+key)
    else
      JObject(value - key)
}

object JObject{
  def apply(value: Seq[(String, JValue)]): JObject = JObject(ListMap(value:_*))
  def apply(value: Map[String, JValue]): JObject = JObject(ListMap(value.toSeq :_*))
}

case class JArray(value: Seq[JValue]) extends JValue{
  override def asArray = value
  override def isArray = true
  override def asJsValue = asJsArray
  override def asJsArray = JsArray(value.map(_.asJsValue))
  override def size = value.size

  override def map(converter: JValue => JValue): JValue =
    JArray(value.map(converter))

  override def foreach(func: JValue => Unit): Unit =
    value.foreach(func)

  /*
  override def filter(filterFunc: JValue => Boolean): JValue =
    JArray(value.filter(filterFunc))
   */
  
  override def filter(filterFunc: JValue => JValue): JValue =
    JArray(value.filter(filterFunc(_).asBoolean))


/*
  override def ++(other: JValue): JObject =
    JArray(value ++ other.asArray)
 */
}

case class JUndefined(key: String, parent: JValue) extends JValue{
  override def pp() = "Undefined key '"+key+"' in " + parent.pp()
  override def error = throw new JsonException("""Trying to access key """"+key+"""", which is not found in """+parent)
  override def asOption[R](command: JValue => R): Option[R] = None
  override def getOrElse(orElseValue: Any): JValue = J(orElseValue)
  override val isDefined = false
  override def asJsValue = JsNull
}

object JNull extends JValue {
  override def asJsValue = JsNull
  override def isNull = true
  override def asOption[R](command: JValue => R): Option[R] = None
  override def getOrElse(orElseValue: Any): JValue = J(orElseValue)
  override val isDefined = false
}


/**
  * Similar to Play framework's "Json" object, but quicker to use for testing and parsing.
  * @note J is safer than Play framework's 'Json' object since it doesn't allow key clash when creating and merging objects.
  * @note J guarantees to maintain object field order. Play framework's 'Json' object does not.
  * @see [[https://github.com/kmatheussen/doppelauge/blob/master/test/JsonSpec.scala]] for examples
  */
object J {

  private def jsValueToJValue(value: JsValue): JValue =
    value match {
      case j: JValue   => j
      case j: JsNumber => JNumber(j.value)
      case j: JsString => JString(j.value)
      case j: JsBoolean => JBoolean(j.value)
      case j: JsObject => JObject(
        j.fields.map{
          case (k: String, v: JsValue) => k -> jsValueToJValue(v)
        }
      )
      case j: JsArray => JArray(j.value.map(jsValueToJValue).toSeq)
      case `JsNull` => JNull
    }

  /**
    * Converts any of the following types into a JValue: [[JValue]], JsValue, BigDecimal, Int, Long, Float, Double, String, Boolean, Map[String,_], Iterable[_], Array[_], Option[_], Json.JsValueWrapper.
    */
  def apply(a: Any): JValue =
    a match {
      case value: JValue => value
      case value: JsValue =>  jsValueToJValue(value)
      case value: BigDecimal =>  JNumber(value)
      case value: Int =>  JNumber(value)
      case value: Long =>  JNumber(value)
      case value: Float =>  JNumber(value)
      case value: Double =>  JNumber(value)
      case value: String => JString(value)
      case value: Boolean => JBoolean(value)
      case value: ListMap[_,_] => JObject(
        value.asInstanceOf[ListMap[String,Any]].map{
          case (k: String, null)   => k -> JNull // don't know exactly why this is necessary, but we get error if null is not handled explicitly.
          case (k: String, v: Any) => k -> apply(v)
        }
      )
      case value: Map[_,_] => apply(ListMap(value.toList:_*))
      /* TODO. Make this work:
       case value: Iterable[(String,_)] => apply(
        ListMap(value:_*)
      )
       */
      case value: Iterable[_] => JArray(value.map(apply).toSeq)
      case value: Array[_] => JArray(value.map(apply).toSeq)
      case `None` => JNull
      case Some(value) => apply(value)
      case _ if a==null => JNull
      case value: Json.JsValueWrapper => apply(Json.arr(value)(0).get)
      case _ => throw new Exception(s"""Unable to convert "$a" to JValue. Class: ${a.getClass}""")
    }

  private def validateUniqueKeys(keys: List[String], json: JValue): Unit = keys match {
    case `Nil`               => ()
    case _ :: `Nil`          => ()
    case a :: b :: _ if a==b => throw new JsonException("The key \"" + a + "\" is defined twice in " + json.pp())
    case _ :: rest           => validateUniqueKeys(rest, json)
  }

  def obj(vals: (String, Any)*): JObject = {
    val ret = apply(ListMap(vals:_*)).asInstanceOf[JObject]

    if (ret.size != vals.size) {
      validateUniqueKeys(vals.map(_._1).sorted.toList, ret)
      throw new Exception("Error: The input to J.obj() has a different size than the output of J.obj(), but there are apparently no duplicate keys in the input. Input: "+vals+", output: "+ret.pp())
    }

    ret
  }

  def arr(vals: Any*): JArray =
    apply(vals.map(apply)).asInstanceOf[JArray]

  def parse(jsonString: String): JValue =
    try{
      jsValueToJValue(play.api.libs.json.Json.parse(jsonString))
    }catch{
      case e: Throwable => throw new JsonParseException(s"""Could not parse "$jsonString": ${e.getMessage}""")
    }

  /**
    * Combines [[parse]] and JValue.validateRemaining
    * @example
    * {{{
      val jsonText = """
          {
            "a" : 1,
            "b" : 2
          }
        """

      J.parseAndValidate(jsonText){json =>
        List(
          json("a"),
          json("b")
       )
      }

      J.parseAndValidate(jsonText){json =>
        List(
          json("a")
       )
      } must throwA[JsonParseException]

      J.parseAndValidate(jsonText, ignoreKeys=Set("b")){json =>
        List(
          json("a")
       )
      }

      J.parseAndValidate(jsonText, allowedKeys=Set("a")){json =>
        List(
          json("a"),
          json("b")
       )
      } must throwA[JsonParseException]
    * }}}
    */
  def parseAndValidate[R](jsonString: String, ignore: Set[String] = Set(), allowedKeys: Set[String] = Set())(command: JValue => R): R = {
    val json = parse(jsonString)

    if (allowedKeys != Set() && json.isObject)
      json.asMap.keys.foreach(key =>
        if (!allowedKeys.contains(key))
          throw new JsonParseException(s"""Key "$key" is not supported when parsing json string""")
      )

    val ret = command(json)
    json.validateRemaining(ignore)
    ret
  }

  /**
   * Same as [[parseAndValidate]], except that it doesn't validate that all fields are read.
   * @example
   * {{{
      val jsonText = """
          {
            "a" : 1,
            "b" : 2
          }
        """

      J.parseIt(jsonText) { json =>
        json("a")
      } === 2
   * }}}
   */
  def parseIt[R](jsonString: String)(command: JValue => R): R =
    command(parse(jsonString))

  /**
    * Used in conjunction with [[JValue.change]]
    */
  val ___removeThisField: (String, Any) = "___remove_this_field___" -> "___remove_this_field___"

  /**
    * Does NOT throw exception if key clash. Might want to use {{{
    flattenJObjects(objs.map(J(_))).asJsValue
    * }}} instead.
    */
  def flattenJsObjects(objs: Seq[JsObject]): JsObject =
    if (objs.isEmpty)
      Json.obj()
    else
      objs.head ++ flattenJsObjects(objs.tail)

  /*
  def flattenJsObjects(objs: JsObject*): JsObject =
    flattenJsObjects(objs.toSeq)
   */

  /**
    * Throws exception if key clash
    */
  def flattenJObjects(objs: Seq[JValue]): JObject =
    if (objs.isEmpty)
      obj()
    else
      objs.head ++ flattenJObjects(objs.tail)
}


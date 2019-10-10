package no.samordnaopptak.json

import play.api.libs.json.JsString


class JsonChangerException(message: String, val path: String) extends Exception(message)

/**
  * Change the content of a json value in a safe and descriptive manner.
  * 
  * Firstly, the name is misleading. JsonChanger is not ''changing'' (i.e mutating) the values. It returns a new value.
  * 
  * The main function, [[JsonChanger.apply]] takes two arguments: '''json_value''' and '''changer'''.
  * The '''changer''' variable has similarities to the '''matcher''' variable used in [[JsonMatcher]], but while
  * [[JsonMatcher]] only returns '''true''' or '''false''', JsonChanger returns a new Json value.
  * 
  * The most important similarity between JsonChanger and [[JsonMatcher]] is that they both do pattern matching.
  * When changing a json value with JsonChanger, it also pattern matches the '''json_value''' against the '''changer'''. This pattern matching
  * should make bugs appear earlier than they would have been othervice.
  * (You might argue that it would be better to first validate the json against a schema, but this way you get validation for free, plus that the validation schema maintains itself automatically.)
  * 
  * There are several custom changers such as [[___identity]], [[Replace]], [[Func]], [[Map]], [[MapChanger]], etc. See documentation below for examples.
  * Custom changers can also be created from the outside by implementing the [[Changer]] trait.
  * 
  * The pattern matcher in JsonChanger checks that: a) a json value doesn't change type (unless we tell it to),
  * b) we don't add or remove fields to objects (unless we tell it to), c) we don't add or remove values to arrays (unless we tell it to).
  * 
  * 
  * @example
  {{{
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
  }}}
  * 
  * @see [[https://github.com/kmatheussen/doppelauge/blob/master/test/JsonChangerSpec.scala]] for more examples
  */
object JsonChanger{

  private def throwChangeException(message: String, path: String) =
    throw new JsonChangerException(message+"\n\npath: "+path+"\n\n ", path)



  trait Type {
    val typeName: String

    def is(json: JValue): Boolean

    private def throwIt(json: JValue, path: String) =
      throwChangeException("JsonChanger.TypeValidator expected a value of the type \""+typeName+"\", but found "+json.pp() + " instead.", path)

    /**
      * Throw exception if type is incorrect
      */
    def validate(json: JValue, path: String) =
      if (!is(json))
        throwIt(json, path)
  }



  /**
    * Used in conjunction with [[TypeChange]], [[Func]], and [[Map]]
    */
  object Expects {

    class Or(validators: Type*) extends Type {
      val typeName = validators.map(_.typeName).mkString(" or ")
      def is(json: JValue) = validators.exists(_.is(json))
    }

    object Or {
      def apply(validators: Type*) =
        new Or(validators:_*)
    }

    class And(validators: Type*) extends Type {
      val typeName = validators.map(_.typeName).mkString(" and ")
      def is(json: JValue) = validators.forall(_.is(json))
    }

    object And {
      def apply(validators: Type*) =
        new And(validators:_*)
    }

    /**
      * Expects undefined or '''type_'''
      */
    class Maybe(type_ : Type) extends Or(type_, Undefined)

    object Maybe {
      def apply(type_ : Type) = new Maybe(type_)
    }


    /**
      * Expects object
      */
    object Object extends Type{
      val typeName = "object"
      def is(json: JValue) = json.isObject
    }

    /**
      * Expects array
      */
    object Array extends Type{
      val typeName = "array"
      def is(json: JValue) = json.isArray
    }

    /**
      * Expects number
      */
    object Number extends Type{
      val typeName = "number"
      def is(json: JValue) = json.isNumber
    }

    /**
      * Expects string
      */
    object String extends Type{
      val typeName = "string"
      def is(json: JValue) = json.isString
    }

    /**
      * Expects boolean
      */
    object Boolean extends Type{
      val typeName = "boolean"
      def is(json: JValue) = json.isBoolean
    }



    /**
      * Expects object or undefined
      */
    object MaybeObject extends Maybe(Object)

    /**
      * Expects array or undefined
      */
    object MaybeArray extends  Maybe(Array)

    /**
      * Expects number or undefined
      */
    object MaybeNumber extends Maybe(Number)

    /**
      * Expects string or undefined
      */
    object MaybeString extends Maybe(String)

    /**
      * Expects boolean or undefined
      */
    object MaybeBoolean extends Maybe(Boolean)



    /**
      * Expects null
      */
    object Null extends Type{
      val typeName = "null"
      def is(json: JValue) = json.isNull
    }

    /**
      * Expects undefined value. An undefined value is either null, or the result of trying to do e.g.
      * {{{
      J.obj()("hello")
      * }}}
      */
    object Undefined extends Type{
      val typeName = "undefined"
      def is(json: JValue) = !json.isDefined
    }

    /**
      * Expects a defined value. Defined values are all values that are not undefined
      * @see [[Undefined]]
      */
    object Defined extends Type{
      val typeName = "defined"
      def is(json: JValue) = json.isDefined
    }

    /**
      * Expects anything, both defined and undefined values
      * @see [[Defined]] and [[Undefined]]
      */
    object Any extends Type{
      val typeName = "any"
      def is(json: JValue) = true
    }
  }


  trait InputTypeTransformer[R] {
    protected def createFunc(input_type: Type): R

    val _object = createFunc(Expects.Object)
    val array = createFunc(Expects.Array)
    val string = createFunc(Expects.String)
    val number = createFunc(Expects.Number)
    val boolean = createFunc(Expects.Boolean)

    val _null = createFunc(Expects.Null)
    val defined = createFunc(Expects.Defined)
    val undefined = createFunc(Expects.Undefined)

    val maybeObject = createFunc(Expects.MaybeObject)
    val maybeArray = createFunc(Expects.MaybeArray)
    val maybeString = createFunc(Expects.MaybeString)
    val maybeNumber = createFunc(Expects.MaybeNumber)
    val maybeBoolean = createFunc(Expects.MaybeBoolean)

    val any = createFunc(Expects.Any)
  }


  trait InputOutputTypeTransformer[T,R] {

    protected def createFunc(input_type: Type, output_type: Type, t: T): R

    protected class F(expected_input_type: Type){

      // elegant, but code using it just looks confusing
      //def apply(t: T): R = createFunc(expected_input_type, expected_input_type, t)

      def _object(t: T) = createFunc(expected_input_type, Expects.Object, t)
      def array(t: T) = createFunc(expected_input_type, Expects.Array, t)
      def string(t: T) = createFunc(expected_input_type, Expects.String, t)
      def number(t: T) = createFunc(expected_input_type, Expects.Number, t)
      def boolean(t: T) = createFunc(expected_input_type, Expects.Boolean, t)

      def _null(t: T) = createFunc(expected_input_type, Expects.Null, t)
      def defined(t: T) = createFunc(expected_input_type, Expects.Defined, t)
      def undefined(t: T) = createFunc(expected_input_type, Expects.Undefined, t)

      def maybeObject(t: T) = createFunc(expected_input_type, Expects.MaybeObject, t)
      def maybeArray(t: T) = createFunc(expected_input_type, Expects.MaybeArray, t)
      def maybeString(t: T) = createFunc(expected_input_type, Expects.MaybeString, t)
      def maybeNumber(t: T) = createFunc(expected_input_type, Expects.MaybeNumber, t)
      def maybeBoolean(t: T) = createFunc(expected_input_type, Expects.MaybeBoolean, t)

      def any(t: T) = createFunc(expected_input_type, Expects.Any, t)
    }

    object _object extends F(Expects.Object)
    object array extends F(Expects.Array)
    object string extends F(Expects.String)
    object number extends F(Expects.Number)
    object boolean extends F(Expects.Boolean)

    object _null extends F(Expects.Null)
    object defined extends F(Expects.Defined)
    object undefined extends F(Expects.Undefined)

    object maybeObject extends F(Expects.MaybeObject)
    object maybeArray extends F(Expects.MaybeArray)
    object maybeString extends F(Expects.MaybeString)
    object maybeNumber extends F(Expects.MaybeNumber)
    object maybeBoolean extends F(Expects.MaybeBoolean)

    object any extends F(Expects.Any)
  }

  /**
    * Interface for custom changers.
    * 
    * @example
    * {{{
      object Add1 extends Changer {
        override def pp() = "Add1"

        def transform(json: JValue, path: String, allow_mismatched_types: Boolean) = {
          Expects.Number.validate(json, path)
          JsonChanger.apply(json, json + 1, path, allow_mismatched_types)
        }
      }

      JsonChanger(50, Add1) === J(51)
    * }}}
    * 
    @see [[MaybeChanger]]
    */
  trait Changer extends JValue {
    override def asJsValue = JsString(pp())

    /**
      The transform virtual method.
      @param allow_mismatched_types Must be forwarded if calling [[JsonChanger.apply]], unless you want to explicitly override type mismatching to fail or succeed (see [[TypeChange]]).
      */
    def transform(json: JValue, path: String, allow_mismatched_types: Boolean): Any
  }


  /**
    * Interface for custom changers.
    * 
    * @example
    * This is the implementation of the [[JsonChanger.Maybe]] changer:
    * {{{
  case class Maybe(changer: Any) extends MaybeChanger {
    val j_changer = J(changer)
    override def pp() = "Maybe("+j_changer.pp()+")"

    def maybeTransform(json: JValue, path: String, allow_mismatched_types: Boolean): Option[Any] =
      if (json.isDefined)
        Some(JsonChanger.apply(json, changer, path, allow_mismatched_types))
      else
        None
  }
    * }}}
    * 
    * @see [[Changer]]
    */
  trait MaybeChanger extends Changer {
    def maybeTransform(json: JValue, path: String, allow_mismatched_types: Boolean): Option[Any]

    def transform(json: JValue, path: String, allow_mismatched_types: Boolean): Any = {
      maybeTransform(json, path, allow_mismatched_types) match {
        case None => json
        case Some(value) => value
      }
    }
  }

  /**
    * Replaces the input with a specified output if the input matches.
    * 
    * @example
    {{{
        JsonChanger(
          J.arr(2,3),
          J.arr(Replace(3, 5), Replace(3, 5))
        ) ===
        J.arr(2, 5)

        JsonMatcher.matchJson(
          JsonChanger(
            30,
            Replace(JsonMatcher.___anyNumber, 120)
          ),
          J(120)
        )
    }}}
    * 
    * @see [[JsonMatcher]]
    */
  case class Replace(matcher: Any, changer: Any) extends MaybeChanger {
    val j_matcher = J(matcher)
    val j_changer = J(changer)

    override def pp() = "JsonChanger.Replace(matcher: "+j_matcher.pp()+", changer: "+j_changer.pp()+")"

    def maybeTransform(json: JValue, path: String, allow_mismatched_types: Boolean) =
      if (JsonMatcher.matchJson(j_matcher, json, throwException=false))
        Some(JsonChanger.apply(json, j_changer, path, allow_mismatched_types))
      else
        None
  }

  /**
    * Trying several [[MaybeChanger]]s [[MaybeChanger.maybeTransform]] methods, and returns the result of the first one not returning None.
    * 
    * An exception is thrown if neither of the changers return a value.
    * 
    * @example
    * {{{
      JsonMatcher.matchJson(
        JsonChanger(
          J.obj(
            "aaa" -> 30,
            "bbb" -> 50,
            "ccc" -> 70
          ),
          J.obj(
            "aaa" -> Or(
              Replace(30, 100),
              Replace(50, 120)
            ),
            "bbb" -> Or(
              Replace(30, 100),
              Replace(50, 120)
            ),
            "ccc" -> Or(
              Replace(30, 100),
              Replace(50, 120),
              140
            )
          )
        ),
        J.obj(
          "aaa" -> 100,
          "bbb" -> 120,
          "ccc" -> 140
        )
      )
    * }}}
    * 
    */
  case class Or(changers: Any*) extends MaybeChanger {
    val j_changers = J(changers)

    override def pp() = "JsonChanger.Or(changers: "+j_changers.pp()

    def maybeTransform(json: JValue, path: String, allow_mismatched_types: Boolean) = {

      def inner(changers: List[JValue]): Option[Any] = changers match {
        case `Nil` =>
          throwChangeException("JsonChanger.Or: No changer applied for "+json.pp(), path)

        case ((maybeChanger: MaybeChanger) :: rest) =>
          maybeChanger.maybeTransform(json, path, allow_mismatched_types) match {
            case None => inner(rest)
            case value => value
          }

        case ((changer: JValue) :: _) =>
          Some(JsonChanger.apply(json, changer, path, allow_mismatched_types))
      }

      inner(j_changers.asArray.toList)
    }

  }

  private def validate_input_output_types(expected_input_type: Type, expected_output_type: Type, input: JValue, path: String)(get_output: => Any): JValue = {
    expected_input_type.validate(input, path)

    val output = J(get_output)

    expected_output_type.validate(output, path)

    output
  }

  /**
    * Apply a custom function on a json value.
    * 
    * The ''Func'' object has helper objects to create new Func changers less verbosely.
    * For example, instead of writing ''new Func(Expects.Number, Expects.String, func)'',
    * i.e. a function that takes a number as input argument, and produces a string, we can write ''Func.number.string(func)''.
    * 
    * @example
    {{{
      JsonChanger(
        J.obj(
          "aaa" -> 30
        ),
        J.obj(
          "aaa" -> Func.number.string(_.toString)
        )
      ) ===
      J.obj("aaa" -> "30")
    }}}
    */
  case class Func(expected_input_type: Type, expected_output_type: Type, func: JValue => Any) extends Changer {
    override def pp() = "Func("+expected_input_type.typeName+", "+expected_output_type.typeName+", <func>)"

    def transform(json: JValue, path: String, allow_mismatched_types: Boolean) =
      validate_input_output_types(expected_input_type, expected_output_type, json, path){
        JsonChanger.apply(json, func(json), path, allow_mismatched_types = true)
      }
  }

  object Func extends InputOutputTypeTransformer[JValue => Any, Func] {
    protected def createFunc(expected_input_type: Type, expected_output_type: Type, func: JValue => Any) =
      apply(expected_input_type, expected_output_type, func)
  }


  /**
    * Map a function on array
    * 
    * @example
    {{{
        JsonChanger(
          J.arr(2,3),
          Map.number.number(_ + 2)
        ) ===
        J.arr(4,5)
    }}}
    * 
    * @see [[InputOutputTypeTransformer]]
    */
  case class Map(expected_input_type: Type, expected_output_type: Type, transform_func: JValue => Any) extends Changer {
    override def pp() = "Map"

    def transform(json: JValue, path: String, allow_mismatched_types: Boolean) =
      if (json.isArray)
        json.asArray.map{ value =>
          validate_input_output_types(expected_input_type, expected_output_type, value, path){
            transform_func(value)
          }
        }
      else
        throwChangeException("JsonChanger.Map expects array. Found "+json.pp(), path)
  }

  object Map extends InputOutputTypeTransformer[JValue => Any, Map] {
    protected def createFunc(expected_input_type: Type, expected_output_type: Type, func: JValue => Any) =
      apply(expected_input_type, expected_output_type, func)
  }


  /**
    * Must be used if the output type is different from the input type
    * 
    * @example
    {{{
      JsonChanger(
        50,
        TypeChange.number.string("aiai") // <- input type is number, output type is string
      ) === JString("aiai")
    }}}
    * 
    * @see [[InputOutputTypeTransformer]]
    */
  case class TypeChange(expected_input_type: Type, expected_output_type: Type, changer: Any) extends Changer {
    override def pp() = "TypeChange("+expected_input_type.typeName+", "+expected_output_type.typeName+", "+J(changer).pp()+")"

    val func = new Func(expected_input_type, expected_output_type, _ => changer)

    def transform(json: JValue, path: String, allow_mismatched_types: Boolean) =
      func.transform(json, path, allow_mismatched_types)
  }

  object TypeChange extends InputOutputTypeTransformer[Any, TypeChange] {
    protected def createFunc(expected_input_type: Type, expected_output_type: Type, changer: Any) =
      apply(expected_input_type, expected_output_type, changer)
  }



  /**
    * Change the name and content of a field
    * 
    * @example
    * {{{
      JsonChanger(
        J.obj(
          "aaa" -> 50
        ),
        J.obj(
          "aaa" -> ChangeThisField(
             "bbb" -> ___identity.number)
        )
      ) ===
      J.obj("bbb" -> 50)
    * }}}
    * 
    * If it's uncertain whether the field exists in the input json value, [[Maybe]] can be applied to the changer:
    * {{{
      JsonMatcher.matchJson(
        JsonChanger(
          J.obj(),
          J.obj(
            "aaa" -> ChangeThisField(
               "bbb" -> Maybe(JsonChanger___identity.number))
          )
        ),
        J.obj()
      )
    * }}}
    * Othervice, an exception is thrown if the field doesn't exist. If you want to create a new field no matter what, a combination of [[JsonChanger.Maybe]]([[___removeThisField]]) and [[NewField]] can be used instead.
    */ 
  case class ChangeThisField(newFieldNameAndChanger: (String, Any)) extends JValue {
    val newFieldName = newFieldNameAndChanger._1
    val j_changer = J(newFieldNameAndChanger._2)
    override def pp() = "ChangeThisField: "+newFieldName+", "+j_changer.pp()
    override def asJsValue = JsString(pp())
  }


  /**
    * Add new field to object.
    * @note If the field may already exist in the object, use [[ForceNewField]] instead.
    * 
    * @example
    {{{
        JsonChanger(
          J.obj(
            "aaa" -> 50
          ),
          J.obj(
            "aaa" -> 90,
            "bbb" -> NewField(100)
          )
        ) ===
        J.obj(
          "aaa" -> 90,
          "bbb" -> 100
        )
    }}}
    * 
    */
  case class NewField(value: Any) extends JValue {
    val j_value = J(value)
    override def pp() = "NewField: "+j_value.pp()
    override def asJsValue = JsString(pp())
  }


  /**
    * Same as [[NewField]], but does not throw exception if the field already exists. The argument is also a changer and not a value.
    * If the key doesn't exist, the input value to the changer will be an instance of [[JUndefined]].
    * 
    *  @example
    {{{
      JsonMatcher.matchJson(
        JsonChanger(
          J.obj(
            "aaa" -> "hello"
          ),
          J.obj(
            "aaa" -> ForceNewField(Func.maybeString.string(_.getOrElse("[undefined]"))),
            "bbb" -> ForceNewField(Func.maybeString.string(_.getOrElse("[undefined]")))
          )
        ),
        J.obj(
          "aaa" -> "hello",
          "bbb" -> "[undefined]"
        )
      )
    }}}
    * 
    */
  case class ForceNewField(changer: Any) extends JValue {
    override def pp() = "ForceNewField("+J(changer).pp()+")"
    override def asJsValue = JsString(pp()) 
  }


  /**
    * Apply a change to a json value if the json value is defined.
    * 
    * @note Must be used if it is uncertain whether a field is present in an object.
    * 
    * @example
    {{{
        JsonChanger(
          J.obj(
            "aaa" -> 30
          ),
          J.obj(
            "aaa" -> Maybe(50)
          )
        ) ===
        J.obj(
          "aaa" -> 50
        )

        JsonChanger(
          J.obj(),
          J.obj(
            "aaa" -> Maybe(50)
          )
        ) ===
        J.obj()

        JsonChanger(
          null,
          Maybe(Func.number.number(_ + 1))
        ) ===
        JNull

        JsonChanger(
          90,
          Maybe(Func.number.number(_ + 1))
        ) ===
        91
      )
    }}}
    * 
    */
  case class Maybe(changer: Any) extends MaybeChanger {
    val j_changer = J(changer)
    override def pp() = "Maybe("+j_changer.pp()+")"

    def maybeTransform(json: JValue, path: String, allow_mismatched_types: Boolean): Option[Any] =
      if (json.isDefined)
        Some(JsonChanger.apply(json, changer, path, allow_mismatched_types))
      else
        None
  }

  /**
    * Map [[JsonChanger.apply]] on array
    * 
    * @example
    {{{
        JsonChanger(
          J.arr(2,3),
          MapChanger(Replace(3,9))
        ) ===
        J.arr(2,9)
    }}}
    * 
    */
  case class MapChanger(changer: Any) extends Changer {
    val j_changer = J(changer)
    override def pp() = "MapChanger "+j_changer.pp()

    private def change(n: Int, jsons: List[JValue], path: String): List[JValue] =
      if (jsons.isEmpty)
        List()
      else
        JsonChanger.apply(jsons.head, j_changer, path + "[" + n + "]", allow_mismatched_types = false) :: change(1+n, jsons.tail, path)

    def transform(json: JValue, path: String, allow_mismatched_types: Boolean) =
      if (json.isArray)
        J(change(0, json.asArray.toList, path))
      else
        throwChangeException("JsonChanger.MapChanger expects array. Found "+json.pp(), path)

  }


  /**
    * Allow other fields in an object than the ones specified in the changer.
    * 
    * @example
    {{{
        JsonChanger(
          J.obj(
            "aaa" -> 50
          ),
          J.obj(
            ___allowOtherFields
          )
        ) ===
        J.obj("aaa" -> 50)
    }}}
    * 
    */
  val ___allowOtherFields  = JsonMatcher.___allowOtherFields

  /**
    * Allow other values in an array than the ones specified in the changer.
    * Must be specified at the last position of a changer array.
    * 
    * @example
    {{{
        JsonChanger(
          J.arr(5),
          J.arr(
            ___allowOtherValues
          )
        ) ===
        J.arr(5)
    }}}
    * 
    */
  val ___allowOtherValues  = JsonMatcher.___allowOtherValues


  /**
    See [[___removeThisField]]
    */
  case class RemoveThisField(type_ : Type) extends JValue{
    override def pp() = "RemoveThisField("+type_.toString+")"
    override def asJsValue = JsString(pp())
  }

  /**
    * Removes a field from an object
    * 
    * @example
    * {{{
        JsonChanger(
          J.obj(
            "aaa" -> 50
          ),
          J.obj(
            "aaa" -> ___removeThisField.number
          )
        ) ===
        J.obj()
    * }}}
    * 
    * [[___removeThisField]] must be surrounded with [[Maybe]] if it's uncertain whether the field is present, in order to avoid [[JsonChangerException]] to be thrown:
    * {{{
      JsonMatcher.matchJson(
        JsonChanger(
          J.obj(),
          J.obj(
            "aaa" -> Maybe(___removeThisField.number)
          )
        ),
        J.obj()
      )
    * }}}
    */
  object ___removeThisField extends InputTypeTransformer[RemoveThisField] {
    protected def createFunc(input_type: Type) =
      RemoveThisField(input_type)
  }


  /**
    * Identity function changers
    * 
    * @example
    {{{
        JsonChanger(
          J.obj(
            "aaa" -> 10
          ),
          J.obj(
            "aaa" -> ___identity.number
          )
        ) === J.obj("aaa" -> 10)
    }}}
    * 
    */
  object ___identity extends InputTypeTransformer[Func]{
    protected def createFunc(input_type: Type) =
      Func(input_type, input_type, a => a)
  }


  /**
    * Insert new value into array
    * @example
    {{{
        JsonChanger(
          J.arr(
            2,
            3
          ),
          J.arr(
            ___identity.number,
            InsertValue(90),
            ___identity.number
          )
        ) ===
        J.arr(
          2,
          90,
          3
        )
    }}}
    * @note see also [[___removeValue]]
    */
  case class InsertValue(value: Any) extends JValue {
    val j_value = J(value)
    override def pp() = "InsertValue "+j_value.pp()
    override def asJsValue = JsString(pp())
  }

  /**
    See [[___removeValue]]
    */
  case class RemoveValue(type_ : Type) extends JValue{
    override def pp() = "RemoveValue("+type_.toString+")"
    override def asJsValue = JsString(pp())
  }

  /**
    * Remove value from array
    * @example
    {{{
        JsonChanger(
          J.arr(
            2,
            3
          ),
          J.arr(
            ___removeValue.number,
            ___identity.number
          )
        ) ===
        J.arr(
          3
        )
    }}}
    * @note see also [[InsertValue]]
    */
  object ___removeValue extends InputTypeTransformer[RemoveValue] {
    protected def createFunc(input_type: Type) =
      RemoveValue(input_type)
  }


  private def changeArray(json: JArray, changer: JArray, path: String): JValue = {

    val allowOthers = J(___allowOtherValues)

    def change(n: Int, jsons: List[JValue], changers: List[JValue]): List[JValue] = (jsons, changers) match {
      case (Nil, Nil) => List()

      case (_, (insert: InsertValue) :: rest) =>
        insert.j_value :: change(n+1, jsons, rest)

      case (Nil, _) =>
        throwChangeException("Too many changers in array: "+J(changers).pp(), path + "[" + n + "]")

      case (_, Nil) =>
        throwChangeException("Missing changer for "+jsons.head.pp(), path + "[" + n + "]")

      case (_, `allowOthers` :: Nil) =>
        jsons

      case (_, `allowOthers` :: rest) =>
        throwChangeException("Can not have values after ___allowOtherValues in an array: "+json.pp(), path)

      case (j::js, (removeValue: RemoveValue) :: rest) =>
        removeValue.type_.validate(j, path + "[" + n + "]")
        change(n, js, rest)

      case (j::js, c::cs) =>
        apply(j, c, path + "[" + n + "]") :: change(1+n, js, cs)
    }

    J(
      change(0, json.value.toList, changer.value.toList)
    )
  }

  // Needs cleanup
  private def changeObject(json: JObject, changer: JObject, path: String): JValue = {

    //println("changeObject. path: "+path)
    //println("changeObject. json: "+json.pp())
    //println("changeObject. changer: "+changer.pp())

    val allowOtherFields = changer.asMap.get(___allowOtherFields._1).isDefined

    def applyChanger(key: String, value: JValue, changer: JValue): (String, JValue) = changer match {
      case changeThisField: ChangeThisField =>
        applyChanger(changeThisField.newFieldName, value, changeThisField.j_changer)

      case newFieldChanger: NewField => throwChangeException("""NewField can not be used on existing field. If it's uncertain whether the field already exists, "ForceNewField" can be used instead.""", path+"."+key)

      case forceNewField: ForceNewField =>
        key -> apply(value, forceNewField.changer, path+"."+key)

      case maybeChanger: Maybe =>
        if (value.isDefined)
          applyChanger(key, value, maybeChanger.j_changer)
        else
          key -> value

      case removeThisField: RemoveThisField =>
        removeThisField.type_.validate(value, path+"."+key)
        key -> removeThisField

      case changer: JValue =>
        key -> apply(value, changer, path+"."+key)
    }

    var usedChangerKeys = Set[String]()

    // Create return value
    //
    val ret = json.asMap.map{
      case (key: String, value: JValue) => changer.asMap.get(key) match {

        case None =>
          if (allowOtherFields)
            key -> value
          else
            throwChangeException("No changer for key \""+key+"\" in "+changer.pp(), path)

        case Some(change) =>
          val res = applyChanger(key, value, change)
          usedChangerKeys += res._1
          res

      }
    }.filter(field =>
      !field._2.isInstanceOf[RemoveThisField]
    )

    val addedFieldsInChanger = changer.asMap.filter {
      case (key: String, _: NewField)      => true
      case (key: String, _: ForceNewField) => !ret.keys.toSet.contains(key)
      case _ => false
    }.map{
      case (key: String, newField: NewField)           => key -> newField.value
      case (key: String, forceNewField: ForceNewField) => key -> apply(JUndefined(key, json), forceNewField.changer, path+"."+key, allow_mismatched_types = true)
    }

    // Check that all changer keys are used (if there are bugs, it's probably better to rewrite than try to fix it)
    if(true){
      val changerKeys = changer.asMap.map{
        case (key: String, changeThisField: ChangeThisField) => changeThisField.newFieldName
        case (key: String, _) => key
      }.toSet

      val maybeChangerKeys = changer.asMap.filter{
        case (key: String, value: Maybe) => true
        case (key: String, changeThisField: ChangeThisField) if changeThisField.j_changer.isInstanceOf[Maybe] => true
        case _ => false
      }.map{
        case (key: String, changeThisField: ChangeThisField) => changeThisField.newFieldName
        case (key: String, _) => key
      }.toSet

      //println("changerKeys: "+changerKeys)
      //println("usedChangerKeys: "+usedChangerKeys)
      //println("maybeChangerKeys: "+maybeChangerKeys)

      val unusedChangerKeys = changerKeys -- usedChangerKeys -- maybeChangerKeys -- Set(___allowOtherFields._1) -- addedFieldsInChanger.keys

      if ( unusedChangerKeys.nonEmpty)
        throwChangeException("Unknown keys in changer: "+unusedChangerKeys.mkString(", "), path)
    }

    J(ret) ++ J(addedFieldsInChanger)
  }


  private def apply(json: JValue, changer: JValue, path: String, allow_mismatched_types: Boolean): JValue = {

    //println("JsonChanger.apply. path: "+path)

    def throwMatchException(match_type: String) =
      throwChangeException(s"$json is not a $match_type. You can use TypeChange on the changer to avoid this. value: "+json.pp()+", changer: "+changer.pp(), path)

    (json, changer, allow_mismatched_types) match {

      case (_,           c: Changer,   _)    => J(c.transform(json, path, allow_mismatched_types))

      case (j: JObject,  c: JObject,   _)    => changeObject(j, c, path)
      case (j: JArray,   c: JArray,    _)    => changeArray(j, c, path)

      case (_,           _,            true) => changer

      case (_: JNumber,    c: JNumber,    _) => c
      case (_: JString,    c: JString,    _) => c
      case (_: JBoolean,   c: JBoolean,   _) => c
      case (_: JUndefined, c: JUndefined, _) => c

      case (`JNull`,     `JNull`,        _)  => JNull
      case (_,           `JNull`,        _)  => throwMatchException("null")

      case (_,           _: JObject,     _)  => throwMatchException("object")
      case (_,           _: JArray,      _)  => throwMatchException("array")
      case (_,           _: JNumber,     _)  => throwMatchException("number")
      case (_,           _: JString,     _)  => throwMatchException("string")
      case (_,           _: JBoolean,    _)  => throwMatchException("boolean")
      case (_,           _: JUndefined,  _)  => throwMatchException("undefined")

      case (_,           _,              _)  => changer
    }

  }

  /**
    * Main function.
    * @param json_value the value to change
    * @param changer the pattern to change against
    * @param path Prepended to '''path''' value in [[JsonChangerException]] (if thrown).
    * @param allow_mismatched_types Allow mismatched types. Example:
    {{{
     
      // This example:

      JsonChanger(50, "hello", allow_mismatched_types = true)

      // ...works. However, this example:

      JsonChanger(J.arr(50), J.arr("hello"), allow_mismatched_types = true)

      // ...will fail independent of the value of 'allow_mismatched_types'.
      //
      // To allow mismatched types for array or object values, we must do this instead:

      JsonChanger(J.arr(50), J.arr(TypeChange.number.string("hello"))
    }}}
    * 
    The '''allow_mismatched_types''' parameter is exposed here since it must be handled manually in [[Changer.transform]]. In [[Changer.transform]], you sometimes want to call [[JsonChanger.apply]] and then '''allow_mismatched_types''' must be forwarded to avoid the changer to fail if the current [[Changer]] instance (i.e. '''this''') was surrounded with a [[TypeChange]] changer.
    */
  def apply(json_value: Any, changer: Any, path: String = "", allow_mismatched_types: Boolean = false): JValue =
    apply(J(json_value), J(changer), path, allow_mismatched_types)

}

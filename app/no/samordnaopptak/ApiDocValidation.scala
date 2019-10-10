package no.samordnaopptak.apidoc

import com.fasterxml.jackson.annotation.JsonIgnore

import play.api.Play.current

import com.google.inject.Inject

import no.samordnaopptak.json._


class ApiDocValidation @Inject() (
  environment: play.api.Environment
) {

  /**
    * See [[https://github.com/kmatheussen/Doppelauge/blob/master/API_DOC.md#fixing-runtime-exceptions API_DOC.md ]]
    */
  class MismatchPathParametersException(message: String) extends Exception(message)

  /**
    * See [[https://github.com/kmatheussen/Doppelauge/blob/master/API_DOC.md#fixing-runtime-exceptions API_DOC.md ]]
    */
  class MismatchFieldException(message: String) extends Exception(message)

  /**
    * See [[https://github.com/kmatheussen/Doppelauge/blob/master/API_DOC.md#fixing-runtime-exceptions API_DOC.md ]]
    */
  class AlreadyDefinedFieldException(message: String) extends Exception(message)

  /**
    * See [[https://github.com/kmatheussen/Doppelauge/blob/master/API_DOC.md#fixing-runtime-exceptions API_DOC.md ]]
    */
  class UnknownFieldException(message: String) extends Exception(message)

  /**
    * Trying to load which does not exist in path
    */
  class ClassNotFoundException(message: String) extends Exception(message)


  //play.api.Play.classloader.loadClass(className)

  private def safeLoadClassThroughDependencyInjection(className: String): java.lang.Class[_] =
    try{
      environment.classLoader.loadClass(className)
    } catch {
      case e: java.lang.ClassNotFoundException => null.asInstanceOf[java.lang.Class[_]]
    }

  private def safeLoadClass(className: String): java.lang.Class[_] =
    try{
      play.api.Play.classloader.loadClass(className)
     } catch {
        case e: java.lang.ClassNotFoundException => null.asInstanceOf[java.lang.Class[_]]
    }

  private def loadClassThroughDependencyInjection(fullClassName: String, className: String): java.lang.Class[_] =
    try{
      environment.classLoader.loadClass(className)
    } catch {
      case e: java.lang.ClassNotFoundException => throw new ClassNotFoundException("""The class with name """" + fullClassName + """" was not found""")
    }

  private def loadClass(fullClassName: String, className: String): java.lang.Class[_] =
    try{
      play.api.Play.classloader.loadClass(className)
    } catch {
      case e: java.lang.ClassNotFoundException => throw new ClassNotFoundException("""The class with name """" + fullClassName + """" was not found""")
    }


  /*

  null, List("lib", "ApiDoc", "MismatchFieldException") -> loadClass(
                                                              null,
                                                              List("lib.ApiDoc"), "MismatchFieldException"
                                                           )

  null, List("lib.ApiDoc", "MismatchFieldException")    -> loadClass(
                                                              play.api.Play.classloader.loadClass("lib.ApiDoc"),
                                                              List("MismatchFieldException")
                                                           )

  Class("lib.ApiDoc"), List("MismatchFieldException")   -> parent.getClasses.find(_.getCannonicalName()=="lib.ApiDoc.MimatchFieldException").get

   */
  private def loadInnerClass(parent: java.lang.Class[_], fullClassName: String, className: String, elms: List[String]): java.lang.Class[_] = {
    //println(className+": "+"parent: "+(if (parent==null) "null" else parent.getCanonicalName())+", elms: "+elms)

    if (parent==null && elms.isEmpty) {
      loadClass(fullClassName, className)

    } else if (parent==null) {
      val class_ = safeLoadClass(elms.head)
      if (class_ != null)
        loadInnerClass(class_, fullClassName, className, elms.tail)
      else if (elms.size==1)
        loadClass(fullClassName, className)
      else
        loadInnerClass(null, fullClassName, className, elms.head+"."+elms(1) :: elms.tail.tail)

    } else if (elms.isEmpty) {
      parent

    } else {
      val class_ = parent.getClasses.find(_.getCanonicalName==parent.getCanonicalName+"."+elms.head)
      if (class_.isDefined)
        loadInnerClass(class_.get, fullClassName, className, elms.tail)
      else
        loadClass(fullClassName, className)
    }
  }

  /**
    * Used internally.
 *
    * @example
    * {{{
  package here

  class Inner1{
    case class Inner2()
  }

  ApiDocValidation.loadInnerClass("here.Inner1.Inner2")
    * }}}
    */
  @deprecated("Only to support Play 2.4.x and static router, in the future use (To Be Implemented ...)")
  def loadInnerClass(className: String): java.lang.Class[_] =
    loadInnerClass(null.asInstanceOf[java.lang.Class[_]], className, className, className.split('.').toList)


  /**
    * Used internally. Validates that the fields for a datatype matches the content of a class.
    */
  def validateDataTypeFields(className: String, dataTypeName: String, fields: Set[String], addedFields: Set[String], removedFields: Set[String]): Unit = {

    val class_ = try{
      loadInnerClass(className)
    } catch {
      case e: ClassNotFoundException =>
        //println("couldn't load class "+className+". Trying models instead")
        loadInnerClass("models."+className)
    }

    def getClassFieldNames(parameterAnnotations: List[Array[java.lang.annotation.Annotation]], fields: List[java.lang.reflect.Field]): List[String] = {
      if (fields.isEmpty)
        List()
      else {
        val field = fields.head
        val annotations = if (parameterAnnotations.isEmpty) field.getDeclaredAnnotations.toList else parameterAnnotations.head.toList
        val rest = getClassFieldNames(parameterAnnotations.drop(1), fields.tail) // List().drop(1) == List()

        val fieldName = field.getName
        if(fieldName.contains("$"))
          rest
        else
          annotations.find(_.isInstanceOf[JsonIgnore]) match {
            case Some(annotation: JsonIgnore) if annotation.value => rest
            case None => fieldName :: rest
          }
      }
    }

    if(class_.getConstructors.isEmpty)
      throw new Exception(s"""While evaluating "$dataTypeName": Class $class_ does not have any constructors.""")

    val classFields = getClassFieldNames(
      class_.getConstructors.head.getParameterAnnotations.toList,
      class_.getDeclaredFields.toList
    ).toSet

    if ( (removedFields &~ classFields).nonEmpty)
      throw new UnknownFieldException(s"""While evaluating "$dataTypeName": One or more removedFields are not defined for class '$className': """ + (removedFields &~ classFields) + ". classFields: "+classFields + "\n(See README.md for more information)\n")

    if ( (addedFields & classFields).nonEmpty)
      throw new AlreadyDefinedFieldException(s"""While evaluating "$dataTypeName": One or more addedFields are already defined for class '$className': """+(addedFields & classFields)+"\n(See README.md for more information)\n")

    if ( (addedFields & removedFields).nonEmpty)
      throw new AlreadyDefinedFieldException(s"""While evaluating "$dataTypeName": One or more fields are both present in addedFields and removedFields (for '$className'): """+(addedFields & removedFields)+"\n(See README.md for more information)\n")

    val modifiedClassFields = classFields ++ addedFields -- removedFields

    if ( fields != modifiedClassFields)
      throw new MismatchFieldException(s"""While evaluating "$dataTypeName": The ApiDoc datatype does not match the class '$className'. Mismatched fields: """+ ((fields | modifiedClassFields) -- (fields & modifiedClassFields)) + "\n(See README.md for more information)\n")
  }

  /**
    * Used internally. Called by [[ApiDocParser.ApiDocs.validate]].
    */
  def validate(apiDoc: ApiDocParser.ApiDoc) = {
    val uriParms = apiDoc.methodAndUri.uriParms
    val parameters = apiDoc.parameters match{
      case None => ApiDocParser.Parameters(List())
      case Some(parameter) => parameter
    }
    val pathParms = parameters.fields.filter(_.paramType == ApiDocParser.ParamType.path)
    val pathParmKeys = pathParms.map(_.name)

    if (uriParms.size != pathParmKeys.size)
      throw new MismatchPathParametersException(s"""Mismatch between the number of parameters in the uri, and the number of path parameters.\nuriParms: $uriParms\npathParms:$pathParmKeys\njson: ${apiDoc.toJson}).""" + "\n(See README.md for more information)\n")

    pathParmKeys.foreach(pathParm =>
      if (!uriParms.contains(pathParm))
        throw new MismatchPathParametersException(s"""The path parameter "$pathParm" is not defined in the path.""" + "\n(See README.md for more information)\n")
    )
  }

}

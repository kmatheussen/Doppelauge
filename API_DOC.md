


API-Doc
=========

Non-cluttering swagger doc for scala playframework.

This API-Doc does the following validation checks:

  * All endpoints must be documented
  * The endpoints in the api docs must match the endpoints in conf/routes
  * All used datatypes must be defined
  * Datatypes must have corresponding scala classes.
  * The fields in datatypes must be the same as the fields in the corresponding scala classes.
  * Datatypes can only be defined once


ScalaDoc
========
http://users.notam02.no/~kjetism/doppelauge/#no.samordnaopptak.apidoc.package


QUICK START
===========

1. execute "activator run" in the terminal.

2. Go to this address in your browser:

   http://localhost:9000/swagger-ui/dist/index.html?url=http://localhost:9000/api/v1/api-docs



HOW TO USE IN YOUR OWN PROJECT (QUICK AND DIRTY)
================================================

1. Unless you have already done so, add "doppelauge" into build.sbt as instructed in README.md


2. If webjars is not a dependency in your project, add these lines to build.sbt:
   ```
   libraryDependencies ++= Seq(
      "org.webjars" %% "webjars-play" % "2.4.0-1"
   )
   ```


3. Add the following lines to conf/routes:
   ```
   GET   /webjars/*file    controllers.WebJarAssets.at(file)
   GET   /api/v1/api-docs  controllers.ApiDocController.get()
   ```
   
4. Create the controller:

   ```scala
  package controllers

  import play.api._
  import play.api.mvc._

  import no.samordnaopptak.apidoc.ApiDoc
  import no.samordnaopptak.apidoc.ApiDocUtil
  import no.samordnaopptak.json.J

  class ApiDocController extends Controller {

    // The required swagger info object (see https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#infoObject)
    val swaggerInfoObject = J.obj(
      "info" -> J.obj(
        "title"   -> "Generated Swagger API",
        "version" -> "1.0"
      )
    )

    @ApiDoc(doc="""
      GET /api/v1/api-docs
  
      DESCRIPTION
        Get main swagger json documentation
    """)
    def get() = Action {
      val generatedSwaggerDocs = ApiDocUtil.getSwaggerDocs()
      val json = generatedSwaggerDocs ++ swaggerInfoObject
      Ok(json.asJsValue)
    }
  }
  ```
  (also see app/controller/ApiDocController.scala)

5. Now the api-docs should be available at the following address:
   ```
   /webjars/doppelauge/1.0/swagger-ui/dist/index.html?url=/api/v1/api-docs
   ```



Examples
==========

Basic usage:


    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{id}/{type}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String <- The user id
        type: Enum(Woman, Man, Dog) String <- The user can either be a woman, a man, or a dog.

      RESULT
        200: String
        404: Any <- User not found
        400: Any <- Syntax Error
    """)
    def getUser(id: String) = ...



Define parameter types:

    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{phone}/{age}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String(path)                 <- 'path' parameter type (default, unless the parameter name is 'body')
        body: String                     <- 'body' parameter type.
        phone: String(query)             <- 'query' parameter type
        mobile: String(query, optional)  <- Optional 'query' parameter type
        email: String(form)              <- 'form' parameter type
        age: Enum(10,20,30,40) Int       <- Enum Int parameter. (also a 'path' parameter type)
    """)
    def getUser(id: String) = ...



Defining types:

    case class User(id: String, firstName: String)

    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{id}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String <- Parameter comment

      RESULT
        200: User
        404: User <- User not found
        400: User <- Syntax Error

      User:
        id: String
        firstName: String          <- The first name of the user.
        age: Enum(10,20,30,40) Int <- Age can be either 10, 20, 30 or 40
        nickNames: Array String    <- All nickNames in an array of strings
    """)
    def getUser(id: String) = ...





Defining types, based on an almost matching existing class:

    case class User(id: String, firstName: String)

    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{id}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String <- Parameter comment

      RESULT
        201: User
        404: Any <- User not found
        400: Any <- Syntax Error

      User(-firstName, +lastName):
        id: String
        lastName: String
    """)
    def getUser(id: String) = ...




Defining types, based on an almost matching existing class, using @JsonIgnore:

    import com.fasterxml.jackson.annotation._
    case class User(id: String, @JsonIgnore firstName: String)

    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{id}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String <- Parameter comment

      RESULT
        203: User
        404: Any <- User not found
        400: Any <- Syntax Error

      User(+lastName):
        id: String
        lastName: String
    """)
    def getUser(id: String) = ...




Defining types, based on a class defined elsewhere:

    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{id}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String <- Parameter comment

      RESULT
        204: User
        404: Any <- User not found
        400: Any <- Syntax Error

      User: models.user.User(-firstName, +lastName)
        id: String
        lastName: String
    """)
    def getUser(id: String) = ...




Defining types, not based on a class:

    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{id}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String <- Parameter comment

      RESULT
        205: User
        404: Any <- User not found
        400: Any <- Syntax Error

      User: !
        id: String
        lastName: String
    """)
    def getUser(id: String) = ...



Defining a type with unknown number of elements:

    @no.samordnaopptak.apidoc.ApiDoc(doc="""
      GET /api/v1/users/{id}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String <- Parameter comment

      RESULT
        206: User
        400: Any <- User not found
        401: Any <- Syntax Error

      User: !
        id: String
        lastName: String
        ...
    """)
    def getUser(id: String) = ...



See also the example controller: https://github.com/sun-opsys/doppelauge/blob/master/app/controllers/UserController.scala



Fixing runtime exceptions
=========================

Api-doc performs several validations during runtime.


* ```MissingMethodException```  :
  * Message: ```Missing ApiDoc for ${routeEntry.scalaClass}.${routeEntry.scalaMethod}```
  
    Solution 1: Add ApiDoc annotation for this method.
    
    Solution 2: Filter out this method from the list of route entries when calling the ```no.samordnaopptak.apidoc.ApiDocUtil.validate``` method:
    
    ```scala 
     val routeEntries =
       no.samordnaopptak.apidoc.RoutesHelper.getRouteEntries()
        .filter(_.scalaClass != classname && _.scalaMethod != methodname)
        
     no.samordnaopptak.apidoc.ApiDocUtil.validate(routeEntries = routeEntries)
    ```


* ```MethodMismatchException``` :
  * Message: ```Conflicting REST method declared in the autodoc and in conf/routes for ${routeEntry.scalaClass}.${routeEntry.scalaMethod}```
  
    Solution: Make sure the method for this endpoint in the routes file (```conf/routes```) matches with the api doc.
    For instance if the routes file has ```POST```, the api doc method also has to be ```POST```.
  


* ```UriMismatchException``` :
  * Message: ```Conflicting uri declared in the autodoc and in conf/routes for ${routeEntry.scalaClass}.${routeEntry.scalaMethod}```
  
    Solution: Make sure the uri for this endpoint in the routes file (```conf/routes```) matches with the api doc.
    For instance if the routes file has ```/api/v1/dosomething```, the api doc uri also has to be ```/api/v1/dosomething```.


* ```UnknownFieldException``` :
  * Message: ```While evaluating "${dataTypeName}": One or more removedFields are not defined for class '$className'```
  
    Example:
    ```scala
      package here
     
      case class User(name: String)

      object Controller {
      
        @ApiDoc(doc="""
          User: here.User(-email)
            name: String
        """)
        ...
      }
    ```
    The api-doc tells us that the type ```User``` matches the scala class ```User```, except for the ```email``` field. But the scala class ```User``` doesn't contain an ```email``` field.

* ```AlreadyDefinedFieldException``` :
  * Message: ```"While evaluating "${dataTypeName}": One or more addedFields are already defined for class '$className'```
  
    Example:
    ```scala
      package here
     
      case class User(name: String)

      object Controller {
      
        @ApiDoc(doc="""
          User: here.User(+name)
            name: String
        """)
        ...
      }
    ```
    The api-doc tells us that the type ```User``` also contains a ```name``` field, but this is not necessary to specify since the scala class ```User``` already contains a ```name``` field.

  * Message: ```While evaluating "${dataTypeName}": One or more fields are both present in addedFields and removedFields (for '$className')```
  
    Example:
    ```scala
      package here
     
      case class User(name: String)

      object Controller {
        @ApiDoc(doc="""
          User: here.User(+email,-email)
             name: String
        """)
        ...
      }
    ```
    Here, ```email``` is both added and removed from the type.


* ```MismatchFieldException``` :
  * Message: ```While evaluating "${dataTypeName}": The ApiDoc datatype does not match the class '$className'. Mismatched fields: ```
  
    Example:
    ```scala
      package here
     
      case class User(name: String)

      object Controller {
        @ApiDoc(doc="""
          User: here.User
             email: String
        """)
        ...
      }
    ```
    A clear mismatch.


* ```MismatchPathParametersException``` :
  * Message: ```Mismatch between the number of parameters in the uri, and the number of path parameters.```
  
    Example 1:
    ```
      GET /api/v1/{name}/{id}

      PARAMETERS 
        id: String
    ```
    In this example, the ```name``` parameter is missing under ```PARAMETERS```
    
    Example 2:
    ```scala
      POST /api/v1/user

      PARAMETERS 
        user: User        
      """)
    ```
    In this example, ```user``` is defined as a path parameter, while it should have been defined as a ```body``` parameter. Two ways to solve this problem are:
    
      * 1:
      ```
        POST /api/v1/user

        PARAMETERS 
          user: User (body)
      ```
      Here we define the "parameter type" for ```user``` manually by adding "(body)" after the type name. (By default, the parameter type has the value "path")
      
      * 2:
      ```
        POST /api/v1/user

        PARAMETERS 
          body: User
      ```
      Here the "parameter type" is set to ```body``` automatically since the default "parameter type" for a variable named `body` is "body".
      
      See https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#parameterObject for a list of parameter types.


  * Message: ```The path parameter "${pathParm}" is not defined in the path```
  
    Example:
    ```scala
      GET /api/v1/{name}

      DESCRIPTION
        Get user

      PARAMETERS 
        id: String
      """)
    ```
    Here the number of parameters matches, but there is a mismatch between `name` and `id`.


Notes
=========

* Scala classes must be specified with full path. (I.e. you have to write "controllers.User", not "User".)
  The only exception is if the classes is in the "models" package. (I.e. it is possible to write "User" instead of "models.User").

* In order to run validation checks on the api docs, it is necessary to call `ApiDocController.validate()`.
  If using specs2, it might be a good idea to put the following code (or something similar) into tests/ApiDocControllerSpec.scala:

  ```scala
    package test

    import org.specs2.mutable.Specification
    import play.api.test._
    import play.api.test.Helpers._

    import no.samordnaopptak.apidoc.{RoutesHelper, ApiDocUtil}

    class ApiDocControllerSpec extends Specification {
      "Validate swagger api docs" in {
          running(FakeApplication()){
            ApiDocUtil.validate()
          }
          true === true
      }
    }
  ```

  If there are end points defined in the "routes" file which have no api doc, you need to remove these before calling validate():

  ```scala
    class ApiDocControllerSpec extends Specification {
      "Validate swagger api docs" in {
          running(FakeApplication()){
            val routeEntries =
               no.samordnaopptak.apidoc.RoutesHelper.getRouteEntries()
                 .filter(_.scalaClass != "controllers.Assets") // no api-doc for the static assets files
            ApiDocUtil.validate(routeEntries)
          }
          true === true
      }
    }
  ```

  Also see test/ApiDocControllerSpec

* "INCLUDE" can be used to include annotations from another method:

  ```scala
    @ApiDoc(doc="""
      PARAMETERS 
        id: String (header) <- ID of the user

      INCLUDE test.Include.includedDocFunc
    """)
  ```
    

License
==========

Copyright 2013-2015 SUN/OPSYS at University of Oslo.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

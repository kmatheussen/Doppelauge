package no.samordnaopptak.apidoc

import com.google.inject.Inject

import no.samordnaopptak.json._


/**
  * High level interface to ApiDoc
  */
class ApiDocUtil @Inject() (
  annotationHelper: AnnotationHelper,
  routesHelper: RoutesHelper
) {

  /**
    * @return swagger 2.0 docs.
    * 
    * Implementation:
    * {{{
  def getSwaggerDocs(basePath: String = "/", apidocstrings: List[String] = AnnotationHelper.getApiDocsFromAnnotations()): JObject = {
    val apiDocs = ApiDocParser.getApiDocs(apidocstrings)
    val dataTypes = ApiDocParser.getDataTypes(apidocstrings)

    SwaggerUtil.getMain(basePath, apiDocs, dataTypes)
  }
    * }}}
    */
  def getSwaggerDocs(basePath: String = "/", apidocstrings: List[String] = annotationHelper.getApiDocsFromAnnotations()): JObject = {
    val apiDocs = ApiDocParser.getApiDocs(
      annotationHelper.apiDocValidator,
      apidocstrings
    )

    val dataTypes = ApiDocParser.getDataTypes(
      annotationHelper.apiDocValidator,
      apidocstrings
    )

    SwaggerUtil.getMain(basePath, apiDocs, dataTypes)
  }

  /**
    * Main validation function. Should be called from tests.
    *
    * @see [[https://github.com/kmatheussen/Doppelauge/blob/master/API_DOC.md#fixing-runtime-exceptions API_DOC.md ]] for instructions on how to handle runtime exceptions.
    */
  def validate(routeEntries: List[RouteEntry] = routesHelper.getRouteEntries()): Unit = {
    annotationHelper.validate(routeEntries)
    getSwaggerDocs("/")
  }
}

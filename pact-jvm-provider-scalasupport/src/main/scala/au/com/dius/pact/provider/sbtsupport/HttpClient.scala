package au.com.dius.pact.provider.sbtsupport

import au.com.dius.pact.provider.unfiltered.Conversions
import au.com.dius.pact.model.{Request, Response}
import au.com.dius.pact.provider.CollectionUtils
import com.typesafe.scalalogging.StrictLogging
import com.ning.http.client.FluentStringsMap
import dispatch.url

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}

object HttpClient extends StrictLogging {
  def run(request:Request)(implicit executionContext: ExecutionContext):Future[Response] = {
    logger.debug("request=" + request)
    val query = new FluentStringsMap()
    if (request.getQuery != null) {
      val queryMap = CollectionUtils.javaLMapToScalaLMap(request.getQuery)
      queryMap.foldLeft(query) {
        (fsm, q) => q._2.foldLeft(fsm) { (m, a) => m.add(q._1, a) }
      }
    }
    val headers = if (request.getHeaders == null) None
      else Some(JavaConversions.mapAsScalaMap(request.getHeaders))
    val r = url(request.getPath).underlying(
      _.setMethod(request.getMethod).setQueryParams(query)
    ) <:< headers.getOrElse(Map())
    val body = if (request.getBody != null) request.getBody.orElse("") else null
    val httpRequest = if (body != null) r.setBody(body) else r
    dispatch.Http(httpRequest).map(Conversions.dispatchResponseToPactResponse)
  }
}

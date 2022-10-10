package spp.jetbrains.marker.detect.endpoint

import com.intellij.microservices.url.UrlPath
import com.intellij.microservices.url.UrlResolveRequest
import com.intellij.microservices.url.UrlResolverManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import io.vertx.core.Future
import io.vertx.core.Promise
import spp.jetbrains.marker.source.info.EndpointDetector
import spp.jetbrains.marker.source.info.EndpointDetector.EndpointNameDeterminer
import spp.jetbrains.marker.source.mark.guide.GuideMark
import java.util.*

/**
 * Detects endpoints using IntelliJ's [UrlResolverManager] experimental functionality.
 *
 * Note: IntelliJ Ultimate only.
 */
class UrlResolverEndpointDetector(
    project: Project
) : EndpointDetector<EndpointNameDeterminer>(project), EndpointNameDeterminer {

    private val log = logger<UrlResolverEndpointDetector>()
    override val detectorSet = setOf(this)

    override fun determineEndpointName(guideMark: GuideMark): Future<Optional<DetectedEndpoint>> {
        val detectedEndpointPromise = Promise.promise<Optional<DetectedEndpoint>>()
        val targetPaths = ReadAction.compute(ThrowableComputable {
            UrlResolverManager.getInstance(guideMark.project).getVariants(
                UrlResolveRequest(null, null, UrlPath.fromExactString(""), null)
            )
        })
        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().runProcess({
                for (targetPath in targetPaths) {
                    if (targetPath.resolveToPsiElement() == guideMark.getPsiElement()) {
                        val endpointName = getEndpointName(targetPath.path)
                        val methodType = targetPath.methods.firstOrNull() ?: "GET" //todo: handle multiple methods
                        val fullEndpointName = "$methodType:$endpointName"
                        log.info("Detected endpoint: $fullEndpointName")

                        detectedEndpointPromise.complete(Optional.of(DetectedEndpoint(fullEndpointName, false)))
                        break
                    }
                }
                detectedEndpointPromise.tryComplete(Optional.empty())
            }, null)
        }
        return detectedEndpointPromise.future()
    }

    private fun getEndpointName(url: UrlPath): String {
        val urlPath = StringBuilder()
        url.segments.forEach {
            when (it) {
                is UrlPath.PathSegment.Exact -> urlPath.append("/").append(it.value)
                is UrlPath.PathSegment.Variable -> urlPath.append("/").append("{").append(it.variableName).append("}")

                else -> {
                    //todo: correctly handle this
                    urlPath.append("/").append("*")
                    log.warn("Unsupported url path segment type: ${it.javaClass}")
                }
            }
        }
        return urlPath.toString()
    }
}

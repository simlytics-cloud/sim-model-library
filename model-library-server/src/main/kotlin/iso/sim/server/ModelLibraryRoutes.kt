package iso.sim.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.pekko.http.javadsl.model.*
import org.apache.pekko.http.javadsl.server.AllDirectives
import org.apache.pekko.http.javadsl.server.PathMatchers
import org.apache.pekko.http.javadsl.server.Route

class ModelLibraryRoutes(
    private val service: ModelCatalogService,
    private val runService: RunService = RunService(service),
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
) : AllDirectives() {

    fun routes(): Route {
        return concat(
            path("openapi.yaml") {
                get {
                    val spec = loadResource("open-api/model-libary-open-api-spec.yaml")
                    complete(HttpEntities.create(ContentTypes.TEXT_PLAIN_UTF8, spec))
                }
            },
            path("swagger") {
                get {
                    complete(
                        HttpEntities.create(
                            ContentTypes.TEXT_HTML_UTF8,
                            """
                            <!doctype html>
                            <html lang="en">
                              <head>
                                <meta charset="utf-8" />
                                <meta name="viewport" content="width=device-width, initial-scale=1" />
                                <title>Model Library Swagger UI</title>
                                <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
                              </head>
                              <body>
                                <div id="swagger-ui"></div>
                                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js"></script>
                                <script>
                                  window.ui = SwaggerUIBundle({
                                    url: '/openapi.yaml',
                                    dom_id: '#swagger-ui',
                                    deepLinking: true,
                                    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                                    layout: 'BaseLayout'
                                  });
                                </script>
                              </body>
                            </html>
                            """.trimIndent()
                        )
                    )
                }
            },
            pathPrefix("v1") {
                concat(
                    path("models") {
                        get {
                            completeJson(service.listModels())
                        }
                    },
                    path(PathMatchers.segment("models").slash(PathMatchers.segment())) { modelId ->
                        get {
                            try {
                                completeJson(service.getModel(modelId))
                            } catch (_: ModelNotFoundException) {
                                complete(
                                    HttpResponse.create()
                                        .withStatus(StatusCodes.NOT_FOUND)
                                        .withEntity(
                                            ContentTypes.APPLICATION_JSON,
                                            objectMapper.writeValueAsString(
                                                ErrorResponse(code = "MODEL_NOT_FOUND", message = "Model '$modelId' was not found")
                                            )
                                        )
                                )
                            }
                        }
                    },
                    path(PathMatchers.segment("models").slash(PathMatchers.segment()).slash("run")) { modelId ->
                        put {
                            entity(org.apache.pekko.http.javadsl.unmarshalling.Unmarshaller.entityToString()) { requestBody ->
                                try {
                                    val request = objectMapper.readValue<StartModelRunRequest>(requestBody)
                                    val response = runService.startRun(modelId, request)
                                    complete(
                                        HttpResponse.create()
                                            .withStatus(StatusCodes.ACCEPTED)
                                            .addHeader(HttpHeader.parse("Location", response.statusUrl))
                                            .withEntity(
                                                ContentTypes.APPLICATION_JSON,
                                                objectMapper.writeValueAsString(response)
                                            )
                                    )
                                } catch (_: ModelNotFoundException) {
                                    completeError(StatusCodes.NOT_FOUND, "MODEL_NOT_FOUND", "Model '$modelId' was not found")
                                } catch (ex: InvalidRunRequestException) {
                                    completeError(StatusCodes.BAD_REQUEST, "INVALID_RUN_REQUEST", ex.message ?: "Invalid run request")
                                }
                            }
                        }
                    },
                    path(PathMatchers.segment("runs").slash(PathMatchers.segment())) { runId ->
                        get {
                            try {
                                completeJson(runService.getRunStatus(runId))
                            } catch (_: RunNotFoundException) {
                                completeError(StatusCodes.NOT_FOUND, "RUN_NOT_FOUND", "Run '$runId' was not found")
                            }
                        }
                    }
                )
            },
            pathPrefix("ui") {
                getFromResourceDirectory("web")
            },
            pathSingleSlash {
                redirect(Uri.create("ui/index.html"), StatusCodes.MOVED_PERMANENTLY)
            }
        )
    }

    private fun completeError(statusCode: StatusCode, code: String, message: String): Route {
        return complete(
            HttpResponse.create()
                .withStatus(statusCode)
                .withEntity(
                    ContentTypes.APPLICATION_JSON,
                    objectMapper.writeValueAsString(ErrorResponse(code = code, message = message))
                )
        )
    }

    private fun completeJson(payload: Any): Route {
        return complete(HttpEntities.create(ContentTypes.APPLICATION_JSON, objectMapper.writeValueAsString(payload)))
    }

    private fun loadResource(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("Required resource '$path' was not found")
        stream.use { return it.readBytes().toString(Charsets.UTF_8) }
    }
}

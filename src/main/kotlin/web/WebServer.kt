package web

import engine.ScraperEngine
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import models.FinalResponse
import models.ScrapeRequest
import models.ScrapeState

@Serializable
sealed interface WsMessage {
    @Serializable
    @kotlinx.serialization.SerialName("StateUpdate")
    data class StateUpdate(val state: ScrapeState) : WsMessage

    @Serializable
    @kotlinx.serialization.SerialName("FinalResult")
    data class FinalResult(val response: FinalResponse) : WsMessage
    
    @Serializable
    @kotlinx.serialization.SerialName("Error")
    data class Error(val message: String) : WsMessage
}

/**
 * Controller for the Web interface.
 */
class WebServer(private val engine: ScraperEngine) {
    fun start(port: Int = 8080) {
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            module(this@WebServer.engine)
        }.start(wait = true)
    }
}

fun Application.module(engine: ScraperEngine) {
    val jsonConfig = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    install(ContentNegotiation) {
        json(jsonConfig)
    }
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(jsonConfig)
        pingPeriod = kotlin.time.Duration.parse("15s")
        timeout = kotlin.time.Duration.parse("15s")
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        staticResources("/", "web") {
            default("index.html")
        }

        route("/api") {
            get("/scrapers") {
                val storeNames = engine.scrapers.map { it.storeName }
                call.respond(storeNames)
            }
        }
        
        webSocket("/api/scrape/progress") {
            try {
                val request = receiveDeserialized<ScrapeRequest>()
                
                val stateChannel = Channel<ScrapeState>(Channel.BUFFERED)
                val collectorJob = launch {
                    for (state in stateChannel) {
                        sendSerialized<WsMessage>(WsMessage.StateUpdate(state))
                    }
                }
                
                try {
                    val response = engine.runScrapers(request, request.isHeadless, stateChannel)
                    stateChannel.close()
                    collectorJob.join()
                    
                    sendSerialized<WsMessage>(WsMessage.FinalResult(response))
                } catch (e: Exception) {
                    stateChannel.close()
                    collectorJob.join()
                    sendSerialized<WsMessage>(WsMessage.Error(e.message ?: "Unknown error occurred during scraping"))
                }
            } catch (e: Exception) {
                sendSerialized<WsMessage>(WsMessage.Error("Failed to parse request: ${e.message}"))
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid request"))
            }
        }
    }
}

package web

import com.google.common.truth.Truth.assertThat
import engine.ScraperEngine
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.*
import org.junit.jupiter.api.Test
import scrapers.Scraper
import kotlin.test.assertTrue

class WebServerTest {

    private val jsonConfig = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun getScrapers_returnsListOfStoreNames() = testApplication {
        val fakeScraper = object : Scraper {
            override val storeName = "TestStore"
            override suspend fun scrape(
                context: com.microsoft.playwright.BrowserContext,
                request: ScrapeRequest,
                onState: suspend (models.ScrapeState) -> Unit
            ): ScrapeResult = ScrapeResult.Success(emptyList())
        }
        val engine = ScraperEngine(listOf(fakeScraper))
        application {
            module(engine)
        }

        val response = client.get("/api/scrapers")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).contains("TestStore")
    }

    @Test
    fun webSocketProgress_receivesRequestAndEmitsUpdatesAndFinalResponse() = testApplication {
        val fakeScraper = object : Scraper {
            override val storeName = "TestStore"
            override suspend fun scrape(
                context: com.microsoft.playwright.BrowserContext,
                request: ScrapeRequest,
                onState: suspend (models.ScrapeState) -> Unit
            ): ScrapeResult {
                onState(ScrapeState.Navigating("TestStore", "http://example.com"))
                return ScrapeResult.Success(listOf(Product("TestStore", 100, "http://example.com/item", "Item")))
            }
        }
        val engine = ScraperEngine(listOf(fakeScraper))
        application {
            module(engine)
        }

        val client = createClient {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(jsonConfig)
            }
        }

        client.webSocket("/api/scrape/progress") {
            val request = ScrapeRequest("milk", "10001", true, listOf("TestStore"))
            sendSerialized(request)

            val messages = incoming.consumeAsFlow().take(2).toList()

            // First message should be StateUpdate
            val msg1 = jsonConfig.decodeFromString<WsMessage>((messages[0] as Frame.Text).readText())
            assertTrue { msg1 is WsMessage.StateUpdate }
            assertThat((msg1 as WsMessage.StateUpdate).state).isEqualTo(ScrapeState.Navigating("TestStore", "http://example.com"))

            // Second message should be FinalResult
            val msg2 = jsonConfig.decodeFromString<WsMessage>((messages[1] as Frame.Text).readText())
            assertTrue { msg2 is WsMessage.FinalResult }
            assertThat((msg2 as WsMessage.FinalResult).response.results).hasSize(1)
            assertThat(msg2.response.results[0].productName).isEqualTo("Item")
        }
    }

    @Test
    fun webSocketProgress_whenScraperFails_emitsFailureInFinalResponse() = testApplication {
        val fakeScraper = object : Scraper {
            override val storeName = "TestStore"
            override suspend fun scrape(
                context: com.microsoft.playwright.BrowserContext,
                request: ScrapeRequest,
                onState: suspend (models.ScrapeState) -> Unit
            ): ScrapeResult {
                throw RuntimeException("Network Error")
            }
        }
        val engine = ScraperEngine(listOf(fakeScraper))
        application {
            module(engine)
        }

        val client = createClient {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(jsonConfig)
            }
        }

        client.webSocket("/api/scrape/progress") {
            val request = ScrapeRequest("milk", "10001", true, listOf("TestStore"))
            sendSerialized(request)

            val messages = incoming.consumeAsFlow().take(1).toList()

            // First message should be FinalResult because it failed immediately
            val msg1 = jsonConfig.decodeFromString<WsMessage>((messages[0] as Frame.Text).readText())
            assertTrue { msg1 is WsMessage.FinalResult }
            assertThat((msg1 as WsMessage.FinalResult).response.failures).hasSize(1)
            assertThat(msg1.response.failures[0].reason).isEqualTo("Network Error")
        }
    }
}

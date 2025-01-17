package dev.forst.ktor.apikey

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.server.application.call
import io.ktor.server.auth.Principal
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class TestApiKeyAuth {

    private data class ApiKeyPrincipal(val key: String) : Principal

    private val defaultHeader = "X-Api-Key"

    @Test
    fun `test apikey auth does not influence open routes`() {
        val apiKey = UUID.randomUUID().toString()

        val module = buildApplicationModule {
            validate { header -> header.takeIf { it == apiKey }?.let { ApiKeyPrincipal(it) } }
        }

        testApplication {
            application(module)
            val response = client.get(Routes.open) { }
            assertEquals(HttpStatusCode.OK, response.status)

            val response2 = client.get(Routes.open) {
                header(defaultHeader, apiKey)
            }
            assertEquals(HttpStatusCode.OK, response2.status)

            val response3 = client.get(Routes.open) {
                header(defaultHeader, "$apiKey-wrong")
            }
            assertEquals(HttpStatusCode.OK, response3.status)
        }
    }

    @Test
    fun `test reasonable defaults work`() {
        val apiKey = UUID.randomUUID().toString()

        val module = buildApplicationModule {
            validate { header -> header.takeIf { it == apiKey }?.let { ApiKeyPrincipal(it) } }
        }

        testApplication {
            application(module)
            val response = client.get(Routes.authenticated) {
                header(defaultHeader, apiKey)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val principal = response.body<ApiKeyPrincipal>()
            assertEquals(principal, ApiKeyPrincipal(apiKey))

            val response2 = client.get(Routes.authenticated) {
                header(defaultHeader, "$apiKey-wrong")
            }
            assertEquals(HttpStatusCode.Unauthorized, response2.status)
        }
    }

    @Test
    fun `test auth should accept valid api key`() {
        // use different from default code to verify that it actually works
        val errorStatus = HttpStatusCode.Conflict
        val header = "hello"
        val apiKey = "world"

        val module = buildApplicationModule {
            headerName = header
            challenge { call.respond(errorStatus) }
            validate { header -> header.takeIf { it == apiKey }?.let { ApiKeyPrincipal(it) } }
        }

        testApplication {
            application(module)
            val response = client.get(Routes.authenticated) {
                header(header, apiKey)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val principal = response.body<ApiKeyPrincipal>()
            assertEquals(principal, ApiKeyPrincipal(apiKey))
        }
    }

    @Test
    fun `test auth should accept reject invalid api key`() {
        val errorStatus = HttpStatusCode.Conflict
        val header = "hello"
        val apiKey = "world"

        val module = buildApplicationModule {
            headerName = header
            challenge { call.respond(errorStatus) }
            validate { header -> header.takeIf { it == apiKey }?.let { ApiKeyPrincipal(it) } }
        }

        testApplication {
            application(module)
            val response = client.get(Routes.authenticated) {
                header(header, apiKey)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val principal = response.body<ApiKeyPrincipal>()
            assertEquals(principal, ApiKeyPrincipal(apiKey))

            val response2 = client.get(Routes.authenticated) {
                header(header, "$apiKey-wrong")
            }
            assertEquals(errorStatus, response2.status)
        }
    }
}

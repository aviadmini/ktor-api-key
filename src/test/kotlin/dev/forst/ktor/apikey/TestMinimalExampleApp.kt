package dev.forst.ktor.apikey

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.server.application.Application
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestMinimalExampleApp {
    @Test
    fun `test minimal example app works as expected`() {
        testApplication {
            application(Application::minimalExample)

            val response = client.get("/") {}
            assertEquals(HttpStatusCode.Unauthorized, response.status)

            val response2 = client.get("/") {
                header("X-Api-Key", "this-is-expected-key")
            }
            assertEquals(HttpStatusCode.OK, response2.status)
            assertEquals("Key: this-is-expected-key", response2.body())
        }
    }
}

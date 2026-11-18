package com.etyhan.app.ai

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class LlmClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: LlmClient

    private val okBody = """
        {
          "model": "deepseek-chat",
          "choices": [{"message": {"role": "assistant", "content": "{\"t\":\"学校\"}"}, "finish_reason": "stop"}],
          "usage": {"prompt_tokens": 812, "completion_tokens": 96}
        }
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = LlmClient(AnalysisParser.json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun cfg(jsonMode: Boolean = true) = LlmConfig(
        baseUrl = server.url("/v1").toString(),
        apiKey = "sk-test-key",
        model = "deepseek-chat",
        jsonMode = jsonMode,
    )

    private fun enqueueOk() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(okBody),
        )
    }

    @Test
    fun sendsOpenAiCompatibleRequestAndParsesUsage() = runBlocking {
        enqueueOk()
        val completion = client.complete(cfg(), "system prompt", "user text")

        assertEquals("{\"t\":\"学校\"}", completion.content)
        assertEquals(812, completion.promptTokens)
        assertEquals(96, completion.completionTokens)
        assertEquals("stop", completion.finishReason)
        assertEquals("deepseek-chat", completion.model)

        val request = server.takeRequest()
        assertEquals("/v1/chat/completions", request.path)
        assertEquals("Bearer sk-test-key", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"stream\":false"))
        assertTrue(body.contains("response_format"))
        assertTrue(body.contains("\"max_tokens\":1200"))
        assertTrue(body.contains("system prompt"))
    }

    @Test
    fun retriesWithoutJsonModeWhenServerRejectsIt() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":{"message":"response_format is not supported for this model"}}"""),
        )
        enqueueOk()

        val completion = client.complete(cfg(), "system prompt", "user text")
        assertEquals("{\"t\":\"学校\"}", completion.content)

        assertEquals(2, server.requestCount)
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertTrue(first.body.readUtf8().contains("response_format"))
        assertFalse(second.body.readUtf8().contains("response_format"))
    }

    @Test
    fun mapsAuthErrorWithFriendlyMessage() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"invalid api key"}}"""),
        )
        try {
            client.complete(cfg(), "system", "user")
            fail("应该抛出 LlmException")
        } catch (e: LlmException) {
            assertEquals(401, e.status)
            assertTrue(e.message.contains("鉴权失败"))
            assertTrue(e.message.contains("invalid api key"))
            assertTrue(e.raw!!.contains("invalid api key"))
        }
    }

    @Test
    fun rejectsBlankBaseUrlBeforeNetwork() = runBlocking {
        try {
            client.complete(cfg().copy(baseUrl = "  "), "s", "u")
            fail("应该抛出 LlmException")
        } catch (e: LlmException) {
            assertTrue(e.message.contains("Base URL"))
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun emptyContentWithReasoningIsReported() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"choices":[{"message":{"content":"","reasoning_content":"思考中…"},"finish_reason":"length"}]}""",
            ),
        )
        try {
            client.complete(cfg(), "s", "u")
            fail("应该抛出 LlmException")
        } catch (e: LlmException) {
            assertTrue(e.message.contains("思考内容"))
        }
    }
}

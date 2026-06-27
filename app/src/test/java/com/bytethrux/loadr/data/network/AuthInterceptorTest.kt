package com.bytethrux.loadr.data.network

import com.bytethrux.loadr.data.local.TokenDataStore
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for the 401-handling interceptor logic in RetrofitClient.
 *
 * The critical invariant: a stale 401 (sent with an old token that has since
 * been replaced by a fresh login) must NOT wipe the current valid token.
 * Only clear the token when the request's token matches the currently-stored one.
 */
class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenDataStore: TokenDataStore

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenDataStore = mockk()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildClientWith(currentToken: String?): OkHttpClient {
        every { tokenDataStore.accessToken } returns flowOf(currentToken)
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (response.code == 401) {
                    val requestToken = request.header("Authorization")?.removePrefix("Bearer ")
                    val storedToken = runBlocking { tokenDataStore.accessToken.first() }
                    if (requestToken != null && requestToken == storedToken) {
                        runBlocking { tokenDataStore.clearToken() }
                    }
                }
                response
            }
            .build()
    }

    // ------------------------------------------------------------------
    // 401 scenarios
    // ------------------------------------------------------------------

    @Test
    fun `401 with matching token clears stored token`() {
        val token = "current_valid_token"
        coEvery { tokenDataStore.clearToken() } just Runs

        server.enqueue(MockResponse().setResponseCode(401))
        buildClientWith(token)
            .newCall(Request.Builder().url(server.url("/api")).header("Authorization", "Bearer $token").build())
            .execute().close()

        coVerify(exactly = 1) { tokenDataStore.clearToken() }
    }

    @Test
    fun `401 with stale token does not clear the fresh stored token`() {
        val staleToken = "old_pre_login_token"
        val freshToken = "new_post_login_token"

        server.enqueue(MockResponse().setResponseCode(401))
        buildClientWith(freshToken)
            .newCall(Request.Builder().url(server.url("/api")).header("Authorization", "Bearer $staleToken").build())
            .execute().close()

        // clearToken() must never be called — the fresh token must survive
        coVerify(exactly = 0) { tokenDataStore.clearToken() }
    }

    @Test
    fun `401 without Authorization header does not clear stored token`() {
        server.enqueue(MockResponse().setResponseCode(401))
        buildClientWith("some_token")
            .newCall(Request.Builder().url(server.url("/api")).build())
            .execute().close()

        coVerify(exactly = 0) { tokenDataStore.clearToken() }
    }

    // ------------------------------------------------------------------
    // Non-401 scenarios: token must never be touched
    // ------------------------------------------------------------------

    @Test
    fun `200 response never clears stored token`() {
        val token = "valid_token"
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        buildClientWith(token)
            .newCall(Request.Builder().url(server.url("/api")).header("Authorization", "Bearer $token").build())
            .execute().close()

        coVerify(exactly = 0) { tokenDataStore.clearToken() }
    }

    @Test
    fun `403 response never clears stored token`() {
        val token = "valid_token"
        server.enqueue(MockResponse().setResponseCode(403))
        buildClientWith(token)
            .newCall(Request.Builder().url(server.url("/api")).header("Authorization", "Bearer $token").build())
            .execute().close()

        coVerify(exactly = 0) { tokenDataStore.clearToken() }
    }

    @Test
    fun `500 response never clears stored token`() {
        val token = "valid_token"
        server.enqueue(MockResponse().setResponseCode(500))
        buildClientWith(token)
            .newCall(Request.Builder().url(server.url("/api")).header("Authorization", "Bearer $token").build())
            .execute().close()

        coVerify(exactly = 0) { tokenDataStore.clearToken() }
    }
}

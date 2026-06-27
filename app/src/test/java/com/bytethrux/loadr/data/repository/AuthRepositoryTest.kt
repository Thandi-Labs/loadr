package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.TokenResponse
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.net.ConnectException

class AuthRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        api = mockk()
        tokenDataStore = mockk()
        every { tokenDataStore.accessToken } returns flowOf(null)
        every { tokenDataStore.username } returns flowOf(null)
        repository = AuthRepository(api, tokenDataStore)
    }

    // ------------------------------------------------------------------
    // login()
    // ------------------------------------------------------------------

    @Test
    fun `login success stores token and username then returns Success`() = runTest {
        coEvery { api.login("julius", "secret") } returns TokenResponse("tok_abc", "bearer")
        coEvery { tokenDataStore.saveToken(any()) } just Runs
        coEvery { tokenDataStore.saveUsername(any()) } just Runs

        val result = repository.login("julius", "secret")

        assertTrue(result is AuthResult.Success)
        coVerify { tokenDataStore.saveToken("tok_abc") }
        coVerify { tokenDataStore.saveUsername("julius") }
    }

    @Test
    fun `login 401 returns invalid credentials message`() = runTest {
        val ex = mockk<HttpException> { every { code() } returns 401 }
        coEvery { api.login(any(), any()) } throws ex

        val result = repository.login("julius", "wrong")

        assertTrue(result is AuthResult.Error)
        assertEquals("Invalid username or password", (result as AuthResult.Error).message)
    }

    @Test
    fun `login 422 returns check credentials message`() = runTest {
        val ex = mockk<HttpException> { every { code() } returns 422 }
        coEvery { api.login(any(), any()) } throws ex

        val result = repository.login("julius", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals("Please check your credentials", (result as AuthResult.Error).message)
    }

    @Test
    fun `login 500 returns server error with code`() = runTest {
        val ex = mockk<HttpException> { every { code() } returns 500 }
        coEvery { api.login(any(), any()) } throws ex

        val result = repository.login("julius", "pass")

        assertTrue(result is AuthResult.Error)
        assertTrue((result as AuthResult.Error).message.contains("500"))
    }

    @Test
    fun `login ConnectException returns cannot reach server message`() = runTest {
        coEvery { api.login(any(), any()) } throws ConnectException("refused")

        val result = repository.login("julius", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals("Cannot reach server. Check your connection.", (result as AuthResult.Error).message)
    }

    @Test
    fun `login generic exception returns its message`() = runTest {
        coEvery { api.login(any(), any()) } throws Exception("Unexpected boom")

        val result = repository.login("julius", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals("Unexpected boom", (result as AuthResult.Error).message)
    }

    // ------------------------------------------------------------------
    // logout()
    // ------------------------------------------------------------------

    @Test
    fun `logout delegates to tokenDataStore clearToken`() = runTest {
        coEvery { tokenDataStore.clearToken() } just Runs

        repository.logout()

        coVerify(exactly = 1) { tokenDataStore.clearToken() }
    }

    // ------------------------------------------------------------------
    // savedToken / savedUsername
    // ------------------------------------------------------------------

    @Test
    fun `savedToken emits value from tokenDataStore`() = runTest {
        // AuthRepository captures the flow at construction time, so we must
        // stub before creating the repository for this specific test.
        every { tokenDataStore.accessToken } returns flowOf("my_token")
        val repo = AuthRepository(api, tokenDataStore)

        val collected = mutableListOf<String?>()
        repo.savedToken.collect { collected.add(it) }

        assertEquals(listOf("my_token"), collected)
    }

    @Test
    fun `savedUsername emits value from tokenDataStore`() = runTest {
        every { tokenDataStore.username } returns flowOf("julius")
        val repo = AuthRepository(api, tokenDataStore)

        val collected = mutableListOf<String?>()
        repo.savedUsername.collect { collected.add(it) }

        assertEquals(listOf("julius"), collected)
    }
}

package com.bytethrux.loadr.ui.auth

import com.bytethrux.loadr.data.repository.AuthRepository
import com.bytethrux.loadr.data.repository.AuthResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        every { repository.savedToken } returns flowOf(null)
        every { repository.savedUsername } returns flowOf(null)
        viewModel = AuthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------
    // Initial state
    // ------------------------------------------------------------------

    @Test
    fun `initial state when no stored token is not logged in`() {
        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.token)
    }

    @Test
    fun `stored token causes isLoggedIn true with username`() {
        every { repository.savedToken } returns flowOf("stored_tok")
        every { repository.savedUsername } returns flowOf("julius")
        viewModel = AuthViewModel(repository)

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertEquals("julius", viewModel.uiState.value.username)
        assertEquals("stored_tok", viewModel.uiState.value.token)
    }

    @Test
    fun `null stored token keeps isLoggedIn false`() {
        every { repository.savedToken } returns flowOf(null)
        every { repository.savedUsername } returns flowOf(null)
        viewModel = AuthViewModel(repository)

        assertFalse(viewModel.uiState.value.isLoggedIn)
    }

    // ------------------------------------------------------------------
    // login()
    // ------------------------------------------------------------------

    @Test
    fun `login success sets isLoggedIn true and clears loading`() = runTest {
        coEvery { repository.login("julius", "pass") } returns AuthResult.Success

        viewModel.login("julius", "pass")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login error sets errorMessage and clears loading`() = runTest {
        coEvery { repository.login(any(), any()) } returns AuthResult.Error("Invalid username or password")

        viewModel.login("julius", "wrongpass")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Invalid username or password", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login sets isLoading true during the request`() = runTest {
        coEvery { repository.login(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(500)
            AuthResult.Success
        }

        viewModel.login("julius", "pass")
        // UnconfinedTestDispatcher runs eagerly up to the first suspension (the delay)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `login clears previous error before attempting`() = runTest {
        // First call: error
        coEvery { repository.login(any(), any()) } returns AuthResult.Error("Bad credentials")
        viewModel.login("julius", "bad")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Second call: success
        coEvery { repository.login(any(), any()) } returns AuthResult.Success
        viewModel.login("julius", "good")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ------------------------------------------------------------------
    // logout()
    // ------------------------------------------------------------------

    @Test
    fun `logout delegates to repository and resets state`() = runTest {
        coEvery { repository.logout() } just Runs

        viewModel.logout()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.logout() }
        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertNull(viewModel.uiState.value.token)
        assertNull(viewModel.uiState.value.username)
    }

    // ------------------------------------------------------------------
    // clearError()
    // ------------------------------------------------------------------

    @Test
    fun `clearError sets errorMessage to null`() = runTest {
        coEvery { repository.login(any(), any()) } returns AuthResult.Error("Some error")
        viewModel.login("julius", "pass")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}

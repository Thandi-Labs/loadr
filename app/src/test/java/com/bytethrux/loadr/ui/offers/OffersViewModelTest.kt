package com.bytethrux.loadr.ui.offers

import app.cash.turbine.test
import com.bytethrux.loadr.data.network.OfferDto
import com.bytethrux.loadr.data.network.OfferType
import com.bytethrux.loadr.data.repository.OffersRepository
import com.bytethrux.loadr.data.repository.OffersResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OffersViewModelTest {

    private lateinit var repository: OffersRepository
    private lateinit var viewModel: OffersViewModel
    private val dispatcher = UnconfinedTestDispatcher()

    private val dataOffer = OfferDto(1, "250MB 24HRS", "*544*1*3#", 20.0, true, OfferType.DATA)
    private val smsOffer = OfferDto(2, "200SMS 24HRS", "*544*54*LD*1*1#", 20.0, true, OfferType.SMS)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        viewModel = OffersViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------
    // ViewModel initialisation: must NOT pre-fetch offers
    // ------------------------------------------------------------------

    @Test
    fun `init does not trigger getOffers automatically`() {
        coVerify(exactly = 0) { repository.getOffers() }
    }

    @Test
    fun `initial state is empty with no loading or error`() {
        val state = viewModel.uiState.value
        assertEquals(emptyList<OfferDto>(), state.offers)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertFalse(state.isActionInProgress)
    }

    // ------------------------------------------------------------------
    // refresh()
    // ------------------------------------------------------------------

    @Test
    fun `refresh success loads offers and clears errorMessage`() = runTest {
        coEvery { repository.getOffers() } returns OffersResult.Success(listOf(dataOffer))

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(dataOffer), state.offers)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `refresh clears a previous error when it succeeds`() = runTest {
        coEvery { repository.getOffers() } returns OffersResult.Error("401 Unauthorized")
        viewModel.refresh()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        coEvery { repository.getOffers() } returns OffersResult.Success(listOf(dataOffer))
        viewModel.refresh()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(dataOffer), viewModel.uiState.value.offers)
    }

    @Test
    fun `refresh error sets errorMessage and keeps isLoading false`() = runTest {
        coEvery { repository.getOffers() } returns OffersResult.Error("Network error")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("Network error", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `refresh clears errorMessage at the start of each call`() = runTest {
        coEvery { repository.getOffers() } returns OffersResult.Error("old error")
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals("old error", viewModel.uiState.value.errorMessage)

        // Make the second call pause so we can observe the intermediate state.
        // UnconfinedTestDispatcher runs the coroutine eagerly up to the first delay
        // suspension, so the first state update (errorMessage = null) is visible
        // before the repository call returns.
        coEvery { repository.getOffers() } coAnswers {
            kotlinx.coroutines.delay(500)
            OffersResult.Success(listOf(dataOffer))
        }
        viewModel.refresh()

        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ------------------------------------------------------------------
    // createOffer()
    // ------------------------------------------------------------------

    @Test
    fun `createOffer success emits ShowSnackbar and refreshes list`() = runTest {
        coEvery { repository.createOffer(any()) } returns OffersResult.Success(Unit)
        coEvery { repository.getOffers() } returns OffersResult.Success(listOf(dataOffer))

        viewModel.eventFlow.test {
            viewModel.createOffer("250MB 24HRS", 20.0, OfferType.DATA, "*544*1*3#")
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Offer created successfully", event.message)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { repository.getOffers() }
    }

    @Test
    fun `createOffer error emits error message via ShowSnackbar`() = runTest {
        coEvery { repository.createOffer(any()) } returns OffersResult.Error("Validation failed")

        viewModel.eventFlow.test {
            viewModel.createOffer("250MB 24HRS", 20.0, OfferType.DATA, "*544*1*3#")
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Validation failed", event.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createOffer builds OfferDto with active true and zero id`() = runTest {
        val capturedOffer = slot<OfferDto>()
        coEvery { repository.createOffer(capture(capturedOffer)) } returns OffersResult.Success(Unit)
        coEvery { repository.getOffers() } returns OffersResult.Success(emptyList())

        viewModel.eventFlow.test {
            viewModel.createOffer("200SMS", 15.0, OfferType.SMS, "*111#")
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        with(capturedOffer.captured) {
            assertEquals(0, id)
            assertEquals("200SMS", offer_name)
            assertEquals(15.0, amount, 0.001)
            assertEquals(OfferType.SMS, category)
            assertEquals("*111#", ussd)
            assertTrue(active)
        }
    }

    // ------------------------------------------------------------------
    // updateOffer()
    // ------------------------------------------------------------------

    @Test
    fun `updateOffer success emits ShowSnackbar and refreshes`() = runTest {
        coEvery { repository.updateOffer(dataOffer) } returns OffersResult.Success(Unit)
        coEvery { repository.getOffers() } returns OffersResult.Success(emptyList())

        viewModel.eventFlow.test {
            viewModel.updateOffer(dataOffer)
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Offer updated successfully", event.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateOffer error emits error snackbar without refreshing`() = runTest {
        coEvery { repository.updateOffer(any()) } returns OffersResult.Error("Update failed")

        viewModel.eventFlow.test {
            viewModel.updateOffer(dataOffer)
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Update failed", event.message)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { repository.getOffers() }
    }

    // ------------------------------------------------------------------
    // deleteOffer()
    // ------------------------------------------------------------------

    @Test
    fun `deleteOffer success emits ShowSnackbar and refreshes`() = runTest {
        coEvery { repository.deleteOffer(1) } returns OffersResult.Success(Unit)
        coEvery { repository.getOffers() } returns OffersResult.Success(emptyList())

        viewModel.eventFlow.test {
            viewModel.deleteOffer(1)
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Offer deleted successfully", event.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteOffer error emits error snackbar`() = runTest {
        coEvery { repository.deleteOffer(any()) } returns OffersResult.Error("Delete failed")

        viewModel.eventFlow.test {
            viewModel.deleteOffer(1)
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Delete failed", event.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------
    // toggleOfferStatus()
    // ------------------------------------------------------------------

    @Test
    fun `toggleOfferStatus active offer calls deactivateOffer`() = runTest {
        coEvery { repository.deactivateOffer(1) } returns OffersResult.Success(Unit)
        coEvery { repository.getOffers() } returns OffersResult.Success(emptyList())

        viewModel.eventFlow.test {
            viewModel.toggleOfferStatus(dataOffer.copy(active = true))
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Offer deactivated", event.message)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { repository.deactivateOffer(1) }
        coVerify(exactly = 0) { repository.activateOffer(any()) }
    }

    @Test
    fun `toggleOfferStatus inactive offer calls activateOffer`() = runTest {
        coEvery { repository.activateOffer(1) } returns OffersResult.Success(Unit)
        coEvery { repository.getOffers() } returns OffersResult.Success(emptyList())

        viewModel.eventFlow.test {
            viewModel.toggleOfferStatus(dataOffer.copy(active = false))
            advanceUntilIdle()

            val event = awaitItem() as OffersUiEvent.ShowSnackbar
            assertEquals("Offer activated", event.message)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { repository.activateOffer(1) }
        coVerify(exactly = 0) { repository.deactivateOffer(any()) }
    }

    // ------------------------------------------------------------------
    // isActionInProgress flag
    // ------------------------------------------------------------------

    @Test
    fun `isActionInProgress is true during CRUD operations and false after`() = runTest {
        // Pause the repository call so the coroutine suspends mid-way.
        // UnconfinedTestDispatcher runs the launch eagerly up to the first suspension,
        // so isActionInProgress = true is visible before advanceUntilIdle().
        coEvery { repository.createOffer(any()) } coAnswers {
            kotlinx.coroutines.delay(500)
            OffersResult.Success(Unit)
        }
        coEvery { repository.getOffers() } returns OffersResult.Success(emptyList())

        viewModel.eventFlow.test {
            viewModel.createOffer("Test", 10.0, OfferType.DATA, "*111#")
            assertTrue(viewModel.uiState.value.isActionInProgress)

            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isActionInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

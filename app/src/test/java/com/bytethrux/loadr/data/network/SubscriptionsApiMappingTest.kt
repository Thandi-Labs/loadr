package com.bytethrux.loadr.data.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Verifies Gson mapping against FastAPI-shaped JSON for the subscriptions
 * endpoints (Numeric prices arrive as JSON numbers, datetimes as ISO strings).
 */
class SubscriptionsApiMappingTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `plan list maps ids names prices and durations`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  {"id":1,"name":"Daily Subscription","description":"Unlimited Daily",
                   "request_count":1000,"price":30.0,"duration_days":1,"active":true},
                  {"id":4,"name":"300 USSD Requests","description":"1 Ksh=6 USSDs",
                   "request_count":300,"price":50.0,"duration_days":30,"active":true}
                ]
                """.trimIndent()
            )
        )

        val plans = api.getSubscriptionPlans()

        assertEquals(2, plans.size)
        assertEquals("Daily Subscription", plans[0].name)
        assertEquals(30.0, plans[0].price, 0.001)
        assertEquals(1, plans[0].duration_days)
        assertEquals(300, plans[1].request_count)
    }

    @Test
    fun `my-subscription maps requests remaining and iso dates`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"id":11,"user_id":7,"subscription_id":4,"requests_remaining":287,
                 "start_date":"2026-07-05T08:00:00.123456",
                 "expiry_date":"2026-08-04T08:00:00.123456"}
                """.trimIndent()
            )
        )

        val sub = api.getMySubscription("Bearer t")

        assertEquals(287, sub.requests_remaining)
        assertEquals("2026-08-04T08:00:00.123456", sub.expiry_date)
        assertEquals(4, sub.subscription_id)
    }
}

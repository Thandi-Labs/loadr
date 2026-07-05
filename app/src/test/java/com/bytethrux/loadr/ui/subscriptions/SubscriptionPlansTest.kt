package com.bytethrux.loadr.ui.subscriptions

import com.bytethrux.loadr.data.network.SubscriptionPlanDto
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionPlansTest {

    private fun plan(price: Double) = SubscriptionPlanDto(
        id = 1,
        name = "Plan",
        description = "Desc",
        request_count = 300,
        price = price,
        duration_days = 30,
    )

    @Test
    fun `activation code follows the star-140 pattern with the plan amount`() {
        assertEquals("*140*30*0768585724#", SubscriptionsViewModel.activationCode(plan(30.0)))
        assertEquals("*140*900*0768585724#", SubscriptionsViewModel.activationCode(plan(900.0)))
    }

    @Test
    fun `decimal backend prices dial as whole shillings`() {
        assertEquals("*140*50*0768585724#", SubscriptionsViewModel.activationCode(plan(50.00)))
    }
}

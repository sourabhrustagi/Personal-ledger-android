package com.ledger.personal.data.remote.mock

import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockResponsesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun allMockResponsesAreValidJson() {
        val allResponses = listOf(
            MockResponses.SESSION,
            MockResponses.OTP,
            MockResponses.TRANSACTIONS_LIST,
            MockResponses.TRANSACTION_DETAIL,
            MockResponses.DELETE_TRANSACTION,
            MockResponses.STATIC_CATEGORIES,
            MockResponses.STATIC_VERSION,
            MockResponses.STATIC_STATUS,
            MockResponses.DASHBOARD,
            MockResponses.PAY_SHORTCUTS,
            MockResponses.CAMPAIGN_SHELVES,
            MockResponses.BILL_PAY_KEY,
            MockResponses.BILLS_LOOKUP,
            MockResponses.BILLS_PAY,
            MockResponses.DEVICE_CHALLENGE,
            MockResponses.DEVICE_BIND,
        )

        for (rawJson in allResponses) {
            assertTrue(rawJson.isNotBlank())
            val element = json.parseToJsonElement(rawJson)
            assertNotNull(element)
        }
    }

    @Test
    fun responseForResolvesCorrectEndpoints() {
        assertNotNull(MockResponses.responseFor("/session"))
        assertNotNull(MockResponses.responseFor("/auth/otp"))
        assertNotNull(MockResponses.responseFor("/transactions", "GET"))
        assertNotNull(MockResponses.responseFor("/transactions", "POST"))
        assertNotNull(MockResponses.responseFor("/transactions/tx-1", "DELETE"))
        assertNotNull(MockResponses.responseFor("/static/categories"))
        assertNotNull(MockResponses.responseFor("/static/version"))
        assertNotNull(MockResponses.responseFor("/static/status"))
        assertNotNull(MockResponses.responseFor("/dashboard"))
        assertNotNull(MockResponses.responseFor("/pay/shortcuts"))
        assertNotNull(MockResponses.responseFor("/campaigns/shelves"))
        assertNotNull(MockResponses.responseFor("/pay/key"))
        assertNotNull(MockResponses.responseFor("/bills/lookup"))
        assertNotNull(MockResponses.responseFor("/bills/pay"))
        assertNotNull(MockResponses.responseFor("/devices/challenge"))
        assertNotNull(MockResponses.responseFor("/devices/bind"))
    }
}

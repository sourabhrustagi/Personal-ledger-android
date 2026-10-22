package com.ledger.personal.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiEndpointsTest {

    @Test
    fun baseUrlsAreConfiguredAndValid() {
        assertTrue(ApiEndpoints.BaseUrl.MOCK_API.startsWith("http://"))
        assertTrue(ApiEndpoints.BaseUrl.DEV_API.startsWith("https://"))
        assertTrue(ApiEndpoints.BaseUrl.STAGING_API.startsWith("https://"))
        assertTrue(ApiEndpoints.BaseUrl.PROD_API.startsWith("https://"))
    }

    @Test
    fun endpointsAreNonEmptyAndConsistent() {
        assertEquals("session", ApiEndpoints.SESSION)
        assertEquals("auth/otp", ApiEndpoints.OTP)
        assertEquals("transactions", ApiEndpoints.TRANSACTIONS)
        assertEquals("static/categories", ApiEndpoints.STATIC_CATEGORIES)
        assertEquals("static/version", ApiEndpoints.STATIC_VERSION)
        assertEquals("static/status", ApiEndpoints.STATIC_STATUS)
        assertEquals("dashboard", ApiEndpoints.DASHBOARD)
        assertEquals("pay/shortcuts", ApiEndpoints.PAY_SHORTCUTS)
        assertEquals("campaigns/shelves", ApiEndpoints.CAMPAIGN_SHELVES)
        assertEquals("pay/key", ApiEndpoints.BILL_PAY_KEY)
        assertEquals("bills/lookup", ApiEndpoints.BILLS_LOOKUP)
        assertEquals("bills/pay", ApiEndpoints.BILLS_PAY)
        assertEquals("devices/challenge", ApiEndpoints.DEVICE_CHALLENGE)
        assertEquals("devices/bind", ApiEndpoints.DEVICE_BIND)
    }
}

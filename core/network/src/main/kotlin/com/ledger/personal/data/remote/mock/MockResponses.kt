package com.ledger.personal.data.remote.mock

import com.ledger.personal.data.remote.ApiEndpoints

/**
 * Centralized mock JSON responses for all HTTP endpoints.
 * All mock responses are represented strictly as valid JSON payloads.
 */
object MockResponses {

    /** Session authentication response JSON. */
    val SESSION: String = """
        {
          "token": "mock-session"
        }
    """.trimIndent()

    /** Phone OTP verification response JSON. */
    val OTP: String = """
        {
          "message": "OTP sent",
          "status": "SENT"
        }
    """.trimIndent()

    /** Transactions list query response JSON. */
    val TRANSACTIONS_LIST: String = """
        {
          "items": [
            {
              "id": "tx-mock-1",
              "amount_cents": 125000,
              "currency": "INR",
              "category": "Dining",
              "description": "Starbucks Coffee",
              "timestamp": 1729500000000
            },
            {
              "id": "tx-mock-2",
              "amount_cents": 450000,
              "currency": "INR",
              "category": "Groceries",
              "description": "Supermarket Supplies",
              "timestamp": 1729400000000
            },
            {
              "id": "tx-mock-3",
              "amount_cents": 89000,
              "currency": "INR",
              "category": "Transport",
              "description": "Metro Card Recharge",
              "timestamp": 1729300000000
            }
          ],
          "page": 1,
          "page_size": 15,
          "total_items": 3
        }
    """.trimIndent()

    /** Single transaction detail or create/patch response JSON. */
    val TRANSACTION_DETAIL: String = """
        {
          "id": "tx-mock-1",
          "amount_cents": 125000,
          "currency": "INR",
          "category": "Dining",
          "description": "Starbucks Coffee",
          "timestamp": 1729500000000
        }
    """.trimIndent()

    /** Reference categories list response JSON. */
    val STATIC_CATEGORIES: String = """
        {
          "categories": [
            "Dining",
            "Groceries",
            "Shopping",
            "Transport",
            "Bills",
            "Entertainment",
            "Health",
            "Investment"
          ]
        }
    """.trimIndent()

    /** Minimum supported app version check JSON. */
    val STATIC_VERSION: String = """
        {
          "versionCode": 1
        }
    """.trimIndent()

    /** App update status JSON. */
    val STATIC_STATUS: String = """
        {
          "softUpdate": false,
          "versionCode": 1
        }
    """.trimIndent()

    /** Dashboard home board layout JSON. */
    val DASHBOARD: String = """
        {
          "tabs": [
            {"id": "home", "title": "Home"},
            {"id": "pay", "title": "Pay"},
            {"id": "passbook", "title": "Passbook"},
            {"id": "accounts", "title": "Accounts"}
          ],
          "sections": [
            {
              "kind": "balance",
              "title": "Total Balance",
              "body": "",
              "items": []
            },
            {
              "kind": "shortcuts",
              "title": "Quick Actions",
              "body": "",
              "items": [
                {"id": "send", "title": "Send Money"},
                {"id": "scan", "title": "Scan QR"},
                {"id": "bills", "title": "Pay Bills"}
              ]
            }
          ]
        }
    """.trimIndent()

    /** Payment shortcut payees JSON. */
    val PAY_SHORTCUTS: String = """
        {
          "payees": [
            {"displayName": "Aarav Sharma", "vpa": "aarav@upi"},
            {"displayName": "Diya Patel", "vpa": "diya@okaxis"},
            {"displayName": "Rohan Mehta", "vpa": "rohan@icici"}
          ]
        }
    """.trimIndent()

    /** Promotional campaign shelves JSON. */
    val CAMPAIGN_SHELVES: String = """
        {
          "shelves": [
            {
              "key": "festive",
              "name": "Festive Deals",
              "offers": [
                {"sku": "upi5", "pitch": "5% back on UPI up to ₹100", "until": "Until 31 Oct"},
                {"sku": "grocery", "pitch": "₹50 off groceries above ₹500", "until": "Until 20 Oct"}
              ]
            },
            {
              "key": "bills",
              "name": "Bills",
              "offers": [
                {"sku": "electricity", "pitch": "₹25 cashback on electricity", "until": "This week"}
              ]
            }
          ]
        }
    """.trimIndent()

    /** Bill pay public key response JSON. */
    val BILL_PAY_KEY: String = """
        {
          "publicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uUcqEDVuPp+W5NWDkPnFCBLqpHVQxUOZoD8s="
        }
    """.trimIndent()

    /** Bill lookup response JSON. */
    val BILLS_LOOKUP: String = """
        {
          "billNumber": "BILL-1001",
          "amountMinor": 125000,
          "holder": "Rahul Verma"
        }
    """.trimIndent()

    /** Bill pay response JSON. */
    val BILLS_PAY: String = """
        {
          "status": "SUCCESS",
          "message": "Bill paid successfully"
        }
    """.trimIndent()

    /** Device binding challenge response JSON. */
    val DEVICE_CHALLENGE: String = """
        {
          "challenge": "bW9jay1jaGFsbGVuZ2UtYnl0ZXM="
        }
    """.trimIndent()

    /** Device registration response JSON. */
    val DEVICE_BIND: String = """
        {
          "deviceId": "mock-device-bound-123"
        }
    """.trimIndent()

    /**
     * Resolves the mock JSON response matching [path] and HTTP [method], or null if unhandled.
     */
    fun responseFor(path: String, method: String = "GET"): String? {
        val cleanPath = path.trimEnd('/')
        return when {
            cleanPath.endsWith(ApiEndpoints.SESSION) -> SESSION
            cleanPath.endsWith(ApiEndpoints.OTP) -> OTP
            cleanPath.endsWith(ApiEndpoints.TRANSACTIONS) -> {
                if (method == "GET") TRANSACTIONS_LIST else TRANSACTION_DETAIL
            }
            cleanPath.contains(ApiEndpoints.TRANSACTIONS) -> TRANSACTION_DETAIL
            cleanPath.endsWith(ApiEndpoints.STATIC_CATEGORIES) -> STATIC_CATEGORIES
            cleanPath.endsWith(ApiEndpoints.STATIC_VERSION) -> STATIC_VERSION
            cleanPath.endsWith(ApiEndpoints.STATIC_STATUS) -> STATIC_STATUS
            cleanPath.endsWith(ApiEndpoints.DASHBOARD) -> DASHBOARD
            cleanPath.endsWith(ApiEndpoints.PAY_SHORTCUTS) -> PAY_SHORTCUTS
            cleanPath.endsWith(ApiEndpoints.CAMPAIGN_SHELVES) -> CAMPAIGN_SHELVES
            cleanPath.endsWith(ApiEndpoints.BILL_PAY_KEY) -> BILL_PAY_KEY
            cleanPath.endsWith(ApiEndpoints.BILLS_LOOKUP) -> BILLS_LOOKUP
            cleanPath.endsWith(ApiEndpoints.BILLS_PAY) -> BILLS_PAY
            cleanPath.endsWith(ApiEndpoints.DEVICE_CHALLENGE) -> DEVICE_CHALLENGE
            cleanPath.endsWith(ApiEndpoints.DEVICE_BIND) -> DEVICE_BIND
            else -> null
        }
    }
}

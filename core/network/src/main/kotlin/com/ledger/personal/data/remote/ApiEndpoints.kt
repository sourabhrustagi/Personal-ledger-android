package com.ledger.personal.data.remote

/**
 * Central registry of all API base URLs and network endpoint routes across all environments and features.
 */
object ApiEndpoints {

    /** Default and environment base URLs. */
    object BaseUrl {
        // Mock / Local emulator defaults
        const val MOCK_API = "http://10.0.2.2:8080/retail/v1/"
        const val MOCK_PAYMENTS = "http://10.0.2.2:8091/v1/"
        const val MOCK_PROMOTIONS = "http://10.0.2.2:8094/v1/"

        // Dev environment
        const val DEV_API = "https://dev-api.personal-ledger.com/retail/v1/"
        const val DEV_PAYMENTS = "https://dev-payments.personal-ledger.com/v1/"
        const val DEV_PROMOTIONS = "https://dev-promotions.personal-ledger.com/v1/"

        // Staging environment
        const val STAGING_API = "https://staging-api.personal-ledger.com/retail/v1/"
        const val STAGING_PAYMENTS = "https://staging-payments.personal-ledger.com/v1/"
        const val STAGING_PROMOTIONS = "https://staging-promotions.personal-ledger.com/v1/"

        // Production environment
        const val PROD_API = "https://api.personal-ledger.com/retail/v1/"
        const val PROD_PAYMENTS = "https://payments.personal-ledger.com/v1/"
        const val PROD_PROMOTIONS = "https://promotions.personal-ledger.com/v1/"
    }

    /** Authentication and session endpoints. */
    object Auth {
        const val SESSION = "session"
        const val OTP = "auth/otp"
    }

    /** Transactions endpoint. */
    object Transactions {
        const val TRANSACTIONS = "transactions"
    }

    /** Static public configurations and version check endpoints. */
    object Static {
        const val CATEGORIES = "static/categories"
        const val VERSION = "static/version"
        const val STATUS = "static/status"
    }

    /** Team microservices and dashboard endpoints. */
    object Team {
        const val DASHBOARD = "dashboard"
        const val PAY_SHORTCUTS = "pay/shortcuts"
        const val CAMPAIGN_SHELVES = "campaigns/shelves"
    }

    /** Bill payment endpoints. */
    object BillPay {
        const val KEY = "pay/key"
        const val LOOKUP = "bills/lookup"
        const val PAY = "bills/pay"
    }

    /** Cryptographic hardware device binding endpoints. */
    object Devices {
        const val CHALLENGE = "devices/challenge"
        const val BIND = "devices/bind"
    }

    // Direct top-level accessors for concise usage
    const val SESSION = Auth.SESSION
    const val OTP = Auth.OTP
    const val TRANSACTIONS = Transactions.TRANSACTIONS
    const val STATIC_CATEGORIES = Static.CATEGORIES
    const val STATIC_VERSION = Static.VERSION
    const val STATIC_STATUS = Static.STATUS
    const val DASHBOARD = Team.DASHBOARD
    const val PAY_SHORTCUTS = Team.PAY_SHORTCUTS
    const val CAMPAIGN_SHELVES = Team.CAMPAIGN_SHELVES
    const val BILL_PAY_KEY = BillPay.KEY
    const val BILLS_LOOKUP = BillPay.LOOKUP
    const val BILLS_PAY = BillPay.PAY
    const val DEVICE_CHALLENGE = Devices.CHALLENGE
    const val DEVICE_BIND = Devices.BIND
}

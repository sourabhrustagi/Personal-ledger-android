package com.ledger.personal.ui.theme

import androidx.annotation.StringRes
import com.ledger.personal.designsystem.R

/**
 * Centralized registry of UI string resource IDs supporting multilingual localization via strings.xml.
 */
object AppStrings {
    // App Titles
    @StringRes val APP_NAME: Int = R.string.app_name
    @StringRes val TITLE_HOME: Int = R.string.title_home
    @StringRes val TITLE_PASSBOOK: Int = R.string.title_passbook
    @StringRes val TITLE_TRANSACTIONS: Int = R.string.title_transactions
    @StringRes val TITLE_OFFERS: Int = R.string.title_offers
    @StringRes val TITLE_PAY: Int = R.string.title_pay
    @StringRes val TITLE_SCAN: Int = R.string.title_scan
    @StringRes val TITLE_WALLET: Int = R.string.title_wallet
    @StringRes val TITLE_NEW_TRANSACTION: Int = R.string.title_new_transaction
    @StringRes val TITLE_EDIT_TRANSACTION: Int = R.string.title_edit_transaction
    @StringRes val TITLE_TRANSACTION_DETAILS: Int = R.string.title_transaction_details

    // Action Buttons
    @StringRes val ACTION_CONTINUE: Int = R.string.action_continue
    @StringRes val ACTION_SAVE: Int = R.string.action_save
    @StringRes val ACTION_CANCEL: Int = R.string.action_cancel
    @StringRes val ACTION_DELETE: Int = R.string.action_delete
    @StringRes val ACTION_RETRY: Int = R.string.action_retry
    @StringRes val ACTION_REFRESH: Int = R.string.action_refresh
    @StringRes val ACTION_SEND_MONEY: Int = R.string.action_send_money
    @StringRes val ACTION_CONTACTS: Int = R.string.action_contacts
    @StringRes val ACTION_SCAN_QR: Int = R.string.action_scan_qr
    @StringRes val ACTION_OPEN_CAMERA: Int = R.string.action_open_camera
    @StringRes val ACTION_BACK_TO_LIST: Int = R.string.action_back_to_list
    @StringRes val ACTION_ADD_TRANSACTION: Int = R.string.action_add_transaction
    @StringRes val ACTION_UPDATE_TRANSACTION: Int = R.string.action_update_transaction
    @StringRes val ACTION_SAVING_CHANGES: Int = R.string.action_saving_changes
    @StringRes val ACTION_EDIT_DETAILS: Int = R.string.action_edit_details
    @StringRes val ACTION_SEND_ANOTHER: Int = R.string.action_send_another

    // Form Field Labels & Placeholders
    @StringRes val LABEL_AMOUNT: Int = R.string.label_amount
    @StringRes val LABEL_CATEGORY: Int = R.string.label_category
    @StringRes val LABEL_DESCRIPTION: Int = R.string.label_description
    @StringRes val LABEL_RECIPIENT: Int = R.string.label_recipient
    @StringRes val LABEL_RECIPIENT_PLACEHOLDER: Int = R.string.label_recipient_placeholder
    @StringRes val LABEL_SAVED_CONTACTS: Int = R.string.label_saved_contacts
    @StringRes val LABEL_ENTER_AMOUNT: Int = R.string.label_enter_amount
    @StringRes val LABEL_NOTE_OPTIONAL: Int = R.string.label_note_optional
    @StringRes val LABEL_SELECT_FROM_CONTACTS: Int = R.string.label_select_from_contacts
    @StringRes val LABEL_QUICK_SELECT_CATEGORY: Int = R.string.label_quick_select_category
    @StringRes val LABEL_WAITING_FOR_CATEGORIES: Int = R.string.label_waiting_for_categories
    @StringRes val LABEL_YOU_ARE_SENDING: Int = R.string.label_you_are_sending
    @StringRes val LABEL_MONEY_SENT: Int = R.string.label_money_sent

    // Empty & Loading States
    @StringRes val EMPTY_TRANSACTIONS_TITLE: Int = R.string.empty_transactions_title
    @StringRes val EMPTY_TRANSACTIONS_SUBTITLE: Int = R.string.empty_transactions_subtitle
    @StringRes val EMPTY_TRANSACTIONS_DEFAULT_TITLE: Int = R.string.empty_transactions_default_title
    @StringRes val EMPTY_TRANSACTIONS_DEFAULT_SUBTITLE: Int = R.string.empty_transactions_default_subtitle
    @StringRes val EMPTY_TRANSACTION_NOT_FOUND: Int = R.string.empty_transaction_not_found
    @StringRes val EMPTY_PROMOTIONS: Int = R.string.empty_promotions
    @StringRes val EMPTY_TAB_CONTENT: Int = R.string.empty_tab_content
    @StringRes val NO_MATCHING_TRANSACTIONS: Int = R.string.no_matching_transactions
    @StringRes val LOADING_TRANSACTIONS: Int = R.string.loading_transactions
    @StringRes val LOADING_TRANSACTION_DETAILS: Int = R.string.loading_transaction_details
    @StringRes val LOADING_TRANSACTION_DATA: Int = R.string.loading_transaction_data

    // Section Titles
    @StringRes val SECTION_OFFERS: Int = R.string.section_offers
    @StringRes val SECTION_SCAN_AND_PAY: Int = R.string.section_scan_and_pay
    @StringRes val SECTION_SEND_TO_ANYONE: Int = R.string.section_send_to_anyone
    @StringRes val SECTION_SAVED_CONTACTS: Int = R.string.section_saved_contacts
    @StringRes val SECTION_RECENT_TRANSACTIONS: Int = R.string.section_recent_transactions

    // Offline Screen
    @StringRes val TITLE_NO_INTERNET: Int = R.string.title_no_internet
    @StringRes val MESSAGE_NO_INTERNET: Int = R.string.message_no_internet
    @StringRes val LABEL_WAITING_FOR_CONNECTION: Int = R.string.label_waiting_for_connection
    @StringRes val ACTION_CONTINUE_OFFLINE: Int = R.string.action_continue_offline

    // Payment FSM & In-Doubt States
    @StringRes val ACTION_CHECK_STATUS: Int = R.string.action_check_status
    @StringRes val TITLE_TRANSFER_UNCONFIRMED: Int = R.string.title_transfer_unconfirmed
    @StringRes val MESSAGE_TRANSFER_UNCONFIRMED: Int = R.string.message_transfer_unconfirmed

    // Non-translatable technical currency tokens
    const val CURRENCY_INR: String = "INR"
    const val CURRENCY_USD: String = "USD"
    const val CURRENCY_SYMBOL_RUPEE: String = "₹"
}

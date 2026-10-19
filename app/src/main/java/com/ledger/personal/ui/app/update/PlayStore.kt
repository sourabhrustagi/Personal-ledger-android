package com.ledger.personal.ui.app.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Opens the Play listing for this app. */
object PlayStore {

    /** Play listing id. Debug builds drop the `.debug` suffix so they open the store app. */
    fun storeListingId(packageName: String): String = packageName.removeSuffix(".debug")

    /** Opens the Play listing for this app. */
    fun openStore(context: Context, id: String = storeListingId(context.packageName)) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$id".toUri()))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$id".toUri()))
        }
    }
}

package com.ledger.personal.ui.navigation

import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink
import com.ledger.personal.BuildConfig

/** Custom scheme and this flavor's https host, both for [path]. `reified T` is the route type `navDeepLink` needs. */
inline fun <reified T : Any> appLinks(path: String): List<NavDeepLink> = listOf(
    navDeepLink<T>(basePath = "ledger://$path"),
    navDeepLink<T>(basePath = "https://${BuildConfig.APP_LINK_HOST}/$path"),
)

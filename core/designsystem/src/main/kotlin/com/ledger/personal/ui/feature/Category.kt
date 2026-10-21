package com.ledger.personal.ui.feature

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/** Category icon. */
fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "scan & pay", "scan and pay", "upi" -> Icons.Outlined.QrCodeScanner
    "groceries" -> Icons.Outlined.ShoppingCart
    "dining" -> Icons.Outlined.Restaurant
    "transport" -> Icons.Outlined.DirectionsCar
    "salary" -> Icons.Outlined.Payments
    "utilities" -> Icons.Outlined.Bolt
    "entertainment" -> Icons.Outlined.Movie
    else -> Icons.AutoMirrored.Outlined.ReceiptLong
}

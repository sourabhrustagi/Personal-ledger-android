package com.ledger.personal.data.local

import androidx.room.TypeConverter
import com.ledger.personal.domain.model.Money

/** Converts money for Room. */
class MoneyConverter {
    /** Builds this value from money. */
    @TypeConverter
    fun fromMoney(money: Money): Long = money.minorUnits

    /** Converts this value to money. */
    @TypeConverter
    fun toMoney(minorUnits: Long): Money = Money(minorUnits)
}

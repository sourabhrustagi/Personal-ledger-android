package com.ledger.personal.data.local

import androidx.room.TypeConverter
import com.ledger.personal.domain.model.Money

class MoneyConverter {
    @TypeConverter
    fun fromMoney(money: Money): Long = money.minorUnits

    @TypeConverter
    fun toMoney(minorUnits: Long): Money = Money(minorUnits)
}

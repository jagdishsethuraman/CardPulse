package com.example.cardpulse.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBankType(value: BankType): String {
        return value.name
    }

    @TypeConverter
    fun toBankType(value: String): BankType {
        return try {
            BankType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            BankType.OTHER
        }
    }
}

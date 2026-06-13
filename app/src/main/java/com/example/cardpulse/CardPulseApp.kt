package com.example.cardpulse

import android.app.Application
import com.example.cardpulse.data.db.AppDatabase
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.DefaultCardPulseRepository
import com.example.cardpulse.data.prefs.UserPreferences
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class CardPulseApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val repository: CardPulseRepository by lazy {
        DefaultCardPulseRepository(
            creditCardDao = database.creditCardDao(),
            statementDao = database.statementDao(),
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao()
        )
    }

    val userPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox resource loader for PDF parsing
        PDFBoxResourceLoader.init(this)
    }
}

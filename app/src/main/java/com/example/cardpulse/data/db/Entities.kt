package com.example.cardpulse.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class BankType { SBI, HDFC, FEDERAL, OTHER }

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,
    val bankName: String,           // Maps to BankType name or String
    val lastFourDigits: String,
    val colorHex: String,
    val billingCycleStartDay: Int,
    val creditLimit: Double?,
    val createdAt: Long
)

@Entity(
    tableName = "statements",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("creditCardId")]
)
data class Statement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditCardId: Long,
    val billingPeriodStart: Long,
    val billingPeriodEnd: Long,
    val periodLabel: String,
    val importedAt: Long,
    val sourceFileName: String,
    val totalAmount: Double,
    val transactionCount: Int
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Statement::class,
            parentColumns = ["id"],
            childColumns = ["statementId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("statementId"),
        Index("creditCardId"),
        Index("categoryId")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val statementId: Long,
    val creditCardId: Long,
    val categoryId: Long,
    val amount: Double,
    val description: String,
    val rawNarration: String,
    val transactionDate: Long,
    val isDebit: Boolean,
    val merchantName: String?
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val keywords: String,
    val sortOrder: Int
)

data class TransactionWithDetails(
    @Embedded val transaction: Transaction,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category?,
    @Relation(parentColumn = "creditCardId", entityColumn = "id")
    val creditCard: CreditCard?
)

data class StatementWithCard(
    @Embedded val statement: Statement,
    @Relation(parentColumn = "creditCardId", entityColumn = "id")
    val creditCard: CreditCard?
)

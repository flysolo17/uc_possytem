package com.flysolo.cashregister.firebase.models

class Transaction(
    val transactionID: String? = null,
    val transactionCashier: String ? = null,
    val transactionTimestamp : Long ? = null,
    val transactionItems: List<ItemPurchased> ? = null ) {
    companion object {
        const val TABLE_NAME = "Transaction"
        const val RECEIPT_ID = "transactionID"
        const val TIMESTAMP = "transactionTimestamp"
        const val TRANSACTION_ITEMS = "transactionItems"
        const val REFUND = "transactionTotalRefunds"
    }
}
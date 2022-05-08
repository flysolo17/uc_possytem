package com.flysolo.cashregister.firebase.models



class CashDrawer(var cashDrawerID:  String? = "",
                 var cashierID:  String? = "",
                 var startingCash: Int? = 0,
                 var cashAdded : List<Int> = mutableListOf(),
                 var timestamp: Long? = null){
companion object{
    const val TABLE_NAME = "CashDrawer"
    const val TIMESTAMP = "timestamp"
    const val STARTING_CASH = "startingCash"
    const val CASH_ADDED = "cashAdded"
    }
}
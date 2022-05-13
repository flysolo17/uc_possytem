package com.flysolo.cashregister.firebase.models

class Cashier(
    var cashierID:  String? = null,
    var cashierProfile:  String? = null,
    var cashierName: String? = null,
    var cashierPin:  String? = null,
) {
companion object {
    const val TABLE_NAME = "Cashier"
    const val CASHIER_NAME = "cashierName"
}
}
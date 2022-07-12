package com.flysolo.cashregister.firebase.models
class ItemPurchased(val itemPurchasedID: String? = null,
                    val itemPurchasedName : String? = null,
                    val itemPurchasedQuantity : Int? = null,
                    var itemPurchasedCost: Double ? = 0.00,
                    val itemPurchasedPrice : Double? = 0.00,
                    var itemPurchasedIsRefunded : Boolean? = null){
    companion object {
        const val TABLE_NAME = "Itempurchased"
        const val ITEM_PURCHASED_ID = "itemPurchasedID"
        const val ITEM_PURCHASED_PRICE = "itemPurchasedID"
    }
}
package com.flysolo.cashregister.purchases
class ItemPurchased(val itemPurchasedID: String? = null,
                    val itemPurchasedName : String? = null,
                    val itemPurchasedQuantity : Int? = null,
                    var itemPurchasedCost: Int ? = null,
                    val itemPurchasedPrice : Int? = null,
                    var itemPurchasedIsRefunded : Boolean? = null){
    companion object {
        const val TABLE_NAME = "Itempurchased"
        const val ITEM_PURCHASED_ID = "itemPurchasedID"
        const val ITEM_PURCHASED_PRICE = "itemPurchasedID"
    }
}
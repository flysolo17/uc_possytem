package com.flysolo.cashregister.firebase.models


data class Items(val itemBarcode: String? = null,
                 val itemImageURL: String ? = null,
                 val itemName : String ? = null,
                 val itemCategory: String ? = null,
                 var itemQuantity: Int ? = null,
                 val itemCost : Double? = 0.00,
                 val itemPrice : Double? = 0.00,
                 val timestamp: Long? = null){

    companion object {
        const val TABLE_NAME = "Items"
        const val BARCODE = "itemBarcode"
        const val ITEM_NAME = "itemName"
        const val ITEM_CATEGORY = "categoryName"
        const val ITEM_TIMESTAMP = "timestamp"
        const val ITEM_QUANTITY= "itemQuantity"
        const val IMAGE = "itemImageURL"
    }

}

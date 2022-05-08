package com.flysolo.cashregister.firebase.models


class User(
    var userId: String? = "",
    var userProfile: String = "",
    var userFirstname: String? = "",
    var userLastname: String? = "",
    var userBusinessName: String? =  "",
    var userStorePin: String? = "",
    var userPhoneNumber: String? = "",
    var userEmail: String? = ""

) {

    companion object {
        const val TABLE_NAME = "Users"
        const val EMAIL = "userEmail"
        const val STORE_IMAGE = "userProfile"
        const val NAME = "userName"
        const val BUSINESS_NAME = "userBusinessName"
        const val USER_ID = "userId"
        const val PIN = "userStorePin"
    }
}

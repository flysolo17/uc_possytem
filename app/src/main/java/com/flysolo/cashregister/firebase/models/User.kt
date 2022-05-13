package com.flysolo.cashregister.firebase.models

import android.os.Parcel
import android.os.Parcelable


 class User(
     var userId: String? = "",
     var userProfile: String? = "",
     var userFirstname: String? = "",
     var userLastname: String? = "",
     var userBusinessName: String? =  "",
     var userStorePin: String? = "",
     var userPhoneNumber: String? = "",
     var userEmail: String? = ""

) : Parcelable {



     constructor(parcel: Parcel) : this(
         parcel.readString(),
         parcel.readString(),
         parcel.readString(),
         parcel.readString(),
         parcel.readString(),
         parcel.readString(),
         parcel.readString(),
         parcel.readString()
     ) {
     }

     override fun describeContents(): Int {
        return 0
     }

     override fun writeToParcel(p0: Parcel?, p1: Int) {
         p0?.writeString(userId)
         p0?.writeString(userProfile)
         p0?.writeString(userFirstname)
         p0?.writeString(userLastname)
         p0?.writeString(userBusinessName)
         p0?.writeString(userStorePin)
         p0?.writeString(userPhoneNumber)
         p0?.writeString(userEmail)


     }

     companion object CREATOR : Parcelable.Creator<User> {
         override fun createFromParcel(parcel: Parcel): User {
             return User(parcel)
         }

         override fun newArray(size: Int): Array<User?> {
             return arrayOfNulls(size)
         }
         const val TABLE_NAME = "Users"
         const val EMAIL = "userEmail"
         const val STORE_IMAGE = "userProfile"
         const val NAME = "userName"
         const val BUSINESS_NAME = "userBusinessName"
         const val USER_ID = "userId"
         const val PIN = "userStorePin"
     }
 }

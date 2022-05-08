package com.flysolo.cashregister.navdrawer.home.models

data class Scanning(
    val id : String? = null,
    val name : String? = null,
    val image : String? = null)
{
    companion object {
        const val TABLE_NAME = "Scanning"
    }

}
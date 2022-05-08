package com.flysolo.cashregister.firebase

import java.util.*

class QueryDates {
    fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        val date = Date(timestamp)
        cal.time = date
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 1
        return cal.timeInMillis
    }

    fun endOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        val date = Date(timestamp)
        cal.time = date
        cal[Calendar.HOUR_OF_DAY] = 23
        cal[Calendar.MINUTE] = 59
        cal[Calendar.SECOND] = 59
        return cal.timeInMillis
    }
}
package com.flysolo.cashregister.firebase.models

class Attendance(
     val attendanceID: String? = "",
     var cashierName: String? = "",
     var timeInImage: String? = "",
     var timeOutImage: String? = "",
     var timestampTimeIn : Long? = null,
     var timestampTimeOut : Long? = null) {
    companion object {
        const val TABLE_NAME = "Attendance"
        const val ATTENDANCE_ID = "attendanceID"
        const val TIMEOUT_IMAGE = "timeOutImage"
        const val TIMEOUT_TIMESTAMP = "timestampTimeOut"
        const val TIMEIN_TIMESTAMP = "timestampTimeIn"
    }


}
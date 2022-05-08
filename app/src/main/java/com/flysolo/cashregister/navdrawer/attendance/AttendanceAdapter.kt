package com.flysolo.cashregister.navdrawer.attendance

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore

import android.view.LayoutInflater
import android.view.View

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.RowAttendanceBinding
import com.flysolo.cashregister.firebase.models.Attendance
import com.flysolo.cashregister.firebase.models.Cashier
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*


class AttendanceAdapter(val context: Context, options: FirestoreRecyclerOptions<Attendance?>,val selfieOutIsClick: SelfieOutIsClick) :
    FirestoreRecyclerAdapter<Attendance, AttendanceAdapter.AttendanceViewHolder>(options) {

    interface SelfieOutIsClick{
        fun selfieOutClick(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.row_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int, model: Attendance) {
        holder.textEmployeeName.text = model.cashierName
        if (model.timeInImage!!.isNotEmpty()){
            Picasso.get()
                .load(model.timeInImage)
                .into(holder.cashierSelfieIn)
        }
        if (model.timeOutImage!!.isEmpty()){
            holder.cashierSelfieOut.setOnClickListener {
                selfieOutIsClick.selfieOutClick(position)
            }
        }
        else {
            Picasso.get()
                .load(model.timeOutImage)
                .into(holder.cashierSelfieOut)
        }
        if (model.timestampTimeIn == 0.toLong()){
            holder.textSelfieIn.text = ""
        } else {
            holder.textSelfieIn.text = setCalendarFormat(model.timestampTimeIn!!)
        }
        if (model.timestampTimeOut == 0.toLong()){
            holder.textSelfieOut.text = ""
        } else {
            holder.textSelfieOut.text = setCalendarFormat(model.timestampTimeOut!!)
        }



    }


    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cashierSelfieIn : ShapeableImageView
        var cashierSelfieOut : ShapeableImageView
        var textSelfieIn: TextView
        var textSelfieOut: TextView
        var textEmployeeName: TextView
        init {
            cashierSelfieIn = itemView.findViewById(R.id.cashierSelfieIn)
            cashierSelfieOut = itemView.findViewById(R.id.cashierSelfieOut)
            textSelfieIn = itemView.findViewById(R.id.textSelfieIn)
            textSelfieOut = itemView.findViewById(R.id.textSelfieOut)
            textEmployeeName = itemView.findViewById(R.id.textCashierName)

        }
    }

    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("hh:mm aa")
        return format.format(date)
    }


}



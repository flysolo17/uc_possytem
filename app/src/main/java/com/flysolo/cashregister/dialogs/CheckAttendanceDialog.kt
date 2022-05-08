package com.flysolo.cashregister.dialogs

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.DialogCheckAttendanceBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.Attendance
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.navdrawer.attendance.AttendanceViewModel
import com.google.firebase.firestore.FirebaseFirestore



class CheckAttendanceDialog : DialogFragment() {

    private lateinit var binding: DialogCheckAttendanceBinding
    var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var listCashiers: MutableList<String>? = null
    private lateinit var arrayAdapter: ArrayAdapter<*>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var attendanceViewModel: AttendanceViewModel
    private var imageURI: Uri? = null
    private lateinit var firebaseQueries: FirebaseQueries
    private var cashiername: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DialogCheckAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseQueries = FirebaseQueries(requireContext(),db)
        attendanceViewModel = ViewModelProvider(requireActivity()).get(AttendanceViewModel::class.java)
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val data = result.data
                val bitmap = data?.extras?.get("data") as Bitmap
            }
        listCashiers = mutableListOf()
        arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line, listCashiers!!
        )
        binding.listCashierName.adapter = arrayAdapter
        db.collection(User.TABLE_NAME)
            .document(CashierLoginActivity.userID!!)
            .collection(Cashier.TABLE_NAME)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        val cashierName: String = document.getString("cashierName")!!
                        if (!listCashiers!!.contains(cashierName)) {
                            listCashiers?.add(cashierName)
                            arrayAdapter.notifyDataSetChanged()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "No Addresses", Toast.LENGTH_SHORT).show()
                }
            }
        //list.setItemChecked(0, true);
        binding.listCashierName.setOnItemClickListener { parent, view, position, id ->
            cashiername = listCashiers!![position]
            launchCamera()
            Toast.makeText(requireContext(), listCashiers!![position], Toast.LENGTH_SHORT).show()
        }

    }

    private fun launchCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                cameraLauncher.launch(intent)
            }

        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }
    }

}
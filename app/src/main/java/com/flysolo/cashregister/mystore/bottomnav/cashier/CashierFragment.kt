package com.flysolo.cashregister.mystore.bottomnav.cashier

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.AutoTransition
import android.transition.TransitionManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.cashierlogin.adapter.CashierAdapter
import com.flysolo.cashregister.databinding.FragmentCashierBinding
import com.flysolo.cashregister.dialogs.ProgressDialog
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.navdrawer.attendance.AttendanceAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import java.io.IOException

class CashierFragment : Fragment(),CashierAdapter.CashierClickListener,AttendanceAdapter.SelfieOutIsClick{
    private lateinit var binding: FragmentCashierBinding
    private var cashierProfileURI: Uri? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var firebaseQueries: FirebaseQueries? = null
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private  lateinit var progressDialog: ProgressDialog
    private lateinit var cashierAdapter: CashierAdapter
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val queryDates = QueryDates()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCashierBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressDialog = ProgressDialog(requireActivity())
        firebaseQueries = FirebaseQueries(requireContext(), FirebaseFirestore.getInstance())
        storage = FirebaseStorage.getInstance().getReference("cashier")
        binding.textCreateCashier.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.layoutToExpand,AutoTransition())
            binding.layoutToExpand.visibility =  if (binding.layoutToExpand.visibility === View.GONE) View.VISIBLE else View.GONE
        }
        //Get image in the gallery
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            try {
                if (data?.data != null) {
                    binding.buttonAddCashierProfile.setImageURI(data.data)
                    cashierProfileURI = data.data
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        binding.buttonCreateCashier.setOnClickListener {
            createCashier()
        }
        binding.buttonAddCashierProfile.setOnClickListener {
            launchGallery()
        }
        binding.recyclerviewCashiers.layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
        cashierAdapter = CashierAdapter(requireContext(),
            firebaseQueries!!.getCashiers(CashierLoginActivity.userID!!),this)
        binding.recyclerviewCashiers.adapter = cashierAdapter

        attendanceAdapter = AttendanceAdapter(requireContext(), firebaseQueries!!.getAttendance(
            queryDates.startOfDay(System.currentTimeMillis()),
            queryDates.endOfDay(System.currentTimeMillis())), this)
        binding.recyclerviewAttendance.adapter = attendanceAdapter
    }

    //TODO: pick image from the gallery
    private fun launchGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher!!.launch(galleryIntent)
    }

    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = requireContext().contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }

    //TODO: upload file in firestore and storage
    private fun createCashier() {
        progressDialog.loading()
        val cashierName: String = binding.inputCashierName.editText?.text.toString()
        val cashierPinCode: String = binding.inputCashierPin.editText?.text.toString()

        when {
            cashierName.isEmpty() -> {
                binding.inputCashierName.error = "Enter cashier name"
                progressDialog.stopLoading()
            }
            cashierPinCode.isEmpty() -> {
                binding.inputCashierName.error = "Enter cashier pin"
                progressDialog.stopLoading()
            }
            else -> {
                if (cashierProfileURI != null) {
                    val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(cashierProfileURI!!))
                    mUploadTask = fileReference.putFile(cashierProfileURI!!)
                        .addOnSuccessListener {
                            fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                                val cashier = Cashier(
                                    firebaseQueries?.generateID(Cashier.TABLE_NAME),
                                    uri.toString(),
                                    cashierName,
                                cashierPinCode)
                                firebaseQueries?.createCashier(cashier)
                                progressDialog.stopLoading()
                            }
                        }
                } else {
                    progressDialog.stopLoading()
                    Toast.makeText(requireContext(),"Failed: you need to put cashier profile to the item",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        cashierAdapter.startListening()
        attendanceAdapter.startListening()
    }
    override fun onCashierClick(pos: Int) {
        Toast.makeText(requireContext(),cashierAdapter.getItem(pos).cashierName,Toast.LENGTH_SHORT).show()
    }

    override fun selfieOutClick(position: Int) {
        Toast.makeText(requireContext(),"You can't sign out your employees",Toast.LENGTH_SHORT).show()
    }
}
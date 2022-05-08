package com.flysolo.cashregister.navdrawer.attendance

import android.content.ContentValues
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.flysolo.cashregister.databinding.FragmentAttendanceBinding
import android.content.Intent

import androidx.activity.result.ActivityResultLauncher

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.result.ActivityResult

import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import java.io.IOException
import android.provider.MediaStore
import android.widget.Toast

import android.net.Uri
import android.transition.AutoTransition
import android.transition.TransitionManager

import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity

import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.Attendance
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask

class AttendanceFragment : Fragment() ,AttendanceAdapter.SelfieOutIsClick {
    private lateinit var binding: FragmentAttendanceBinding
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var firebaseQueries: FirebaseQueries
    private var listCashiers: MutableList<String>? = null
    private var selfieUri: Uri? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private val queryDates = QueryDates()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listCashiers = mutableListOf()
        getCashier()
        storage = FirebaseStorage.getInstance().getReference("attendance")
        firebaseQueries = FirebaseQueries(requireContext(), firebaseFirestore)
        cameraLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
                val bitmap = result.data!!.extras!!.get("data") as Bitmap?
                if (bitmap != null) {
                    selfieUri = convertBitmapToUri(System.currentTimeMillis().toString(), bitmap)
                    binding.imageSelfie.setImageURI(selfieUri)
                }
            }

        attendanceAdapter = AttendanceAdapter(requireContext(), firebaseQueries.getAttendance(
            queryDates.startOfDay(System.currentTimeMillis()),
        queryDates.endOfDay(System.currentTimeMillis())), this)
        binding.recyclerviewAttendance.adapter = attendanceAdapter
        binding.imageSelfie.setOnClickListener {
            launchCamera(cameraLauncher)
        }
        binding.buttonExpand.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.layoutToExpand, AutoTransition())
            binding.layoutToExpand.visibility =  if (binding.layoutToExpand.visibility === View.GONE) View.VISIBLE else View.GONE
        }
        binding.buttonCheckAttendance.setOnClickListener {
            val cashierName: String = binding.textCashierName.editText?.text.toString()
            when {
                selfieUri == null -> {
                    Toast.makeText(
                        requireContext(),
                        "Take a selfie for attendance",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                cashierName.isEmpty() -> {
                    binding.textCashierName.error = "Please Select your name"
                }
                else -> {
                    val fileReference = storage!!.child(
                        System.currentTimeMillis().toString() + "." + getFileExtension(selfieUri!!)
                    )
                    mUploadTask = fileReference.putFile(selfieUri!!)
                        .addOnSuccessListener {
                            fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                                val attendance = Attendance(
                                    firebaseQueries.generateID(Attendance.TABLE_NAME),
                                    cashierName,
                                    uri.toString(),
                                    "",
                                    System.currentTimeMillis(), 0
                                )
                                firebaseQueries.signInAttendance(attendance)
                                binding.layoutToExpand.visibility = View.GONE
                            }
                        }
                }
            }
        }

    }

    private fun launchCamera(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                launcher.launch(intent)
            }

        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launcher.launch(intent)
        }
    }

    private fun getCashier() {
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(CashierLoginActivity.userID!!)
            .collection(Cashier.TABLE_NAME)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        val cashierName: String = document.getString("cashierName")!!
                        if (!listCashiers!!.contains(cashierName)) {
                            listCashiers?.add(cashierName)
                            val arrayAdapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_expandable_list_item_1, listCashiers!!
                            )
                            binding.textCashiers.setAdapter(arrayAdapter)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "No Addresses", Toast.LENGTH_SHORT).show()
                }
            }
    }

    //Deprecated
/*    private fun convertBitmapToUri(bitmap: Bitmap): Uri{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream)
        val  path : String = MediaStore.Images.Media
            .insertImage(requireActivity().contentResolver,bitmap,System.currentTimeMillis().toString(),null)
        return Uri.parse(path)
    }*/
    private fun convertBitmapToUri(name: String, bitmap: Bitmap): Uri? {
        val imageCollection: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "images/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }
        return requireActivity().contentResolver.insert(imageCollection, contentValues)?.also {
            requireActivity().contentResolver.openOutputStream(it).use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    throw IOException("Failed to save bitmap")
                }
            }
        }
    }

    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = requireContext().contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }

    override fun onStart() {
        super.onStart()
        attendanceAdapter.startListening()
    }

    private var attendanceID: String? = null
    override fun selfieOutClick(position: Int) {
        attendanceID = attendanceAdapter.getItem(position).attendanceID
        launchCamera(selfieOutLauncher)
    }

    private val selfieOutLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            val bitmap = result.data!!.extras!!.get("data") as Bitmap?
            if (bitmap != null) {
                val timestamp: Long = System.currentTimeMillis()
                val selfieOutUri = convertBitmapToUri(timestamp.toString(), bitmap)
                signOutSelfie(selfieOutUri!!)

            }
        }

    private fun signOutSelfie(uri: Uri) {
        val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(uri))
        mUploadTask = fileReference.putFile(uri)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                    firebaseQueries.signOutAttendance(attendanceID!!, uri.toString(),System.currentTimeMillis())
                }
            }
    }
}


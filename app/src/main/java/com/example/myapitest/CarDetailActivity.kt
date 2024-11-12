package com.example.myapitest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.minhaprimeiraapi.service.RetrofitClient
import com.example.minhaprimeiraapi.service.safeApiCall
import com.example.myapitest.databinding.ActivityCarDetailBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.RetrieveCar
import com.example.myapitest.ui.loadUrl
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class CarDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarDetailBinding

    private  lateinit var car: Car

    private lateinit var retrieveCar: RetrieveCar

    private lateinit var imageUri: Uri

    private var imageFile: File? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            binding.image.loadUrl(imageUri.toString())
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        loadItem()

    }

    private fun loadItem() {
        val carId = intent.getStringExtra(ARG_ID) ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCar(carId) }

            withContext(Dispatchers.Main) {
                when (result) {

                    is com.example.minhaprimeiraapi.service.Result.Error -> {}
                    is com.example.minhaprimeiraapi.service.Result.Success ->  {
                        retrieveCar = result.data
                        car = retrieveCar.value
                        handleSuccess()
                    }
                }
            }
        }
    }

    private fun handleSuccess() {

        binding.name.setText(retrieveCar.value.name)
        binding.year.setText(retrieveCar.value.year)
        binding.licence.setText(retrieveCar.value.licence)
        binding.image.loadUrl(retrieveCar.value.imageUrl)

    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.deleteCTA.setOnClickListener {
            deleteItem()
        }
        binding.editCTA.setOnClickListener {
          uploadImageToFirebase()
        }
        binding.takePictureCta.setOnClickListener {
            takePicture()
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun editItem() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.updateCar(
                    car.id,
                    car.copy(
                        name = binding.name.text.toString(),
                        year = binding.year.text.toString(),
                        licence = binding.licence.text.toString(),
                        imageUrl = imageUri.toString()

                    )
                )
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is com.example.minhaprimeiraapi.service.Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            R.string.unknown_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is com.example.minhaprimeiraapi.service.Result.Success -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            R.string.success_update,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun uploadImageToFirebase() {

        val storageRef = FirebaseStorage.getInstance().reference

        val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        val imageBitmap = BitmapFactory.decodeFile(imageFile!!.path)
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        binding.takePictureCta.isEnabled = false

        imagesRef.putBytes(data)
            .addOnFailureListener {
                binding.takePictureCta.isEnabled = true
                Toast.makeText(this, "Falha ao realizar o upload", Toast.LENGTH_SHORT).show()
                return@addOnFailureListener
            }
            .addOnSuccessListener {
                binding.takePictureCta.isEnabled = true
                imagesRef.downloadUrl.addOnSuccessListener { uri ->
                    imageUri = uri
                    editItem()
                }
            }
    }


    private fun deleteItem() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteCar(car.id) }

            Log.d("FirebaseStorage", car.imageUrl)

            deleteImageFromStorage(car.imageUrl)

            withContext(Dispatchers.Main) {
                when (result) {
                    is com.example.minhaprimeiraapi.service.Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            R.string.error_delete,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is com.example.minhaprimeiraapi.service.Result.Success-> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            R.string.success_delete,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    fun deleteImageFromStorage(imageUrl: String) {
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.reference

        val filePath = imageUrl.substringAfter("/images%2F").substringBefore("?")
        Log.d("FirebaseStorage", filePath.toString())

        val fileReference = storageReference.child("images/$filePath")

        fileReference.delete()
            .addOnSuccessListener {
                Log.d("FirebaseStorage", "Imagem deletada com sucesso.")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseStorage", "Erro ao deletar imagem: ${exception.message}")
            }
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        Log.e("CAMINHO DO ARQUIVO", "Path: ${imageFile?.absolutePath}")
        if (imageFile?.exists() == true) {
            Log.e("ARQUIVO CRIADO", "O arquivo foi criado com sucesso.")
        } else {
            Log.e("ARQUIVO NÃO CRIADO", "Houve um problema ao criar o arquivo.")
        }

        return FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
    }

    companion object {

        private const val ARG_ID = "ARG_ID"
        private const val CAMERA_REQUEST_CODE = 101

        fun newIntent(
            context: Context,
            itemId: String
        ) =
            Intent(context, CarDetailActivity::class.java).apply {
                putExtra(ARG_ID, itemId)
            }
    }
}
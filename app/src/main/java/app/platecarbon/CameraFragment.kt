package app.platecarbon

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import app.platecarbon.databinding.FragmentCameraBinding
import app.platecarbon.network.ApiClient
import app.platecarbon.network.ApiResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File

    // Galeri seçimi için launcher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                // URI’den dosyaya çevir
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val tempFile = File.createTempFile("gallery_", ".jpg", requireContext().cacheDir)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                sendImageToApi(tempFile)
            } catch (e: Exception) {
                Log.e("CameraFragment", "Galeri dosya hatası: ${e.message}")
                Toast.makeText(requireContext(), "Dosya işlenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // İzin launcher’ları
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Kamera izni reddedildi", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(requireContext(), "Galeri izni reddedildi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()

        // Kamera butonu
        binding.captureBtn.setOnClickListener {
            takePhoto()
        }

        // Galeriden seç butonu
        binding.galleryBtn.setOnClickListener {
            checkGalleryPermission()
        }

        // Kamera izni kontrolü
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*")
        } else {
            galleryPermissionLauncher.launch(permission)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraFragment", "Kamera başlatılamadı: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PlateCarbon")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    savedUri?.let {
                        val file = File(requireContext().cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                        requireContext().contentResolver.openInputStream(it)?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d("CameraFragment", "Fotoğraf kaydedildi: ${file.absolutePath}")
                        sendImageToApi(file)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraFragment", "Fotoğraf çekilemedi: ${exception.message}")
                    Toast.makeText(requireContext(), "Fotoğraf çekilemedi", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun sendImageToApi(photoFile: File) {
        val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        Log.d("CameraFragment", "RequestBody oluşturuldu: ${photoFile.name}")

        val body = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)
        Log.d("CameraFragment", "MultipartBody oluşturuldu")

        val call = ApiClient.plateService.uploadPlateImage(body)
        Log.d("CameraFragment", "API çağrısı başlatılıyor")
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("CameraFragment", "API yanıtı alındı: HTTP ${response.code()}")
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d("CameraFragment", "Ham ApiResponse: $apiResponse")
                    apiResponse?.let {
                        val plateNumber = it.plates?.firstOrNull()?.plate_number ?: "Plaka bulunamadı"
                        binding.detectedPlateText.text = plateNumber
                        Toast.makeText(
                            requireContext(),
                            "Plaka: $plateNumber",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("CameraFragment", "Plaka alındı: $plateNumber")
                    } ?: run {
                        Log.d("CameraFragment", "API yanıtı boş")
                        Toast.makeText(requireContext(), "API yanıtı boş", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Hata mesajı alınamadı"
                    Log.e("CameraFragment", "API hatası: HTTP ${response.code()}, Mesaj: $errorBody")
                    Toast.makeText(
                        requireContext(),
                        "API hatası: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("CameraFragment", "Bağlantı hatası: ${t.message}")
                Toast.makeText(
                    requireContext(),
                    "Bağlantı hatası: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, "PlateCarbon").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().filesDir
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
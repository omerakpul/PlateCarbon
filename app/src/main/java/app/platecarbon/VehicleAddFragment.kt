package app.platecarbon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.platecarbon.R
import app.platecarbon.databinding.FragmentVehicleAddBinding
import app.platecarbon.model.VehicleRequest
import app.platecarbon.model.GenericResponse
import app.platecarbon.network.ApiResponse
import app.platecarbon.network.ApiClient
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class VehicleAddFragment : Fragment() {

    private var _binding: FragmentVehicleAddBinding? = null
    private val binding get() = _binding!!

    // Resim verisi saklamak için
    private var savedImageFile: File? = null

    // Marka listesi
    private val brands = listOf(
        "Audi", "Citroen", "Dacia", "Fiat", "Ford",
        "Hyundai", "Mitsubishi", "Nissan", "Opel", "Peugeot",
        "Renault", "Seat", "Skoda", "Toyota", "Volkswagen"
    )

    // Marka-model eşleştirmesi
    private val brandModels = mapOf(
        "Audi" to listOf("A3", "A4", "Q5", "Q7"),
        "Citroen" to listOf("C3", "C4", "C5", "Berlingo"),
        "Dacia" to listOf("Sandero", "Duster", "Logan", "Spring"),
        "Fiat" to listOf("Egea", "Panda", "Tipo", "500X"),
        "Ford" to listOf("Fiesta", "Focus", "Kuga", "Mondeo"),
        "Hyundai" to listOf("i10", "i20", "Tucson", "Elantra"),
        "Mitsubishi" to listOf("Space Star", "ASX", "Outlander", "L200"),
        "Nissan" to listOf("Micra", "Juke", "Qashqai", "X-Trail"),
        "Opel" to listOf("Corsa", "Astra", "Insignia", "Mokka"),
        "Peugeot" to listOf("208", "308", "3008", "508"),
        "Renault" to listOf("Clio", "Megane", "Captur", "Talisman"),
        "Seat" to listOf("Ibiza", "Leon", "Arona", "Ateca"),
        "Skoda" to listOf("Fabia", "Octavia", "Kamiq", "Kodiaq"),
        "Toyota" to listOf("Corolla", "Yaris", "C-HR", "RAV4"),
        "Volkswagen" to listOf("Golf", "Polo", "Passat", "Tiguan")
    )

    // Yakıt türleri listesi
    private val fuelTypes = listOf("Benzin", "Dizel", "LPG", "Hibrit", "Elektrik")

    // Araç tipleri listesi
    private val vehicleTypes = listOf("Sedan", "Hatchback", "SUV", "Ticari", "Coupe")

    // Renk listesi
    private val colors = listOf(
        "Beyaz", "Siyah", "Gri", "Gümüş", "Mavi",
        "Kırmızı", "Yeşil", "Sarı", "Turuncu", "Kahverengi",
        "Lacivert", "Bordo", "Pembe", "Mor", "Altın"
    )

    // Yıl listesi (2005-2024)
    private val years = (2005..2024).map { it.toString() }.reversed()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gelenPlaka = arguments?.getString("plaka")
        binding.plakaEditText.setText(gelenPlaka)

        // Resim dosyasını al (eğer varsa)
        savedImageFile = arguments?.getSerializable("imageFile") as? File

        // Geri butonu click listener'ı
        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_vehicleAddFragment_to_cameraFragment)
        }

        // Tüm dropdown'ları başlat
        setupBrandSpinner()
        setupModelSpinnerEmpty() // <-- eklendi: Model spinner'ı başta boş olsun
        setupFuelTypeSpinner()
        setupVehicleTypeSpinner()
        setupColorSpinner()
        setupYearSpinner()

        // Kamera'dan gelen markayı seç
        val gelenMarka = arguments?.getString("marka")
        if (!gelenMarka.isNullOrBlank() && gelenMarka != "unknown") {
            val duzenlenmisMarka = gelenMarka.lowercase().replaceFirstChar { it.uppercase() }
            selectBrandInSpinner(duzenlenmisMarka)
        }

        // Marka değişikliklerini dinle
        binding.markaSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBrand = parent?.getItemAtPosition(position)?.toString() ?: return
                updateModelSpinner(selectedBrand)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Hiçbir şey seçilmediğinde model dropdown'ını temizle
                setupModelSpinnerEmpty() // <-- eklendi: Seçim yoksa model spinner'ı boşalt
            }
        }

        // Model değişikliklerini dinle
        binding.modelSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Model seçildiğinde diğer dropdown'lar zaten hazır olacak
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Hiçbir şey seçilmediğinde yapılacak işlemler
            }
        }

        binding.kaydetBtn.setOnClickListener {
            val plaka = binding.plakaEditText.text.toString().trim()
            val marka = if (binding.markaSpinner.selectedItem != null) {
                binding.markaSpinner.selectedItem.toString()
            } else {
                ""
            }
            val model = if (binding.modelSpinner.selectedItem != null) {
                binding.modelSpinner.selectedItem.toString()
            } else {
                ""
            }
            val renk = if (binding.renkSpinner.selectedItem != null) {
                binding.renkSpinner.selectedItem.toString()
            } else {
                ""
            }
            val yakit = if (binding.yakitSpinner.selectedItem != null) {
                binding.yakitSpinner.selectedItem.toString()
            } else {
                ""
            }
            val yilStr = if (binding.yilSpinner.selectedItem != null) {
                binding.yilSpinner.selectedItem.toString()
            } else {
                ""
            }
            val aractipi = if (binding.aracTipiSpinner.selectedItem != null) {
                binding.aracTipiSpinner.selectedItem.toString()
            } else {
                ""
            }

            val aracYili = yilStr.toIntOrNull()

            if (plaka.isBlank() || marka.isBlank() || model.isBlank() ||
                renk.isBlank() || yakit.isBlank() || yilStr.isBlank() || aractipi.isBlank() || aracYili == null) {
                Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val vehicleRequest = VehicleRequest(
                plaka = plaka,
                marka = marka,
                model = model,
                renk = renk,
                yakit_turu = yakit,
                arac_tipi = aractipi,
                karbon_emisyon = null,
                arac_yili = aracYili
            )

            ApiClient.plateService.addVehicle(vehicleRequest).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message ?: "Başarıyla kaydedildi"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        // API'den dönen araç bilgilerini al (karbon emisyon dahil)
                        val savedVehicle = response.body()?.arac ?: vehicleRequest
                        // Aynı resmi tekrar gönder (sanki yeni fotoğraf çekilmiş gibi)
                        savedImageFile?.let { imageFile ->
                            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val imagePart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

                            ApiClient.plateService.uploadPlateImage(imagePart).enqueue(object : Callback<ApiResponse> {
                                override fun onResponse(call: Call<ApiResponse>, plateResponse: Response<ApiResponse>) {
                                    if (plateResponse.isSuccessful) {
                                        // Plaka okuma başarılı, result ekranına yönlendir
                                        val plateData = plateResponse.body()
                                        val vehicleInfo = plateData?.arac
                                        val bundle = Bundle().apply {
                                            putString("plaka", vehicleInfo?.plaka ?: plateData?.plaka ?: plaka)
                                            putString("marka", vehicleInfo?.marka ?: plateData?.marka ?: marka)
                                            putString("model", vehicleInfo?.model ?: model)
                                            putString("renk", vehicleInfo?.renk ?: renk)
                                            putString("yakit_turu", vehicleInfo?.yakit_turu ?: yakit)
                                            putInt("arac_yili", vehicleInfo?.arac_yili ?: aracYili)
                                            putFloat("karbon_emisyon", vehicleInfo?.karbon_emisyon ?: 0f)
                                        }
                                        findNavController().navigate(R.id.action_vehicleAddFragment_to_resultFragment, bundle)
                                    } else {
                                        // Plaka okuma başarısız, kaydedilen verilerle result ekranına git
                                        val bundle = Bundle().apply {
                                            putString("plaka", savedVehicle.plaka)
                                            putString("marka", savedVehicle.marka)
                                            putString("model", savedVehicle.model)
                                            putString("renk", savedVehicle.renk)
                                            putString("yakit_turu", savedVehicle.yakit_turu)
                                            putInt("arac_yili", savedVehicle.arac_yili)
                                            putFloat("karbon_emisyon", savedVehicle.karbon_emisyon ?: 0f)
                                        }
                                        findNavController().navigate(R.id.action_vehicleAddFragment_to_resultFragment, bundle)
                                    }
                                }

                                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                    // Plaka okuma hatası, kaydedilen verilerle result ekranına git
                                    val bundle = Bundle().apply {
                                        putString("plaka", savedVehicle.plaka)
                                        putString("marka", savedVehicle.marka)
                                        putString("model", savedVehicle.model)
                                        putString("renk", savedVehicle.renk)
                                        putString("yakit_turu", savedVehicle.yakit_turu)
                                        putInt("arac_yili", savedVehicle.arac_yili)
                                        putFloat("karbon_emisyon", savedVehicle.karbon_emisyon ?: 0f)
                                    }
                                    findNavController().navigate(R.id.action_vehicleAddFragment_to_resultFragment, bundle)
                                }
                            })
                        } ?: run {
                            // Resim yoksa kaydedilen verilerle result ekranına git
                            val bundle = Bundle().apply {
                                putString("plaka", savedVehicle.plaka)
                                putString("marka", savedVehicle.marka)
                                putString("model", savedVehicle.model)
                                putString("renk", savedVehicle.renk)
                                putString("yakit_turu", savedVehicle.yakit_turu)
                                putInt("arac_yili", savedVehicle.arac_yili)
                                putFloat("karbon_emisyon", savedVehicle.karbon_emisyon ?: 0f)
                            }
                            findNavController().navigate(R.id.action_vehicleAddFragment_to_resultFragment, bundle)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Sunucu hatası: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Bağlantı hatası: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupBrandSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            brands
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.markaSpinner.adapter = adapter
    }

    private fun setupModelSpinnerEmpty() { // <-- eklendi
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf<String>() // Boş liste
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.modelSpinner.adapter = adapter
    }

    private fun selectBrandInSpinner(brand: String) {
        val position = brands.indexOf(brand)
        if (position != -1) {
            binding.markaSpinner.setSelection(position)
            updateModelSpinner(brand)
        }
    }

    private fun updateModelSpinner(brand: String) {
        val models = brandModels[brand] ?: listOf<String>()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            models
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.modelSpinner.adapter = adapter
    }

    private fun setupFuelTypeSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            fuelTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.yakitSpinner.adapter = adapter
    }

    private fun setupVehicleTypeSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            vehicleTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.aracTipiSpinner.adapter = adapter
    }

    private fun setupColorSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            colors
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.renkSpinner.adapter = adapter
    }

    private fun setupYearSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            years
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.yilSpinner.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
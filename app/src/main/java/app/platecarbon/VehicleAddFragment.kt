package app.platecarbon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.platecarbon.R
import app.platecarbon.VehicleHistoryManager
import app.platecarbon.databinding.FragmentVehicleAddBinding
import app.platecarbon.model.VehicleRequest
import app.platecarbon.model.GenericResponse
import app.platecarbon.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VehicleAddFragment : Fragment() {

    private var _binding: FragmentVehicleAddBinding? = null
    private val binding get() = _binding!!

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

        val gelenMarka = arguments?.getString("marka")
        if (!gelenMarka.isNullOrBlank() && gelenMarka != "unknown") {

            val duzenlenmisMarka = gelenMarka.lowercase().replaceFirstChar { it.uppercase() }
            binding.markaEditText.setText(duzenlenmisMarka)
        }

        binding.kaydetBtn.setOnClickListener {
            val plaka = binding.plakaEditText.text.toString().trim()
            val marka = binding.markaEditText.text.toString().trim()
            val model = binding.modelEditText.text.toString().trim()
            val renk = binding.renkEditText.text.toString().trim()
            val yakit = binding.yakitEditText.text.toString().trim()
            val aractipi = binding.aracTipiEditText.text.toString().trim()
            val yilStr = binding.yilEditText.text.toString().trim()

            val aracYili = yilStr.toIntOrNull()

            if (plaka.isBlank() || marka.isBlank() || model.isBlank() ||
                renk.isBlank() || yakit.isBlank() || aracYili == null) {
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

            binding.btnBack.setOnClickListener {
                findNavController().navigate(R.id.action_vehicleAddFragment_to_cameraFragment)
            }

            ApiClient.plateService.addVehicle(vehicleRequest).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message ?: "Başarıyla kaydedildi"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                        // VehicleHistoryManager'ı başlat
                        VehicleHistoryManager.initialize(requireContext())

                        // API'den dönen araç bilgilerini al (karbon emisyon dahil)
                        val savedVehicle = response.body()?.arac ?: vehicleRequest

                        // Araç bilgilerini geçmişe ekle
                        VehicleHistoryManager.addVehicleToHistory(savedVehicle)

                        // Result ekranına yönlendir (API'den dönen verilerle)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
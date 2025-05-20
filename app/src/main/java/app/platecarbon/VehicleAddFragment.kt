package app.platecarbon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.platecarbon.R
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

        binding.kaydetBtn.setOnClickListener {
            val plaka = binding.plakaEditText.text.toString().trim()
            val marka = binding.markaEditText.text.toString().trim()
            val model = binding.modelEditText.text.toString().trim()
            val renk = binding.renkEditText.text.toString().trim()
            val yakit = binding.yakitEditText.text.toString().trim()
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

                        // İsteğe bağlı: geri dön
                        findNavController().navigateUp()
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

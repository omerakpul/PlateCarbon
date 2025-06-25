package com.example.plr

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import app.platecarbon.R
import app.platecarbon.databinding.FragmentResultBinding
import android.graphics.Color

class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE

        val args = arguments
        val karbonEmisyon = args?.getFloat("karbon_emisyon") ?: 0f
        Log.d("ResultFragment", "Karbon Emisyon Değeri: $karbonEmisyon")

        binding.tvPlateNumber.text = args?.getString("plaka")
        binding.tvBrand.text = args?.getString("marka")
        binding.tvModel.text = args?.getString("model")
        binding.tvYear.text = args?.getInt("arac_yili")?.toString() ?: "-"
        binding.tvEmissionValue.text = "$karbonEmisyon g/km CO₂"

        // Karbon emisyon değerine göre durum ve renk belirle
        val (emissionStatus, statusColor) = when {
            karbonEmisyon <= 100 -> "Düşük Emisyon" to Color.parseColor("#4CAF50") // Yeşil
            karbonEmisyon <= 150 -> "Normal Aralık" to Color.parseColor("#4CAF50") // Yeşil
            karbonEmisyon <= 200 -> "Yüksek Emisyon" to Color.parseColor("#FF9800") // Turuncu/Sarı
            else -> "Çok Yüksek Emisyon" to Color.parseColor("#F44336") // Kırmızı
        }

        binding.tvEmissionStatus.text = emissionStatus
        binding.tvEmissionStatus.setTextColor(statusColor)

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_resultFragment_to_cameraFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
        _binding = null
    }
}
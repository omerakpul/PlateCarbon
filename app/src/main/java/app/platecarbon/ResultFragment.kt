package com.example.plr

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import app.platecarbon.R
import app.platecarbon.databinding.FragmentResultBinding

class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    private var isHighRisk = false

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
        binding.tvPlateNumber.text = args?.getString("plaka")
        binding.tvBrand.text = args?.getString("marka")
        binding.tvModel.text = args?.getString("model")
        binding.tvYear.text = args?.getInt("arac_yili")?.toString() ?: "-"


        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_resultFragment_to_cameraFragment)
        }

        binding.btnRisk.setOnClickListener {
            isHighRisk = !isHighRisk
            binding.btnRisk.text = if (isHighRisk) "Remove Risk" else "Mark as High Risk"
            Toast.makeText(context, if (isHighRisk) "High Risk" else "Normal", Toast.LENGTH_SHORT).show()
        }

        binding.btnGeneratePdf.setOnClickListener {
            Toast.makeText(context, "PDF oluşturuluyor...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
        _binding = null
    }
}
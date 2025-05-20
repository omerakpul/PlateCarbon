package app.platecarbon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import app.platecarbon.databinding.FragmentVehicleDetailBinding
import app.platecarbon.ui.VehicleDetailFragment
import app.platecarbon.ui.VehicleAddFragment

class VehicleDetailFragment : Fragment() {

    private var _binding: FragmentVehicleDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        binding.plakaText.text = args?.getString("plaka")
        binding.markaText.text = args?.getString("marka")
        binding.modelText.text = args?.getString("model")
        binding.renkText.text = args?.getString("renk")
        binding.yakitText.text = args?.getString("yakit_turu")
        binding.yilText.text = args?.getInt("arac_yili")?.toString() ?: "-"


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

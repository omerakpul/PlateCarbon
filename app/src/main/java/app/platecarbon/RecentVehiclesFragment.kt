package app.platecarbon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.platecarbon.databinding.FragmentRecentVehiclesBinding
import app.platecarbon.model.VehicleRequest

class RecentVehiclesFragment : Fragment() {

    private var _binding: FragmentRecentVehiclesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bu kısımda veri API'den çekilecek
        // Şimdilik dummy veri göster:
        val dummyList = listOf(
            VehicleRequest("34ABC123", "Toyota", "Corolla", "Beyaz", "Benzin", 2021),
            VehicleRequest("06DEF456", "Renault", "Clio", "Gri", "Dizel", 2020),
            VehicleRequest("35XYZ789", "Honda", "Civic", "Siyah", "Benzin", 2022)
        )

        // RecyclerView kurulumunu burada yapacağız (istersen yardımcı olayım)
        Toast.makeText(requireContext(), "Son 5 araç gelecektir", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

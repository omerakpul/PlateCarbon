package app.platecarbon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.platecarbon.VehicleHistoryItem
import app.platecarbon.databinding.FragmentRecentVehiclesBinding
import app.platecarbon.VehicleHistoryManager
import app.platecarbon.adapter.VehicleHistoryAdapter

class RecentVehiclesFragment : Fragment() {

    private var _binding: FragmentRecentVehiclesBinding? = null
    private val binding get() = _binding!!
    private lateinit var vehicleHistoryAdapter: VehicleHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // VehicleHistoryManager'ı başlat
        VehicleHistoryManager.initialize(requireContext())

        setupRecyclerView()
        loadVehicleHistory()
    }

    private fun setupRecyclerView() {
        vehicleHistoryAdapter = VehicleHistoryAdapter(emptyList()) { historyItem ->
            // Araç tıklandığında yapılacak işlem
            onVehicleClicked(historyItem)
        }

        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vehicleHistoryAdapter
        }
    }

    private fun loadVehicleHistory() {
        val history = VehicleHistoryManager.getVehicleHistory()

        if (history.isNotEmpty()) {
            showVehicles(history)
        } else {
            showEmptyState()
        }
    }

    private fun showVehicles(history: List<VehicleHistoryItem>) {
        binding.progressBar.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
        binding.rvVehicles.visibility = View.VISIBLE

        vehicleHistoryAdapter.updateVehicles(history)
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.rvVehicles.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    private fun onVehicleClicked(historyItem: VehicleHistoryItem) {
        // Araç detaylarını göster
        Toast.makeText(requireContext(), "${historyItem.vehicle.plaka} seçildi", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
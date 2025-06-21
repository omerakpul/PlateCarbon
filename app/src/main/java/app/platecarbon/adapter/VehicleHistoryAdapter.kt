package app.platecarbon.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.platecarbon.databinding.ItemVehicleBinding
import app.platecarbon.VehicleHistoryItem
import app.platecarbon.VehicleHistoryManager

class VehicleHistoryAdapter(
    private var historyItems: List<VehicleHistoryItem>,
    private val onVehicleClick: (VehicleHistoryItem) -> Unit
) : RecyclerView.Adapter<VehicleHistoryAdapter.VehicleHistoryViewHolder>() {

    inner class VehicleHistoryViewHolder(private val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(historyItem: VehicleHistoryItem) {
            val vehicle = historyItem.vehicle

            binding.tvPlateNumber.text = vehicle.plaka
            binding.tvBrandModel.text = "${vehicle.marka} ${vehicle.model}"
            binding.tvColor.text = vehicle.renk
            binding.tvFuelType.text = vehicle.yakit_turu
            binding.tvYear.text = vehicle.arac_yili.toString()

            // Tarama zamanını göster
            binding.tvScanTime.text = VehicleHistoryManager.formatScanTime(historyItem.scannedAt)

            // Debug: Karbon emisyon değerini logla
            Log.d("VehicleHistoryAdapter", "Plaka: ${vehicle.plaka}, Karbon Emisyon: ${vehicle.karbon_emisyon}")

            // Karbon emisyonu varsa göster
            vehicle.karbon_emisyon?.let { emission ->
                Log.d("VehicleHistoryAdapter", "Karbon emisyon gösteriliyor: $emission")
                binding.tvEmission.text = "${emission} g/km"
                binding.tvEmission.visibility = android.view.View.VISIBLE
            } ?: run {
                Log.d("VehicleHistoryAdapter", "Karbon emisyon null, gizleniyor")
                binding.tvEmission.visibility = android.view.View.GONE
            }

            // Tıklama olayı
            binding.root.setOnClickListener {
                onVehicleClick(historyItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleHistoryViewHolder {
        val binding = ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleHistoryViewHolder, position: Int) {
        holder.bind(historyItems[position])
    }

    override fun getItemCount(): Int = historyItems.size

    fun updateVehicles(newHistoryItems: List<VehicleHistoryItem>) {
        historyItems = newHistoryItems
        notifyDataSetChanged()
    }
}
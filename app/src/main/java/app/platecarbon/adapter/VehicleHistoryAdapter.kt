package app.platecarbon.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.platecarbon.databinding.ItemVehicleLogBinding
import app.platecarbon.VehicleHistoryItem
import app.platecarbon.model.VehicleLog
import java.text.SimpleDateFormat
import java.util.Locale

class VehicleHistoryAdapter(
    private var historyItems: List<VehicleHistoryItem>,
    private val onVehicleClick: (VehicleHistoryItem) -> Unit
) : RecyclerView.Adapter<VehicleHistoryAdapter.VehicleHistoryViewHolder>() {

    private val dbFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    // Log bilgilerini tutmak için map
    private val vehicleLogs = mutableMapOf<String, VehicleLog>()

    // Sadece log bilgisi olan araçları göster
    private val filteredHistoryItems: List<VehicleHistoryItem>
        get() = historyItems.filter { vehicleLogs.containsKey(it.vehicle.plaka) }

    inner class VehicleHistoryViewHolder(private val binding: ItemVehicleLogBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(historyItem: VehicleHistoryItem) {
            val vehicle = historyItem.vehicle
            val plaka = vehicle.plaka

            // Plaka
            binding.tvPlateNumber.text = plaka

            // Log bilgileri varsa göster
            val vehicleLog = vehicleLogs[plaka]

            if (vehicleLog != null) {
                // API'den gelen log bilgilerini göster
                binding.tvCarbonEmission.text = "%.2f g/km".format(vehicleLog.carbonEmission ?: 0f)
                binding.tvCarbonEmission.visibility = View.VISIBLE

                binding.tvEntryTime.text = "Giriş: ${formatDate(vehicleLog.entryTime)}"
                binding.tvExitTime.text = if(vehicleLog.exitTime != null) "Çıkış: ${formatDate(vehicleLog.exitTime)}" else "Çıkış yapılmadı"
                binding.tvTotalTime.text = "${vehicleLog.totalTimeSeconds ?: 0}s\nToplam"
                binding.tvParkedTime.text = "${vehicleLog.totalParkedSeconds ?: 0}s\nPark"
                binding.tvMovingTime.text = "${vehicleLog.actualMovingSeconds ?: 0}s\nHareket"
            } else {
                // Log bilgisi yoksa hiçbir şey gösterme (bu durumda bu item gösterilmeyecek)
                binding.tvCarbonEmission.visibility = View.GONE
                binding.tvEntryTime.text = ""
                binding.tvExitTime.text = ""
                binding.tvTotalTime.text = ""
                binding.tvParkedTime.text = ""
                binding.tvMovingTime.text = ""
            }
            binding.root.setOnClickListener(null)
        }

        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "Bilinmiyor"
            return try {
                val date = dbFormat.parse(dateString)
                if (date != null) displayFormat.format(date) else "Hatalı Tarih"
            } catch (e: Exception) {
                dateString
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleHistoryViewHolder {
        val binding = ItemVehicleLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleHistoryViewHolder, position: Int) {
        val filteredItems = filteredHistoryItems
        if (position < filteredItems.size) {
            holder.bind(filteredItems[position])
        }
    }

    override fun getItemCount(): Int = filteredHistoryItems.size

    fun updateVehicles(newHistoryItems: List<VehicleHistoryItem>) {
        historyItems = newHistoryItems
        notifyDataSetChanged()
    }

    // Log bilgilerini güncellemek için yeni metod
    fun updateVehicleWithLog(plaka: String, vehicleLog: VehicleLog) {
        vehicleLogs[plaka] = vehicleLog
        notifyDataSetChanged() // Tüm listeyi yenile çünkü filtreleme değişebilir
    }
}
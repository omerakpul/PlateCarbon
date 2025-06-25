package app.platecarbon.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.platecarbon.VehicleHistoryItem
import app.platecarbon.databinding.ItemVehicleLogBinding
import app.platecarbon.model.VehicleLog
import java.text.SimpleDateFormat
import java.util.*

class VehicleHistoryAdapter(
    private var historyItems: List<VehicleHistoryItem>,
    private val onVehicleClick: (VehicleHistoryItem) -> Unit
) : RecyclerView.Adapter<VehicleHistoryAdapter.VehicleHistoryViewHolder>() {

    // API'den gelen tarih formatı (ör: 2025-06-22T13:21:23.759914)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    // Göstermek istediğimiz tarih formatı
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    // Log bilgilerini tutmak için map
    private val vehicleLogs = mutableMapOf<String, VehicleLog>()

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
                if (vehicleLog.carbonEmission != null) {
                    binding.tvCarbonEmission.text = "%.2f g/km".format(vehicleLog.carbonEmission)
                    binding.tvCarbonEmission.visibility = View.VISIBLE
                } else {
                    binding.tvCarbonEmission.visibility = View.GONE
                }

                binding.tvEntryTime.text = "Giriş: ${formatDate(vehicleLog.entryTime)}"
                binding.tvExitTime.text = if (vehicleLog.exitTime != null) "Çıkış: ${formatDate(vehicleLog.exitTime)}" else "Çıkış yapılmadı"

                // Süreleri dinamik olarak ve etiketleriyle birlikte göster
                binding.tvTotalTime.text = "${formatDuration(vehicleLog.totalTimeSeconds)}\nToplam"
                binding.tvParkedTime.text = "${formatDuration(vehicleLog.totalParkedSeconds)}\nPark"
                binding.tvMovingTime.text = "${formatDuration(vehicleLog.actualMovingSeconds)}\nHareket"

            } else {
                // Log bilgisi yoksa temel araç bilgilerini göster
                binding.tvCarbonEmission.visibility = View.GONE
                binding.tvEntryTime.text = "Marka: ${vehicle.marka}"
                binding.tvExitTime.text = "Model: ${vehicle.model}"
                binding.tvTotalTime.text = ""
                binding.tvParkedTime.text = ""
                binding.tvMovingTime.text = ""
            }
            binding.root.setOnClickListener(null)
        }

        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "Bilinmiyor"
            return try {
                // Milisaniyeyi ve 'T'yi dikkate alacak şekilde formatı ayarla
                val adjustedDateString = dateString.substringBefore(".")
                val date = apiDateFormat.parse(adjustedDateString)
                if (date != null) displayDateFormat.format(date) else "Hatalı Tarih"
            } catch (e: Exception) {
                Log.e("VehicleHistoryAdapter", "Tarih parse hatası: $dateString", e)
                dateString
            }
        }

        private fun formatDuration(totalSeconds: Int?): String {
            if (totalSeconds == null || totalSeconds < 0) return "0dk"

            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60

            return when {
                hours > 0 -> "${hours}sa ${minutes}dk"
                minutes > 0 -> "${minutes}dk"
                else -> "1dk" // 1 dakikadan az ise 1dk göster
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleHistoryViewHolder {
        val binding = ItemVehicleLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleHistoryViewHolder, position: Int) {
        holder.bind(historyItems[position])
    }

    override fun getItemCount(): Int = historyItems.size

    fun updateVehicles(newHistoryItems: List<VehicleHistoryItem>) {
        this.historyItems = newHistoryItems
        notifyDataSetChanged()
    }

    // Tek bir aracın log bilgisini güncellemek için metod
    fun updateVehicleWithLog(plaka: String, vehicleLog: VehicleLog) {
        vehicleLogs[plaka] = vehicleLog
        notifyDataSetChanged()
    }

    // Tüm araç log bilgilerini güncellemek için yeni metod
    fun updateAllVehicleLogs(allLogs: Map<String, VehicleLog>) {
        vehicleLogs.clear()
        vehicleLogs.putAll(allLogs)
        notifyDataSetChanged()
    }
}
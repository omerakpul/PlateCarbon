package app.platecarbon.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import app.platecarbon.databinding.ItemVehicleLogBinding
import app.platecarbon.model.VehicleLog
import app.platecarbon.R
import java.text.SimpleDateFormat
import java.util.*

class VehicleHistoryAdapter(
    private var vehicleLogs: List<VehicleLog>,
    private val onVehicleClick: (VehicleLog) -> Unit
) : RecyclerView.Adapter<VehicleHistoryAdapter.VehicleHistoryViewHolder>() {

    // API'den gelen tarih formatı (ör: 2025-06-22T13:21:23.759914)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    // Göstermek istediğimiz tarih formatı
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    inner class VehicleHistoryViewHolder(private val binding: ItemVehicleLogBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vehicleLog: VehicleLog) {
            val plaka = vehicleLog.plate ?: "Bilinmeyen Plaka"

            // Plaka
            binding.tvPlateNumber.text = plaka

            // API'den gelen log bilgilerini göster
            if (vehicleLog.carbonEmission != null) {
                val emissionText = formatEmissionValue(vehicleLog.carbonEmission)
                binding.tvCarbonEmission.text = emissionText
                binding.tvCarbonEmission.visibility = View.VISIBLE

                // CO₂ değerine göre renk belirle
                val emissionColor = getEmissionColor(vehicleLog.carbonEmission)
                binding.tvCarbonEmission.setTextColor(emissionColor)
            } else {
                binding.tvCarbonEmission.visibility = View.GONE
            }

            binding.tvEntryTime.text = "Giriş: ${formatDate(vehicleLog.entryTime)}"
            binding.tvExitTime.text = if (vehicleLog.exitTime != null) "Çıkış: ${formatDate(vehicleLog.exitTime)}" else "Çıkış yapılmadı"

            // Süreleri dinamik olarak ve etiketleriyle birlikte göster
            binding.tvTotalTime.text = "${formatDuration(vehicleLog.totalTimeSeconds)}\nToplam"
            binding.tvParkedTime.text = "${formatDuration(vehicleLog.totalParkedSeconds)}\nPark"
            binding.tvMovingTime.text = "${formatDuration(vehicleLog.actualMovingSeconds)}\nHareket"

            binding.root.setOnClickListener {
                onVehicleClick(vehicleLog)
            }
        }

        // CO2 değerine göre renk belirleme fonksiyonu
        private fun getEmissionColor(emission: Float): Int {
            return when {
                emission < 1000 -> ContextCompat.getColor(itemView.context, R.color.emission_low) // Yeşil - Düşük emisyon
                emission < 1750 -> ContextCompat.getColor(itemView.context, R.color.emission_high) // Turuncu - Orta emisyon
                else -> ContextCompat.getColor(itemView.context, R.color.emission_very_high) // Kırmızı - Yüksek emisyon
            }
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

        // CO₂ değerini formatla - 1000'den büyükse kg cinsinden göster, küsürat sadece gerektiğinde
        private fun formatEmissionValue(emission: Float): String {
            return if (emission >= 1000) {
                val kgValue = emission / 1000
                if (kgValue == kgValue.toInt().toFloat()) {
                    // Tam sayı ise küsürat gösterme
                    "${kgValue.toInt()} kg CO₂"
                } else {
                    // Küsürat varsa 2 basamak göster
                    "%.2f kg CO₂".format(kgValue)
                }
            } else {
                if (emission == emission.toInt().toFloat()) {
                    // Tam sayı ise küsürat gösterme
                    "${emission.toInt()} g CO₂"
                } else {
                    // Küsürat varsa 2 basamak göster
                    "%.2f g CO₂".format(emission)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleHistoryViewHolder {
        val binding = ItemVehicleLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleHistoryViewHolder, position: Int) {
        holder.bind(vehicleLogs[position])
    }

    override fun getItemCount(): Int = vehicleLogs.size

    fun updateVehicles(newVehicleLogs: List<VehicleLog>) {
        this.vehicleLogs = newVehicleLogs
        notifyDataSetChanged()
    }
}
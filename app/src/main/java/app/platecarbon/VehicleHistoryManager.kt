package app.platecarbon

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import app.platecarbon.model.VehicleRequest
import java.util.Date

data class VehicleHistoryItem(
    val vehicle: VehicleRequest,
    val scannedAt: Long = System.currentTimeMillis()
)

object VehicleHistoryManager {
    private const val PREF_NAME = "vehicle_history"
    private const val KEY_HISTORY = "vehicle_history_list"
    private const val MAX_HISTORY_SIZE = 20

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    fun initialize(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    // Plaka okunduğunda çağrılacak
    fun addVehicleToHistory(vehicle: VehicleRequest) {
        val history = getVehicleHistory().toMutableList()

        // Aynı plaka varsa kaldır (tekrar eklemek için)
        history.removeAll { it.vehicle.plaka == vehicle.plaka }

        // Yeni aracı başa ekle
        val historyItem = VehicleHistoryItem(vehicle)
        history.add(0, historyItem)

        // Maksimum sayıyı aşan araçları kaldır
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        // Kaydet
        saveHistory(history)
    }

    // Geçmişi getir
    fun getVehicleHistory(): List<VehicleHistoryItem> {
        val json = sharedPreferences.getString(KEY_HISTORY, "[]")
        val type = object : TypeToken<List<VehicleHistoryItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Geçmişi kaydet (public yapıldı)
    fun saveHistory(history: List<VehicleHistoryItem>) {
        val json = gson.toJson(history)
        sharedPreferences.edit().putString(KEY_HISTORY, json).apply()
    }

    // Geçmişi temizle
    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_HISTORY).apply()
    }

    // Tarih formatını döndür
    fun formatScanTime(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diffInMinutes = (now.time - timestamp) / (1000 * 60)

        return when {
            diffInMinutes < 1 -> "Az önce"
            diffInMinutes < 60 -> "$diffInMinutes dakika önce"
            diffInMinutes < 1440 -> "${diffInMinutes / 60} saat önce"
            else -> "${diffInMinutes / 1440} gün önce"
        }
    }
}
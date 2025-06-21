package app.platecarbon.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.platecarbon.VehicleHistoryItem
import app.platecarbon.databinding.FragmentRecentVehiclesBinding
import app.platecarbon.VehicleHistoryManager
import app.platecarbon.adapter.VehicleHistoryAdapter
import app.platecarbon.model.SingleVehicleResponse
import app.platecarbon.model.VehicleLog
import app.platecarbon.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class RecentVehiclesFragment : Fragment() {

    private var _binding: FragmentRecentVehiclesBinding? = null
    private val binding get() = _binding!!
    private lateinit var vehicleHistoryAdapter: VehicleHistoryAdapter
    private val vehicleLogs = mutableMapOf<String, VehicleLog>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        VehicleHistoryManager.initialize(requireContext())
        setupRecyclerView()
        setupReportButtons()
        loadVehicleHistory()
    }

    private fun setupRecyclerView() {
        vehicleHistoryAdapter = VehicleHistoryAdapter(emptyList()) { historyItem ->
            // Tıklama olayı yok
        }
        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vehicleHistoryAdapter
        }
    }

    private fun setupReportButtons() {
        // CSV Rapor butonu
        binding.btnExportCsv?.setOnClickListener {
            exportToCSV()
        }
        // PDF Rapor butonu
        binding.btnExportPdf?.setOnClickListener {
            exportToPDF()
        }
    }

    private fun loadVehicleHistory() {
        val history = VehicleHistoryManager.getVehicleHistory()

        if (history.isNotEmpty()) {
            showVehicles(history)
            loadAllVehicleLogs(history)
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

    private fun loadAllVehicleLogs(history: List<VehicleHistoryItem>) {
        history.forEach { historyItem ->
            fetchVehicleLog(historyItem.vehicle.plaka)
        }
    }

    private fun fetchVehicleLog(plaka: String) {
        Log.d("RecentVehiclesFragment", "Araç log bilgileri getiriliyor: $plaka")

        val call = ApiClient.plateService.getVehicleLog(plaka)
        call.enqueue(object : Callback<SingleVehicleResponse> {
            override fun onResponse(
                call: Call<SingleVehicleResponse>,
                response: Response<SingleVehicleResponse>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    if (apiResponse != null) {
                        if (apiResponse.found && apiResponse.log != null) {
                            vehicleLogs[plaka] = apiResponse.log
                            updateVehicleWithLogData(plaka, apiResponse.log)
                        } else {
                            removeVehicleFromHistory(plaka)
                        }
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Bilinmeyen hata"
                    Log.e("RecentVehiclesFragment", "API hatası: ${response.code()} - $errorMessage")
                }
            }

            override fun onFailure(call: Call<SingleVehicleResponse>, t: Throwable) {
                Log.e("RecentVehiclesFragment", "Bağlantı hatası: ${t.message}")
            }
        })
    }

    private fun updateVehicleWithLogData(plaka: String, vehicleLog: VehicleLog) {
        vehicleHistoryAdapter.updateVehicleWithLog(plaka, vehicleLog)
    }

    private fun removeVehicleFromHistory(plaka: String) {
        val history = VehicleHistoryManager.getVehicleHistory().toMutableList()
        history.removeAll { it.vehicle.plaka == plaka }
        VehicleHistoryManager.saveHistory(history)
        vehicleHistoryAdapter.updateVehicles(history)
    }

    // CSV Raporu oluştur
    private fun exportToCSV() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "vehicle_report_$timestamp.csv"
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            FileWriter(file).use { writer ->
                // CSV başlıkları
                writer.append("Plaka,Giriş Zamanı,Çıkış Zamanı,Toplam Süre (s),Park Süresi (s),Hareket Süresi (s),CO2 Emisyonu (g/km),Marka,Model,Renk,Yakıt Türü,Araç Yılı\n")

                // Araç verileri
                val history = VehicleHistoryManager.getVehicleHistory()
                history.forEach { historyItem ->
                    val plaka = historyItem.vehicle.plaka
                    val vehicleLog = vehicleLogs[plaka]

                    if (vehicleLog != null) {
                        writer.append("${vehicleLog.plate},")
                        writer.append("${vehicleLog.entryTime},")
                        writer.append("${vehicleLog.exitTime ?: "Çıkış yapılmadı"},")
                        writer.append("${vehicleLog.totalTimeSeconds ?: 0},")
                        writer.append("${vehicleLog.totalParkedSeconds ?: 0},")
                        writer.append("${vehicleLog.actualMovingSeconds ?: 0},")
                        writer.append("${vehicleLog.carbonEmission ?: 0},")
                        writer.append("${historyItem.vehicle.marka},")
                        writer.append("${historyItem.vehicle.model},")
                        writer.append("${historyItem.vehicle.renk},")
                        writer.append("${historyItem.vehicle.yakit_turu},")
                        writer.append("${historyItem.vehicle.arac_yili}\n")
                    }
                }
            }

            shareFile(file, "text/csv", "CSV Raporu")
            Toast.makeText(requireContext(), "CSV raporu oluşturuldu", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("RecentVehiclesFragment", "CSV oluşturma hatası: ${e.message}")
            Toast.makeText(requireContext(), "CSV oluşturma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // PDF Raporu oluştur
    private fun exportToPDF() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "vehicle_report_$timestamp.pdf"
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            // PDF oluşturma kodu buraya gelecek (iText7 kullanarak)
            createPDFReport(file)

            shareFile(file, "application/pdf", "PDF Raporu")
            Toast.makeText(requireContext(), "PDF raporu oluşturuldu", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("RecentVehiclesFragment", "PDF oluşturma hatası: ${e.message}")
            Toast.makeText(requireContext(), "PDF oluşturma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPDFReport(file: File) {
        // PDF oluşturma implementasyonu
        // iText7 kullanarak profesyonel PDF raporu oluştur
    }

    // Dosyayı paylaş
    private fun shareFile(file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Raporu paylaş"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
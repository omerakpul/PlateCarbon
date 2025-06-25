package app.platecarbon.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
import app.platecarbon.model.AllVehicleLogsResponse
import app.platecarbon.model.VehicleLog
import app.platecarbon.network.ApiClient
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFont
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

    // Tarih formatları
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    // Periyodik güncelleme için
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateAllVehicleLogs()
            handler.postDelayed(this, 30000) // 30 saniyede bir güncelle
        }
    }

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

        // Periyodik güncellemeyi başlat
        startPeriodicUpdate()
    }

    private fun startPeriodicUpdate() {
        handler.postDelayed(updateRunnable, 30000) // 30 saniyede bir güncelle
    }

    private fun stopPeriodicUpdate() {
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateAllVehicleLogs() {
        fetchAllVehicleLogs()
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
            fetchAllVehicleLogs()
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

    private fun fetchAllVehicleLogs() {
        Log.d("RecentVehiclesFragment", "Tüm araç log bilgileri getiriliyor")

        val call = ApiClient.plateService.getAllVehicleLogs()
        call.enqueue(object : Callback<AllVehicleLogsResponse> {
            override fun onResponse(
                call: Call<AllVehicleLogsResponse>,
                response: Response<AllVehicleLogsResponse>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    if (apiResponse != null && apiResponse.found && apiResponse.logs != null) {
                        // Tüm log bilgilerini temizle ve yenilerini ekle
                        vehicleLogs.clear()

                        apiResponse.logs.forEach { vehicleLog ->
                            // Debug log'ları ekle
                            Log.d("RecentVehiclesFragment", "Araç: ${vehicleLog.plate}")
                            Log.d("RecentVehiclesFragment", "Emisyon: ${vehicleLog.carbonEmission}")
                            Log.d("RecentVehiclesFragment", "Giriş: ${vehicleLog.entryTime}")
                            Log.d("RecentVehiclesFragment", "Çıkış: ${vehicleLog.exitTime}")

                            // Plate null ise geç
                            if (vehicleLog.plate != null) {
                                vehicleLogs[vehicleLog.plate] = vehicleLog
                            } else {
                                Log.w("RecentVehiclesFragment", "Plate null, bu kayıt atlanıyor")
                            }
                        }

                        // Adapter'ı güncelle
                        vehicleHistoryAdapter.updateAllVehicleLogs(vehicleLogs)

                        Log.d("RecentVehiclesFragment", "${vehicleLogs.size} araç log bilgisi güncellendi")
                    } else {
                        Log.d("RecentVehiclesFragment", "Araç log bilgisi bulunamadı")
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Bilinmeyen hata"
                    Log.e("RecentVehiclesFragment", "API hatası: ${response.code()} - $errorMessage")
                }
            }

            override fun onFailure(call: Call<AllVehicleLogsResponse>, t: Throwable) {
                Log.e("RecentVehiclesFragment", "Bağlantı hatası: ${t.message}")
            }
        })
    }

    // Tarih formatını düzenle
    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Bilinmiyor"
        return try {
            // Milisaniyeyi ve 'T'yi dikkate alacak şekilde formatı ayarla
            val adjustedDateString = dateString.substringBefore(".")
            val date = apiDateFormat.parse(adjustedDateString)
            if (date != null) displayDateFormat.format(date) else "Hatalı Tarih"
        } catch (e: Exception) {
            Log.e("RecentVehiclesFragment", "Tarih parse hatası: $dateString", e)
            dateString // Hata olursa orijinal string'i döndür
        }
    }

    // CSV Raporu oluştur
    private fun exportToCSV() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "vehicle_report_$timestamp.csv"

            // Downloads klasörüne kaydet
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                // CSV başlıkları
                writer.append("Plaka,Giriş Zamanı,Çıkış Zamanı,Toplam Süre (s),Park Süresi (s),Hareket Süresi (s),CO2 Emisyonu (g/km),Marka,Model,Renk,Yakıt Türü,Araç Yılı\n")

                // Araç verileri
                val history = VehicleHistoryManager.getVehicleHistory()
                history.forEach { historyItem ->
                    val plaka = historyItem.vehicle.plaka
                    val vehicleLog = vehicleLogs[plaka]

                    if (vehicleLog != null) {
                        // Plaka bilgisini historyItem'dan al, vehicleLog'dan değil
                        writer.append("\"${plaka}\",")
                        writer.append("\"${formatDate(vehicleLog.entryTime)}\",")
                        writer.append("\"${if (vehicleLog.exitTime != null) formatDate(vehicleLog.exitTime) else "Çıkış yapılmadı"}\",")
                        writer.append("${vehicleLog.totalTimeSeconds ?: 0},")
                        writer.append("${vehicleLog.totalParkedSeconds ?: 0},")
                        writer.append("${vehicleLog.actualMovingSeconds ?: 0},")
                        writer.append("${vehicleLog.carbonEmission ?: 0},")
                        writer.append("\"${historyItem.vehicle.marka ?: ""}\",")
                        writer.append("\"${historyItem.vehicle.model ?: ""}\",")
                        writer.append("\"${historyItem.vehicle.renk ?: ""}\",")
                        writer.append("\"${historyItem.vehicle.yakit_turu ?: ""}\",")
                        writer.append("${historyItem.vehicle.arac_yili ?: 0}\n")
                    }
                }
            }

            // Dosyayı medya tarayıcısına bildir
            notifyMediaScanner(file)

            Toast.makeText(requireContext(), "CSV raporu Downloads klasörüne kaydedildi: $fileName", Toast.LENGTH_LONG).show()

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

            // Downloads klasörüne kaydet
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            // PDF oluşturma kodu
            createPDFReport(file)

            // Dosyayı medya tarayıcısına bildir
            notifyMediaScanner(file)

            Toast.makeText(requireContext(), "PDF raporu Downloads klasörüne kaydedildi: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("RecentVehiclesFragment", "PDF oluşturma hatası: ${e.message}")
            Toast.makeText(requireContext(), "PDF oluşturma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPDFReport(file: File) {
        try {
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // Türkçe karakterler için farklı font deneyelim
            val font = try {
                PdfFontFactory.createFont(StandardFonts.HELVETICA, PdfEncodings.UTF8)
            } catch (e: Exception) {
                // Fallback olarak varsayılan font
                PdfFontFactory.createFont(StandardFonts.HELVETICA)
            }

            // Başlık
            val title = Paragraph("Arac Raporu") // Türkçe karakterleri kaldırdık
                .setFont(font)
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
            document.add(title)

            // Tarih
            val date = Paragraph("Olusturulma Tarihi: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
                .setFont(font)
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(date)

            document.add(Paragraph("\n"))

            // Tablo oluştur - daha az sütun ve daha geniş
            val table = Table(8) // 8 sütuna düşürdük
            table.setWidth(UnitValue.createPercentValue(100f))

            // Tablo başlıkları - Türkçe karakterleri kaldırdık
            val headers = arrayOf(
                "Plaka", "Giris", "Cikis", "Toplam Sure",
                "CO2 Emisyonu", "Marka", "Model", "Yil"
            )

            headers.forEach { header ->
                val cell = Cell().add(Paragraph(header).setFont(font).setBold())
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
                cell.setTextAlignment(TextAlignment.CENTER)
                cell.setFontSize(8f) // Font boyutunu küçülttük
                table.addHeaderCell(cell)
            }

            // Araç verileri
            val history = VehicleHistoryManager.getVehicleHistory()
            history.forEach { historyItem ->
                val plaka = historyItem.vehicle.plaka
                val vehicleLog = vehicleLogs[plaka]

                if (vehicleLog != null) {
                    // Plaka
                    addCell(table, plaka, 8f, font)

                    // Giriş zamanı
                    addCell(table, formatDate(vehicleLog.entryTime), 8f, font)

                    // Çıkış zamanı
                    addCell(table, if (vehicleLog.exitTime != null) formatDate(vehicleLog.exitTime) else "Cikis yapilmadi", 8f, font)

                    // Toplam süre (dakika cinsinden)
                    val totalMinutes = (vehicleLog.totalTimeSeconds ?: 0) / 60
                    addCell(table, "${totalMinutes}dk", 8f, font)

                    // CO2 Emisyonu
                    addCell(table, "${vehicleLog.carbonEmission ?: 0} g/km", 8f, font)

                    // Marka
                    addCell(table, historyItem.vehicle.marka ?: "", 8f, font)

                    // Model
                    addCell(table, historyItem.vehicle.model ?: "", 8f, font)

                    // Yıl
                    addCell(table, (historyItem.vehicle.arac_yili ?: 0).toString(), 8f, font)
                }
            }

            document.add(table)

            // Özet bilgiler
            document.add(Paragraph("\n"))
            val totalVehicles = history.size
            val totalEmissions = history.sumOf { historyItem ->
                val plaka = historyItem.vehicle.plaka
                vehicleLogs[plaka]?.carbonEmission?.toDouble() ?: 0.0
            }

            val summary = Paragraph("""
                Ozet Bilgiler:
                Toplam Arac Sayisi: $totalVehicles
                Toplam CO2 Emisyonu: $totalEmissions g/km
            """.trimIndent())
                .setFont(font)
                .setFontSize(14f)
                .setBold()
            document.add(summary)

            document.close()

        } catch (e: Exception) {
            Log.e("RecentVehiclesFragment", "PDF oluşturma hatası: ${e.message}")
            throw e
        }
    }

    // Hücre ekleme yardımcı metodu
    private fun addCell(table: Table, text: String, fontSize: Float, font: PdfFont) {
        val cell = Cell().add(Paragraph(text).setFont(font).setFontSize(fontSize))
        cell.setTextAlignment(TextAlignment.CENTER)
        cell.setPadding(2f) // Padding'i azalttık
        table.addCell(cell)
    }

    // Dosyayı medya tarayıcısına bildir (Downloads klasöründe görünmesi için)
    private fun notifyMediaScanner(file: File) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val uri = Uri.fromFile(file)
        intent.data = uri
        requireContext().sendBroadcast(intent)
    }

    // Dosyayı paylaş (opsiyonel olarak tutuyoruz)
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
        stopPeriodicUpdate() // Periyodik güncellemeyi durdur
        _binding = null
    }
}
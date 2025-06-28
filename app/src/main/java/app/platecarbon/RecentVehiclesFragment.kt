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
import app.platecarbon.databinding.FragmentRecentVehiclesBinding
import app.platecarbon.adapter.VehicleHistoryAdapter
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
    private val vehicleLogs = mutableListOf<VehicleLog>()

    // Tarih formatları
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    // Periyodik güncelleme için
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            fetchAllVehicleLogs()
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

        setupRecyclerView()
        setupButtons()

        // Fragment'a girerken otomatik olarak verileri yükle
        fetchAllVehicleLogs()

        // Periyodik güncellemeyi başlat
        startPeriodicUpdate()
    }

    override fun onResume() {
        super.onResume()
        // Fragment'a her girişte verileri yeniden yükle
        fetchAllVehicleLogs()
    }

    private fun startPeriodicUpdate() {
        handler.postDelayed(updateRunnable, 30000) // 30 saniyede bir güncelle
    }

    private fun stopPeriodicUpdate() {
        handler.removeCallbacks(updateRunnable)
    }

    private fun setupRecyclerView() {
        vehicleHistoryAdapter = VehicleHistoryAdapter(emptyList()) { vehicleLog ->
            // Tıklama olayı - şu an için boş
        }
        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vehicleHistoryAdapter
        }
    }

    private fun setupButtons() {
        // Yenile butonu
        binding.btnRefresh.setOnClickListener {
            refreshData()
        }

        // CSV Rapor butonu
        binding.btnExportCsv?.setOnClickListener {
            exportToCSV()
        }

        // PDF Rapor butonu
        binding.btnExportPdf?.setOnClickListener {
            exportToPDF()
        }
    }

    // Yenileme fonksiyonu
    private fun refreshData() {
        // Progress bar'ı göster
        binding.progressBar.visibility = View.VISIBLE

        // API'den güncel log bilgilerini al
        fetchAllVehicleLogs()

        // 2 saniye sonra progress bar'ı gizle
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.GONE
        }, 2000)
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
                            // Plate null ise geç
                            if (vehicleLog.plate != null) {
                                vehicleLogs.add(vehicleLog)
                            }
                        }

                        // Son giren en üstte olacak şekilde sırala (giriş zamanına göre azalan sıralama)
                        vehicleLogs.sortByDescending { vehicleLog ->
                            try {
                                // Giriş zamanını parse et
                                val adjustedDateString = vehicleLog.entryTime.substringBefore(".")
                                val date = apiDateFormat.parse(adjustedDateString)
                                date?.time ?: 0L
                            } catch (e: Exception) {
                                Log.e("RecentVehiclesFragment", "Tarih parse hatası: ${vehicleLog.entryTime}", e)
                                0L // Hata olursa en sona koy
                            }
                        }

                        // Adapter'ı güncelle
                        vehicleHistoryAdapter.updateVehicles(vehicleLogs)

                        if (vehicleLogs.isNotEmpty()) {
                            showVehicles()
                        } else {
                            showEmptyState()
                        }
                    } else {
                        showEmptyState()
                    }
                } else {
                    showEmptyState()
                }
            }

            override fun onFailure(call: Call<AllVehicleLogsResponse>, t: Throwable) {
                showEmptyState()
            }
        })
    }

    private fun showVehicles() {
        binding.progressBar.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
        binding.rvVehicles.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.rvVehicles.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
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
                writer.append("Plaka,Giriş Zamanı,Çıkış Zamanı,Toplam Süre (dk),Park Süresi (dk),Hareket Süresi (dk),CO2 Emisyonu\n")

                // Araç verileri
                vehicleLogs.forEach { vehicleLog ->
                    if (vehicleLog.plate != null) {
                        writer.append("\"${vehicleLog.plate}\",")
                        writer.append("\"${formatDate(vehicleLog.entryTime)}\",")
                        writer.append("\"${if (vehicleLog.exitTime != null) formatDate(vehicleLog.exitTime) else "Çıkış yapılmadı"}\",")
                        // Toplam süre (dakika cinsinden)
                        val totalMinutes = (vehicleLog.totalTimeSeconds ?: 0) / 60
                        writer.append("${totalMinutes},")
                        // Park süresi (dakika cinsinden)
                        val parkMinutes = (vehicleLog.totalParkedSeconds ?: 0) / 60
                        writer.append("${parkMinutes},")
                        // Hareket süresi (dakika cinsinden)
                        val movingMinutes = (vehicleLog.actualMovingSeconds ?: 0) / 60
                        writer.append("${movingMinutes},")
                        writer.append("${formatEmissionValueForReport(vehicleLog.carbonEmission ?: 0f)}\n")
                    }
                }
            }
            notifyMediaScanner(file)
            Toast.makeText(requireContext(), "CSV raporu Downloads klasörüne kaydedildi: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
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
            Toast.makeText(requireContext(), "PDF oluşturma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPDFReport(file: File) {
        try {
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // Basit font kullan
            val font = PdfFontFactory.createFont("Helvetica")

            // Baslik
            val title = Paragraph("Arac Raporu")
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

            // Tablo olustur - daha az sutun ve daha genis
            val table = Table(7) // 7 sutuna dusurduk
            table.setWidth(UnitValue.createPercentValue(100f))

            // Tablo basliklari - normal CO2 kullan
            val headers = arrayOf(
                "Plaka", "Giris", "Cikis", "Toplam Sure",
                "CO2 Emisyonu", "Park Sure", "Hareket Sure"
            )

            headers.forEach { header ->
                val cell = Cell().add(Paragraph(header).setFont(font).setBold())
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
                cell.setTextAlignment(TextAlignment.CENTER)
                cell.setFontSize(8f) // Font boyutunu kuculttuk
                table.addHeaderCell(cell)
            }

            // Arac verileri
            vehicleLogs.forEach { vehicleLog ->
                if (vehicleLog.plate != null) {
                    // Plaka
                    addCell(table, vehicleLog.plate, 8f, font)

                    // Giris zamani
                    addCell(table, formatDate(vehicleLog.entryTime), 8f, font)

                    // Cikis zamani
                    addCell(table, if (vehicleLog.exitTime != null) formatDate(vehicleLog.exitTime) else "Cikis yapilmadi", 8f, font)

                    // Toplam sure (dakika cinsinden)
                    val totalMinutes = (vehicleLog.totalTimeSeconds ?: 0) / 60
                    addCell(table, "${totalMinutes}dk", 8f, font)

                    // CO2 Emisyonu
                    addCell(table, formatEmissionValueForReport(vehicleLog.carbonEmission ?: 0f), 8f, font)

                    // Park sure
                    val parkMinutes = (vehicleLog.totalParkedSeconds ?: 0) / 60
                    addCell(table, "${parkMinutes}dk", 8f, font)

                    // Hareket sure
                    val movingMinutes = (vehicleLog.actualMovingSeconds ?: 0) / 60
                    addCell(table, "${movingMinutes}dk", 8f, font)
                }
            }

            document.add(table)

            // Ozet bilgiler
            document.add(Paragraph("\n"))
            val totalVehicles = vehicleLogs.size
            val totalEmissions = vehicleLogs.sumOf { (it.carbonEmission ?: 0f).toDouble() }

            val summary = Paragraph("""
                Ozet Bilgiler:
                Toplam Arac Sayisi: $totalVehicles
                Toplam CO2 Emisyonu: ${formatEmissionValueForReport(totalEmissions.toFloat())}
            """.trimIndent())
                .setFont(font)
                .setFontSize(14f)
                .setBold()
            document.add(summary)

            document.close()

        } catch (e: Exception) {
            Log.e("RecentVehiclesFragment", "PDF olusturma hatasi: ${e.message}")
            throw e
        }
    }

    // Hucre ekleme yardimci metodu
    private fun addCell(table: Table, text: String, fontSize: Float, font: PdfFont) {
        val cell = Cell().add(Paragraph(text).setFont(font).setFontSize(fontSize))
        cell.setTextAlignment(TextAlignment.CENTER)
        cell.setPadding(2f) // Padding'i azalttik
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

    // Raporlar için CO2 değerini formatla - normal CO2 kullan
    private fun formatEmissionValueForReport(emission: Float): String {
        return if (emission >= 1000) {
            val kgValue = emission / 1000
            if (kgValue == kgValue.toInt().toFloat()) {
                "${kgValue.toInt()} kg CO2"
            } else {
                "%.2f kg CO2".format(kgValue)
            }
        } else {
            if (emission == emission.toInt().toFloat()) {
                "${emission.toInt()} g CO2"
            } else {
                "%.2f g CO2".format(emission)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPeriodicUpdate() // Periyodik güncellemeyi durdur
        _binding = null
    }
}
package app.platecarbon.network

data class VehicleInfo(
    val plaka: String?,
    val marka: String?,
    val model: String?,
    val renk: String?,
    val yakit_turu: String?,
    val arac_yili: Int?
)

data class ApiResponse(
    val found: Boolean,
    val plaka: String?,
    val arac: VehicleInfo?
)


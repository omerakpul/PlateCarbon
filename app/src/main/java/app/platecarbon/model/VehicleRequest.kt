package app.platecarbon.model

import java.util.Date

data class VehicleRequest(
    val plaka: String,
    val marka: String,
    val model: String,
    val renk: String,
    val yakit_turu: String,
    val arac_tipi: String,
    val karbon_emisyon: Float?,
    val arac_yili: Int
)

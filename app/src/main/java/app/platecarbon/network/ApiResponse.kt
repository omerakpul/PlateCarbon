package app.platecarbon.network

data class ApiResponse(
    val message: String?,
    val plates: List<Plate>?
)

data class Plate(
    val plate_number: String?,
    val bounding_box: List<Int>?
)
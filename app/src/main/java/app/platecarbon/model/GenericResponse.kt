package app.platecarbon.model

data class GenericResponse(
    val message: String,
    val arac: VehicleRequest? = null
)

package app.platecarbon.model

data class AllVehicleLogsResponse(
    val found: Boolean,
    val logs: List<VehicleLog>?,
    val message: String?
)
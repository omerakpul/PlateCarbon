package app.platecarbon.model
import com.google.gson.annotations.SerializedName

data class SingleVehicleResponse(
    @SerializedName("found") val found: Boolean,
    @SerializedName("log") val log: VehicleLog?
)
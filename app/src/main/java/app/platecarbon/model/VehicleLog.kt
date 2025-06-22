package app.platecarbon.model

import com.google.gson.annotations.SerializedName

data class VehicleLog(
    @SerializedName("plaka") val plate: String?, // String? yaptık çünkü null gelebiliyor
    @SerializedName("entry_time") val entryTime: String,
    @SerializedName("exit_time") val exitTime: String?,
    @SerializedName("total_time_seconds") val totalTimeSeconds: Int?,
    @SerializedName("total_parked_seconds") val totalParkedSeconds: Int?,
    @SerializedName("actual_moving_seconds") val actualMovingSeconds: Int?,
    @SerializedName("carbon_emission") val carbonEmission: Float?
)
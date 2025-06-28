package app.platecarbon.network

import app.platecarbon.model.AllVehicleLogsResponse
import app.platecarbon.model.VehicleRequest
import app.platecarbon.model.GenericResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface PlateApiService {
    @Multipart
    @POST("upload")
    fun uploadPlateImage(
        @Part image: MultipartBody.Part
    ): Call<ApiResponse>

    @POST("add_vehicle")
    fun addVehicle(
        @Body vehicle: VehicleRequest
    ): Call<GenericResponse>


    @GET("vehicle_logs")
    fun getAllVehicleLogs(): Call<AllVehicleLogsResponse>
}

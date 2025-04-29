package app.platecarbon.network

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PlateApiService {
    @Multipart
    @POST("upload") // Python tarafı hangi endpointi verirse onu yazacaksın
    fun uploadPlateImage(
        @Part image: MultipartBody.Part
    ): Call<ApiResponse>
}

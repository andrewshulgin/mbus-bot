import com.google.gson.Gson
import com.google.gson.JsonArray
import io.sentry.Sentry
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream


object ZxingApi {
    fun recognizeBarcode(stream: InputStream): String? {
        return try {
            val client = OkHttpClient()
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "file", stream.readAllBytes().toRequestBody()).build()
            val request = Request.Builder().url("https://zxing.rmrf.co/read?format=DataMatrix").post(body)
                .addHeader("accept", "application/json").build()
            val response = client.newCall(request).execute()
            if (response.code != 200) {
                Sentry.captureMessage(response.body?.string() ?: response.code.toString())
                return null
            }
            val gson = Gson()
            val results = gson.fromJson(response.body?.string(), JsonArray::class.java)
            if (results.isEmpty) {
                return null
            }
            results[0].asJsonObject?.get("text")?.asString
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            Sentry.captureException(e)
            null
        }
    }
}

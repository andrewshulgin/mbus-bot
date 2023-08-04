import com.aspose.barcode.cloud.ApiClient
import com.aspose.barcode.cloud.api.BarcodeApi
import com.aspose.barcode.cloud.model.DecodeBarcodeType
import com.aspose.barcode.cloud.requests.PostBarcodeRecognizeFromUrlOrContentRequest
import java.io.File

class Aspose(clientId: String, clientSecret: String) {
    private val client: ApiClient
    private val api: BarcodeApi

    init {
        client = ApiClient(clientId, clientSecret)
        client.setReadTimeout(5 * 60 * 1000)
        api = BarcodeApi(client)
    }

    fun recognizeBarcode(barcodeImage: File): String? {
        val recognizeRequest = PostBarcodeRecognizeFromUrlOrContentRequest()
        recognizeRequest.type = DecodeBarcodeType.DATAMATRIX.value
        recognizeRequest.readTinyBarcodes = true
        recognizeRequest.image = barcodeImage
        val result = api.postBarcodeRecognizeFromUrlOrContent(recognizeRequest)
        if (result.barcodes.size > 0) {
            return result.barcodes[0].barcodeValue
        }
        return null
    }
}

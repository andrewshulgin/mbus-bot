import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.datamatrix.DataMatrixReader
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


object Zxing {
    fun recognizeBarcode(file: File): String? {
        return try {
            val bufferedImage: BufferedImage = ImageIO.read(file)
            val source: LuminanceSource = BufferedImageLuminanceSource(bufferedImage)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = DataMatrixReader().decode(bitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }
}

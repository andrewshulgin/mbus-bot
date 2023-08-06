import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import com.google.zxing.multi.MultipleBarcodeReader
import io.sentry.Sentry
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.*
import javax.imageio.ImageIO


object Zxing {
    fun recognizeBarcode(stream: InputStream): String? {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        hints[DecodeHintType.TRY_HARDER] = true
        hints[DecodeHintType.POSSIBLE_FORMATS] = BarcodeFormat.DATA_MATRIX
        val hintsPure = EnumMap(hints)
        hintsPure[DecodeHintType.PURE_BARCODE] = true
        return try {
            val bufferedImage: BufferedImage = ImageIO.read(stream)

            val source: LuminanceSource = BufferedImageLuminanceSource(bufferedImage)
            val bitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            val results: MutableList<Result> = mutableListOf()
            val reader = MultiFormatReader()
            var savedException: ReaderException? = null
            try {
                val multiReader: MultipleBarcodeReader = GenericMultipleBarcodeReader(reader)
                val res = multiReader.decodeMultiple(bitmap, hints)
                if (res != null) {
                    results.addAll(res)
                }
            } catch (e: ReaderException) {
                savedException = e
            }
            if (results.isEmpty()) {
                try {
                    val res = reader.decode(bitmap, hintsPure)
                    if (res != null) {
                        results.add(res)
                    }
                } catch (e: ReaderException) {
                    savedException = e
                }
            }
            if (results.isEmpty()) {
                try {
                    val res = reader.decode(bitmap, hints)
                    if (res != null) {
                        results.add(res)
                    }
                } catch (e: ReaderException) {
                    savedException = e
                }
            }
            if (results.isEmpty()) {
                try {
                    val hybridBitmap = BinaryBitmap(HybridBinarizer(source))
                    val res = reader.decode(hybridBitmap, hints)
                    if (res != null) {
                        results.add(res)
                    }
                } catch (e: ReaderException) {
                    savedException = e
                }
            }
            if (results.isEmpty()) {
                if (savedException !is NotFoundException && savedException != null) {
                    savedException.printStackTrace(System.err)
                    Sentry.captureException(savedException)
                }
                return null
            } else {
                return results[0].text
            }
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            Sentry.captureException(e)
            null
        }
    }
}

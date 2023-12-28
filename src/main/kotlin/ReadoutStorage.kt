import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReadoutStorage(private val readoutStorageDir: String) {
    fun getLatestReadout(meter: String): Double? {
        val rootDir = File(readoutStorageDir)
        val latestFile = rootDir.walk()
            .filter {
                it.isFile &&
                        it.extension.uppercase() == "CSV" &&
                        it.name.contains("VALUES", true)
            }
            .maxByOrNull { it.absolutePath }
        if (latestFile == null) {
            return null
        }

        val inputStream = latestFile.inputStream()
        val csvParser = CSVFormat.Builder.create(CSVFormat.DEFAULT).apply {
            setHeader("secondary_addr", "value", "errors", "timestamp")
        }.build().parse(inputStream.reader())

        var value: Double? = null

        for (csvRecord: CSVRecord in csvParser.records) {
            if (csvRecord["secondary_addr"].equals(meter)) {
                val timestamp = csvRecord["timestamp"].toLongOrNull()?.let {
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC)
                }
                val oneDayAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(1)
                if (timestamp != null && timestamp.isAfter(oneDayAgo)) {
                    value = csvRecord["value"].toDoubleOrNull()
                }
                break
            }
        }

        inputStream.close()

        return value
    }
}

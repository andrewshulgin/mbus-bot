import org.apache.commons.csv.CSVFormat
import java.io.File


class NetworkCsv(private val csvPath: String) {
    private var network: MutableMap<String, Pair<Int, Int>> = mutableMapOf()

    init {
        reload()
    }

    fun getMeter(meter: String): Pair<Int, Int>? {
        if (meter in network.keys) return network[meter]
        return null
    }

    fun reload() {
        network.clear()
        val inputStream = File(csvPath).inputStream()
        CSVFormat.Builder.create(CSVFormat.DEFAULT).apply {
            setHeader()
            setSkipHeaderRecord(true)
        }.build().parse(inputStream.reader()).associateByTo(
            network, { it["secondary_addr"] },
            { it["mbus_line"].toInt() to it["primary_addr"].toInt() }
        )
        inputStream.close()
    }
}

import org.openmuc.jmbus.DataRecord
import org.openmuc.jmbus.MBusConnection
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder
import org.openmuc.jmbus.VariableDataStructure
import java.io.IOException

class Mbus(port: String, baud: Int, timeout: Int) {
    private var builder: MBusSerialBuilder? = null

    init {
        builder = MBusConnection.newSerialBuilder(port).setBaudrate(baud).setTimeout(timeout)
    }

    fun read(address: Int): Double? {
        var result: Double? = null
        builder?.build().use { connection ->
            var success = false
            var counter = 0
            while (!success && counter < 10) {
                try {
                    connection?.linkReset(address)
                    success = true
                } catch (e: IOException) {
                    Thread.sleep(100)
                    counter++
                }
            }
        }
        builder?.build().use { connection ->
            if (connection != null) {
                var success = false
                var counter = 0
                var data: VariableDataStructure? = null
                do {
                    while (!success) {
                        try {
                            data = connection.read(address)
                            success = true
                        } catch (e: IOException) {
                            Thread.sleep(100)
                            counter++
                            if (counter == 10) {
                                throw e
                            }
                        }
                    }
                    for (record in data!!.dataRecords) {
                        if (result == null && record.description == DataRecord.Description.VOLUME) {
                            result = record.scaledDataValue
                        }
                    }
                } while (data!!.moreRecordsFollow())
                return result
            }
        }
        return null
    }
}

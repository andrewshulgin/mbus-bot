import org.openmuc.jmbus.DataRecord
import org.openmuc.jmbus.MBusConnection
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder
import org.openmuc.jmbus.MBusConnection.MBusTcpBuilder
import org.openmuc.jmbus.SecondaryAddress
import org.openmuc.jmbus.VariableDataStructure
import java.io.IOException

abstract class Mbus {

    abstract fun getConnection(): MBusConnection
    abstract val retryCount: Int

    fun read(address: Int): Double? {
        var result: Double? = null
        getConnection().use { connection ->
            var success = false
            var counter = 0
            while (!success && counter < retryCount) {
                try {
                    connection.linkReset(address)
                    success = true
                } catch (e: IOException) {
                    Thread.sleep(100)
                    counter++
                }
            }
        }
        getConnection().use { connection ->
            var data: VariableDataStructure? = null
            do {
                var success = false
                var counter = 0
                while (!success) {
                    try {
                        data = connection.read(address)
                        success = true
                    } catch (e: IOException) {
                        Thread.sleep(100)
                        counter++
                        if (counter == retryCount) {
                            throw e
                        }
                    }
                }
                if (data != null) {
                    for (record in data.dataRecords) {
                        if (result == null && record.description == DataRecord.Description.VOLUME) {
                            result = record.scaledDataValue
                        }
                    }
                }
            } while (data != null && data.moreRecordsFollow())
            return result
        }
    }

    fun read(address: String): Double? {
        var result: Double? = null
        var success = false
        var counter = 0
        while (!success) {
            try {
                @OptIn(ExperimentalStdlibApi::class) getConnection().use { connection ->
                    var data: VariableDataStructure
                    connection.selectComponent(
                        SecondaryAddress.newFromLongHeader(
                            "ffffffff2423900e".hexToByteArray(),
                            0
                        )
                    )
                    Thread.sleep(200)
                    connection.sendLongMessage(0xfd, 0x73, 0xbb, byteArrayOf(), true)
                    Thread.sleep(200)
                    connection.linkReset(0xfd)
                    Thread.sleep(200)
                    connection.selectComponent(SecondaryAddress.newFromLongHeader(address.hexToByteArray(), 0))
                    do {
                        Thread.sleep(200)
                        data = connection.read(0xfd)
                        for (record in data.dataRecords) {
                            if (result == null && record.description == DataRecord.Description.VOLUME) {
                                result = record.scaledDataValue
                            }
                        }
                    } while (data.moreRecordsFollow())
                    connection.linkReset(0xfd)
                    success = true
                }
            } catch (e: IOException) {
                Thread.sleep(1000)
                counter++
                if (counter == retryCount) {
                    throw e
                }
            }
        }
        return result
    }
}

class MbusSerial(port: String, baud: Int, timeout: Int, override val retryCount: Int = 10) : Mbus() {
    private var builder: MBusSerialBuilder

    init {
        builder = MBusConnection.newSerialBuilder(port).setBaudrate(baud).setTimeout(timeout)
    }

    override fun getConnection(): MBusConnection {
        return builder.build()
    }
}

class MbusTcp(host: String, port: Int, timeout: Int, override val retryCount: Int = 3) : Mbus() {
    private var builder: MBusTcpBuilder

    init {
        builder = MBusConnection.newTcpBuilder(host, port).setTimeout(timeout)
    }

    override fun getConnection(): MBusConnection {
        return builder.build()
    }
}

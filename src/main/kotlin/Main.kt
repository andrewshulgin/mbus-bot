import io.sentry.Sentry
import sun.misc.Signal
import java.net.SocketTimeoutException
import kotlin.concurrent.thread
import kotlin.random.Random

fun main() {
    var emulateMbus = false
    var mbusGpioMuxChannels: List<Int>? = null
    var serialPort: String? = null
    var mbusLockFile: String? = null
    var serialBaud = 2400
    var mbusTimeout = 1000
    var telegramToken = ""
    var networkCsvPath: String? = null
    var sentryDsn: String? = null
    var mbusTcpAddresses: MutableList<Pair<String, Int>>? = null
    if (System.getenv("EMULATE_MBUS") != null) {
        emulateMbus = listOf("1", "TRUE", "YES").contains(System.getenv("EMULATE_MBUS").uppercase())
    }
    if (System.getenv("MBUS_GPIO_MUX") != null) {
        mbusGpioMuxChannels = System.getenv("MBUS_GPIO_MUX").split(",").map { Integer.parseInt(it) }
    }
    if (System.getenv("MBUS_PORT") != null) {
        serialPort = System.getenv("MBUS_PORT")
    }
    if (System.getenv("MBUS_BAUD") != null) {
        serialBaud = Integer.parseInt(System.getenv("MBUS_BAUD"))
    }
    if (System.getenv("MBUS_LOCK_FILE") != null) {
        mbusLockFile = System.getenv("MBUS_LOCK_FILE")
    }
    if (System.getenv("MBUS_TCP_ADDRESSES") != null) {
        val addresses = System.getenv("MBUS_TCP_ADDRESSES")?.split(",")
        mbusTcpAddresses = mutableListOf()
        if (addresses != null) {
            for (address in addresses) {
                val hostPort = address.split(":")
                mbusTcpAddresses.add(hostPort[0] to Integer.parseInt(hostPort[1]))
            }
        }
    }
    if (System.getenv("MBUS_TIMEOUT") != null) {
        mbusTimeout = Integer.parseInt(System.getenv("MBUS_TIMEOUT"))
    }
    if (System.getenv("NETWORK_CSV_PATH") != null) {
        networkCsvPath = System.getenv("NETWORK_CSV_PATH")
    }
    if (System.getenv("TELEGRAM_TOKEN") != null) {
        telegramToken = System.getenv("TELEGRAM_TOKEN")
    }
    if (System.getenv("SENTRY_DSN") != null) {
        sentryDsn = System.getenv("SENTRY_DSN")
    }

    if (!sentryDsn.isNullOrEmpty()) {
        Sentry.init { options ->
            options.dsn = sentryDsn
            options.tracesSampleRate = 1.0
        }
    }

    val gpioMux = if (mbusGpioMuxChannels.isNullOrEmpty()) null else GpioMux(mbusGpioMuxChannels)
    val mbusLock = if (mbusLockFile.isNullOrEmpty()) null else Lock(mbusLockFile)
    val mbus: MutableList<Mbus> = mutableListOf()
    if (!mbusTcpAddresses.isNullOrEmpty()) {
        for (address in mbusTcpAddresses) {
            mbus.add(MbusTcp(address.first, address.second, mbusTimeout))
        }
    } else if (!serialPort.isNullOrEmpty()) {
        mbus.add(MbusSerial(serialPort, serialBaud, mbusTimeout))
    }
    val csv = if (networkCsvPath.isNullOrEmpty()) null else NetworkCsv(networkCsvPath)
    val tg = Telegram(telegramToken, { meter ->
        try {
            var result: Double? = null
            if (!serialPort.isNullOrEmpty()) {
                val csvResult = csv?.getMeter(meter)
                if (csvResult != null) {
                    val muxChannel = csvResult.first
                    val primaryAddress = csvResult.second
                    result = if (!emulateMbus) {
                        if (mbusLock == null || !mbusLock.isLocked()) {
                            gpioMux?.switch(muxChannel)
                            val readResult = mbus[0].read(primaryAddress)
                            gpioMux?.release()
                            readResult
                        } else {
                            throw Error("MbusLock")
                        }
                    } else {
                        System.err.println(
                            String.format(
                                "Emulating readout of meter %s at channel %d address %d",
                                meter,
                                muxChannel,
                                primaryAddress
                            )
                        )
                        Random.nextDouble(1.000, 999.000)
                    }
                }
            }
            if (result == null && !mbusTcpAddresses.isNullOrEmpty()) {
                if (meter.length == 6) {
                    val ff = 0xff.toByte()

                    @OptIn(ExperimentalStdlibApi::class)
                    val meterAddressMask =
                        (meter.hexToByteArray().reversedArray() + byteArrayOf(ff, ff, ff, ff, ff)).toHexString()
                    if (!emulateMbus) {
                        val threads = mbus.map {
                            thread {
                                try {
                                    val currentResult = it.read(meterAddressMask)
                                    if (currentResult != null && result == null) {
                                        result = currentResult
                                    }
                                } catch (_: SocketTimeoutException) {
                                } catch (e: Exception) {
                                    e.printStackTrace(System.err)
                                    Sentry.captureException(e)
                                }
                            }
                        }
                        while (result == null) {
                            Thread.sleep(100)
                            var running = 0
                            for (t in threads) {
                                if (t.isAlive) {
                                    running++
                                }
                            }
                            if (running == 0) {
                                break
                            }
                        }
                        if (result == null) {
                            throw Error(String.format("No data for ", meterAddressMask))
                        }
                    } else {
                        System.err.println(
                            String.format(
                                "Emulating readout of meter %s (%s)",
                                meter,
                                meterAddressMask
                            )
                        )
                        Random.nextDouble(1.000, 999.000)
                    }
                }
            }
            result
        } catch (e: Exception) {
            Sentry.captureException(e)
            throw e
        }
    }, { stream ->
        try {
            return@Telegram ZxingApi.recognizeBarcode(stream)
        } catch (e: Exception) {
            Sentry.captureException(e)
            null
        }
    })
    Runtime.getRuntime().addShutdownHook(Thread {
        tg.stop()
    })
    Signal.handle(Signal("HUP")) {
        csv?.reload()
    }
    tg.run()
}

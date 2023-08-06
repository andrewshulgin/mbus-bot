import io.sentry.Sentry
import sun.misc.Signal
import kotlin.random.Random

fun main(args: Array<String>) {
    var emulateMbus = false
    var serialPort = "/dev/ttyS3"
    var serialBaud = 2400
    var serialTime = 1000
    var telegramToken = ""
    var networkCsvPath = "/etc/mbus-bot/network.csv"
    var sentryDsn = ""
    if (System.getenv("EMULATE_MBUS") != null) {
        emulateMbus = listOf("1", "TRUE", "YES").contains(System.getenv("EMULATE_MBUS").uppercase())
    }
    if (System.getenv("MBUS_PORT") != null) {
        serialPort = System.getenv("MBUS_PORT")
    }
    if (System.getenv("MBUS_BAUD") != null) {
        serialBaud = Integer.parseInt(System.getenv("MBUS_BAUD"))
    }
    if (System.getenv("MBUS_TIME") != null) {
        serialTime = Integer.parseInt(System.getenv("MBUS_TIME"))
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
    if (args.isNotEmpty()) {
        serialPort = args[0]
    }
    if (args.size > 1) {
        serialBaud = Integer.parseInt(args[1])
    }
    if (args.size > 2) {
        serialTime = Integer.parseInt(args[2])
    }

    if (sentryDsn.isNotEmpty()) {
        Sentry.init { options ->
            options.dsn = sentryDsn
            options.tracesSampleRate = 1.0
        }
    }

    val mux = if (emulateMbus) null else Mux()
    val mbus = if (emulateMbus) null else Mbus(serialPort, serialBaud, serialTime)
    val csv = NetworkCsv(networkCsvPath)
    val tg = Telegram(telegramToken, { meter ->
        try {
            val result = csv.getMeter(meter) ?: return@Telegram null
            if (emulateMbus) {
                System.err.println(
                    String.format(
                        "Emulating readout of meter %s at channel %d address %d",
                        meter,
                        result.first,
                        result.second
                    )
                )
                return@Telegram Random.nextDouble(1.000, 999.000)
            } else {
                mux!!.switch(result.first)
                return@Telegram mbus!!.read(result.second)
            }
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
        csv.reload()
    }
    tg.run()
}

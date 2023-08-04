import io.sentry.Sentry
import sun.misc.Signal

fun main(args: Array<String>) {
    var serialPort = "/dev/ttyS3"
    var serialBaud = 2400
    var serialTime = 1000
    var telegramToken = ""
    var networkCsvPath = "/etc/mbus-bot/network.csv"
    var sentryDsn = ""
    var asposeClientId = ""
    var asposeClientSecret = ""
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
    if (System.getenv("ASPOSE_CLIENT_ID") != null) {
        asposeClientId = System.getenv("ASPOSE_CLIENT_ID")
    }
    if (System.getenv("ASPOSE_CLIENT_SECRET") != null) {
        asposeClientSecret = System.getenv("ASPOSE_CLIENT_SECRET")
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

    val aspose = if (asposeClientId.isNotEmpty() && asposeClientSecret.isNotEmpty()) {
        Aspose(asposeClientId, asposeClientSecret)
    } else {
        null
    }

    val mux = Mux()
    val mbus = Mbus(serialPort, serialBaud, serialTime)
    val csv = NetworkCsv(networkCsvPath)
    val tg = Telegram(telegramToken, { meter ->
        try {
            val result = csv.getMeter(meter) ?: return@Telegram null
            mux.switch(result.first)
            return@Telegram mbus.read(result.second)
        } catch (e: Exception) {
            Sentry.captureException(e)
            throw e
        }
    }, { file ->
        try {
            var result = Zxing.recognizeBarcode(file)
            if (result == null && aspose != null) {
                result = aspose.recognizeBarcode(file)
            }
            return@Telegram result
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

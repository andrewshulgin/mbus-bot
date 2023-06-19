import io.sentry.Sentry

fun main(args: Array<String>) {
    var serialPort = "/dev/ttyS3"
    var serialBaud = 2400
    var serialTime = 1000
    var telegramToken = ""
    var dbName = "mbus"
    var dbUser = "mbus"
    var dbPass = "mbus"
    var sentryDsn = ""
    if (System.getenv("MBUS_PORT") != null) {
        serialPort = System.getenv("MBUS_PORT")
    }
    if (System.getenv("MBUS_BAUD") != null) {
        serialBaud = Integer.parseInt(System.getenv("MBUS_BAUD"))
    }
    if (System.getenv("MBUS_TIME") != null) {
        serialTime = Integer.parseInt(System.getenv("MBUS_TIME"))
    }
    if (System.getenv("DB_NAME") != null) {
        dbName = System.getenv("DB_NAME")
    }
    if (System.getenv("DB_USER") != null) {
        dbUser = System.getenv("DB_USER")
    }
    if (System.getenv("DB_PASS") != null) {
        dbPass = System.getenv("DB_PASS")
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
    val mux = Mux()
    val mbus = Mbus(serialPort, serialBaud, serialTime)
    val db = Database(dbName, dbUser, dbPass)
    val tg = Telegram(telegramToken) { meter ->
        try {
            val result = db.getMeter(meter) ?: return@Telegram null
            mux.switch(result.first)
            return@Telegram mbus.read(result.second)
        } catch (e: Exception) {
            Sentry.captureException(e)
            throw e
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        tg.stop()
    })
    tg.run()
}

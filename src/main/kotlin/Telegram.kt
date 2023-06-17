import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.TelegramFile

class Telegram(telegramToken: String, handler: (meter: String) -> Double?) {
    private var tg: Bot? = null

    init {
        tg = bot {
            token = telegramToken
            dispatch {
                command("start") {
                    tg?.sendPhoto(
                        chatId = ChatId.fromId(message.chat.id),
                        Telegram::class.java.getResource("mbus.jpg").let {
                            TelegramFile.ByByteArray(it!!.readBytes())
                        },
                        caption = "Привіт! \uD83D\uDC4B\n" +
                                "Я вмію зчитувати покази з лічильників\n" +
                                "\uD83D\uDD35 холодного та \uD83D\uDD34 гарячого " +
                                "водопостачання.\n" +
                                "Відправ мені *номер* з наліпки на лічильнику, що " +
                                "виділено \uD83D\uDFE9 *зеленим кольором* у прикладі вище \uD83D\uDC46",
                        parseMode = ParseMode.MARKDOWN
                    )
                }
                text {
                    if (text != "/start") {
                        try {
                            val result = handler(text)
                            if (result == null) {
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id),
                                    text = "На жаль, я не зміг знайти лічильник за таким номером \uD83D\uDE14"
                                )
                            } else {
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id),
                                    text = String.format("Поточний показник: %.3f ㎥", result)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace(System.err)
                            bot.sendMessage(
                                ChatId.fromId(message.chat.id),
                                text = "На жаль, сталася помилка при зчитуванні показників \uD83D\uDE14. " +
                                        "Будь ласка, спробуй пізніше \uD83D\uDE4F"
                            )
                        }
                    }
                }
            }
        }
    }

    fun run() {
        tg?.startPolling()
    }

    fun stop() {
        tg?.stopPolling()
    }
}

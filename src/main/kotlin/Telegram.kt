import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import io.sentry.Sentry

class Telegram(telegramToken: String, private val handler: (meter: String) -> Double?) {
    private var tg: Bot? = null

    init {
        tg = bot {
            token = telegramToken
            dispatch {
                text {
                    val normalizedText = text.split('/', limit = 2).last().trim()
                    if (normalizedText.toIntOrNull() == null) {
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
                    } else {
                        bot.sendChatAction(ChatId.fromId(message.chat.id), ChatAction.TYPING)
                        val result = handleMeter(normalizedText)
                        bot.sendMessage(
                            ChatId.fromId(message.chat.id),
                            text = result.first,
                            replyMarkup = result.second
                        )
                    }
                }

                callbackQuery {
                    val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                    val result = handleMeter(callbackQuery.data)
                    bot.sendMessage(
                        ChatId.fromId(chatId),
                        text = result.first,
                        replyMarkup = result.second
                    )
                }

                telegramError {
                    System.err.println(error.getErrorMessage())
                    Sentry.captureMessage(error.getErrorMessage())
                }
            }
        }
    }

    private fun handleMeter(meter: String): Pair<String, InlineKeyboardMarkup?> {
        val keyboardMarkup = InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(
                    text = String.format("Зчитати %s повторно", meter),
                    callbackData = meter
                )
            )
        )
        return try {
            val result = handler(meter)
            if (result == null) {
                "На жаль, я не зміг знайти лічильник за таким номером \uD83D\uDE14" to null
            } else {
                String.format("Поточний показник: %.3f м³", result) to keyboardMarkup
            }
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            Sentry.captureException(e)
            "На жаль, сталася помилка при зчитуванні показників \uD83D\uDE14. " +
                    "Будь ласка, спробуй пізніше \uD83D\uDE4F" to keyboardMarkup
        }
    }

    fun run() {
        tg?.startPolling()
    }

    fun stop() {
        tg?.stopPolling()
    }
}

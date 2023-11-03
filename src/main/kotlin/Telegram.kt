import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult
import com.github.kotlintelegrambot.entities.inlinequeryresults.InputMessageContent
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import io.sentry.Sentry
import java.io.InputStream

class Telegram(
    telegramToken: String,
    private val handler: (meter: String) -> Double?,
    private val decodeBarcode: (stream: InputStream) -> String?
) {
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
                                    "Відправ мені фото лічильника або номер з наліпки, що " +
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

                photos {
                    val fileId = message.photo?.last()?.fileId ?: return@photos
                    val fileResp = bot.getFile(fileId).first
                    var meter: String? = null
                    if (fileResp?.isSuccessful == true) {
                        bot.sendChatAction(ChatId.fromId(message.chat.id), ChatAction.TYPING)
                        val filePath = fileResp.body()?.result?.filePath ?: return@photos
                        val input = bot.downloadFile(filePath).first?.body()?.byteStream() ?: return@photos
                        meter = decodeBarcode(input)
                    }
                    if (meter != null) {
                        val result = handleMeter(meter)
                        bot.sendMessage(
                            ChatId.fromId(message.chat.id),
                            text = result.first,
                            replyMarkup = result.second
                        )
                    } else {
                        bot.sendMessage(
                            ChatId.fromId(message.chat.id),
                            text = "На жаль, я не зміг зчитати код Data Matrix з фотографії \uD83E\uDDD0\n" +
                                    "Спробуй зробити інше фото або ввести номер вручну \uD83D\uDCDD"
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

                inlineQuery {
                    val result = try {
                        handler(inlineQuery.query)
                    } catch (_: Exception) {
                        null
                    }
                    if (result != null) {
                        bot.answerInlineQuery(
                            inlineQuery.id, InlineQueryResult.Article(
                                inlineQuery.query,
                                "Показник лічильника ${inlineQuery.query}",
                                InputMessageContent.Text(
                                    "Поточний показник лічильника ${inlineQuery.query}: *$result* м³",
                                    parseMode = ParseMode.MARKDOWN
                                ),
                                description = "Поточний показник лічильника ${inlineQuery.query}: $result м³"
                            )
                        )
                    } else {
                        bot.answerInlineQuery(inlineQuery.id)
                    }
                }

                telegramError {
                    val errorMessage = String.format("Telegram: %s", error.getErrorMessage())
                    System.err.println(errorMessage)
                    Sentry.captureMessage(errorMessage)
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
                String.format("На жаль, я не зміг знайти лічильник за номером %s \uD83D\uDE14", meter) to null
            } else {
                String.format("Поточний показник: %.3f м³", result) to keyboardMarkup
            }
        } catch (e: Error) {
            if (e.message == "MbusLock") {
                "Наразі триває автоматичне зчитування лічильників для передачі у КВК \u23F3\n" +
                        "Будь ласка, спробуй пізніше \uD83D\uDE4F" to keyboardMarkup
            } else {
                String.format("На жаль, не вдалося зчитати показники за номером %s \uD83D\uDE14\n", meter) +
                        "Будь ласка, перевір номер або спробуй пізніше \uD83D\uDE4F" to keyboardMarkup
            }
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            Sentry.captureException(e)
            "На жаль, сталася помилка при зчитуванні показників \uD83D\uDE14\n" +
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

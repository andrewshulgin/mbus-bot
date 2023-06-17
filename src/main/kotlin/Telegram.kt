import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId

class Telegram(telegramToken: String, handler: (meter: String) -> Double?) {
    private var tg: Bot? = null

    init {
        tg = bot {
            token = telegramToken
            dispatch {
                command("start") {
                    tg?.sendMessage(
                        chatId = ChatId.fromId(message.chat.id), text = "Привіт! Відправ мені номер свого лічильника"
                    )
                }
                text {
                    if (text != "/start") {
                        val result = handler(text)
                        if (result == null) {
                            bot.sendMessage(
                                ChatId.fromId(message.chat.id),
                                text = "На жаль, я не зміг знайти лічильник за таким номером"
                            )
                        } else {
                            bot.sendMessage(ChatId.fromId(message.chat.id), text = String.format("%.3f ㎥", result))
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

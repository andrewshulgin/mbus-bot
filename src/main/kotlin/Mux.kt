import java.io.File
import java.io.IOException

private val GPIO = intArrayOf(12, 11, 6, 1, 0, 3, 64, 65, 66, 21)

class Mux {

    init {
        for (i in GPIO.indices) {
            try {
                File("/sys/class/gpio/export").writeText(String.format("%d\n", GPIO[i]))
            } catch (_: IOException) {
            }
            File(String.format("/sys/class/gpio/gpio%d/direction", GPIO[i])).writeText("out\n")
        }
    }

    fun switch(channel: Int) {
        if (channel > 0) {
            for (i in GPIO.indices) {
                File(String.format("/sys/class/gpio/gpio%d/value", GPIO[i])).writeText("0\n")
            }
            File(String.format("/sys/class/gpio/gpio%d/value", GPIO[channel - 1])).writeText("1\n")
        }
    }
}

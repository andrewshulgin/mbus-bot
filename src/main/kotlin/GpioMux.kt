import java.io.File
import java.io.IOException

class GpioMux(private val channels: List<Int>) {
    fun switch(channel: Int) {
        for (i in channels.indices) {
            try {
                File("/sys/class/gpio/export").writeText(String.format("%d\n", channels[i]))
            } catch (_: IOException) {
            }
            File(String.format("/sys/class/gpio/gpio%d/direction", channels[i])).writeText("out\n")
        }
        if (channel > 0) {
            for (i in channels.indices) {
                File(String.format("/sys/class/gpio/gpio%d/value", channels[i])).writeText("0\n")
            }
            File(String.format("/sys/class/gpio/gpio%d/value", channels[channel - 1])).writeText("1\n")
        }
    }

    fun release() {
        for (i in channels.indices) {
            try {
                File("/sys/class/gpio/unexport").writeText(String.format("%d\n", channels[i]))
            } catch (_: IOException) {
            }
        }
    }
}

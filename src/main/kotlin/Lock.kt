import java.io.File

class Lock(private val lockFilePath: String) {
    fun isLocked(): Boolean {
        return File(lockFilePath).exists()
    }
}

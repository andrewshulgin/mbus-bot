import java.sql.Connection
import java.sql.DriverManager

class Database(name: String, username: String, password: String) {
    private var connection: Connection? = null

    init {
        connection = DriverManager.getConnection(
            String.format("jdbc:mysql://localhost:3306/%s", name),
            username, password
        )
        connection?.isValid(1000)
    }

    fun getMeter(meter: String): Pair<Int, Int>? {
        if (connection == null) {
            return null
        }
        val query =
            connection!!.prepareStatement("SELECT mbus_line, primary_addr FROM network WHERE secondary_addr = ?")
        query.setString(1, meter)
        val result = query.executeQuery()
        if (result.next()) {
            return result.getInt("mbus_line") to result.getInt("primary_addr")
        }
        return null
    }
}

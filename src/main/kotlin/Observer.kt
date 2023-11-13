interface Observer {
    fun update(message: String)
}


class Logger : Observer {
    override fun update(message: String) {
        // Здесь ваш код для логгирования, например:
        println("Log: $message")
    }
}
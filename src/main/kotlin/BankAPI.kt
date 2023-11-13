
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

class Bank {
    val currencies = ConcurrentHashMap<String, Double>()
    val clients = ConcurrentHashMap<Long, Client>()
    val cashiers = CopyOnWriteArrayList(
        listOf(
            Cashier(1, this),
            Cashier(2, this),
            Cashier(3, this)
        )
    )

    val exchangeRates = ConcurrentHashMap<String, Double>()
    val transactionQueue = LinkedBlockingQueue<Transaction>()
    private val observers = CopyOnWriteArrayList<Observer>()

    @Volatile
    var idCounter : Long = 0

    init {
        with(currencies) {
            put("USD", 1.0)
            put("RUB", 0.01)
            put("EURO", 1.1)
            put("JPY", 0.005)
        }

        val executor = ScheduledThreadPoolExecutor(1)
        executor.scheduleAtFixedRate({
            // Здесь обновляйте курсы валют. Например:
            //exchangeRates["USD"] = getRandomExchangeRate()
        }, 0, 1, TimeUnit.HOURS)
    }



    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun notifyObservers(message: String) {
        observers.forEach {
            it.update(message)
        }
    }
    fun getNextAvailableId() : Long {
        return ++idCounter;
    }

    fun addClient(client : Client){
        if(!currencies.containsKey(client.currency)) throw Exception("There is no such currency with name ${client.currency}")
        if(clients.containsKey(client.id)) throw Exception("Client with id ${client.id} already exists")
        clients[client.id] = client
    }

    fun startWorking() {
        cashiers.forEach(Cashier::start)
    }

}


class Client(val bank : Bank, @Volatile var balance: Double, val id : Long = bank.getNextAvailableId(), @Volatile var currency: String = "USD"){
    //in this case usage of synchronized is not necessary because all fields are Volatile or val, making them thread-safe
    //however, there is no problem about using sycnhronized with lock object as thread-blocker
    val lock = ReentrantLock()
}


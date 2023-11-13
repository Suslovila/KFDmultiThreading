### Домашнее задание: Расширенное многопоточное программирование на Kotlin

#### Описание задания:

Создайте многопоточное приложение на Kotlin, которое не только симулирует работу банка с кассами, но и добавляет
дополнительные слои сложности, такие как валютный обмен и переводы между клиентами.

#### Компоненты:

1. **Клиент**: объект с уникальным ID, суммой денег, и валютой.

2. **Касса**: обработчик транзакций, каждая касса работает в отдельном потоке.

3. **Банк**: хранит информацию о всех клиентах и кассах, а также курсах валют.

#### Требования:

1. Добавить функциональность для обмена
   валют: `exchangeCurrency(clientId: Int, fromCurrency: String, toCurrency: String, amount: Double)`.

2. Реализовать переводы между клиентами: `transferFunds(senderId: Int, receiverId: Int, amount: Double)`.

3. Использовать `ScheduledThreadPoolExecutor` для автоматического обновления курсов валют.

4. Реализовать очередь транзакций, которая обрабатывается асинхронно.

5. Применить паттерн Observer для логгирования.

### Дополнительные детали к домашнему заданию

#### 1. Использование `ScheduledThreadPoolExecutor` для автоматического обновления курсов валют

Цель этого требования — симулировать реальный мир, где курсы валют постоянно меняются. Создайте задачу, которая
автоматически обновляет курсы валют в вашем `Bank` объекте.

 ```kotlin
 import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Bank {
    val exchangeRates = ConcurrentHashMap<String, Double>()

    init {
        val executor = ScheduledThreadPoolExecutor(1)
        executor.scheduleAtFixedRate({
            // Здесь обновляйте курсы валют. Например:
            exchangeRates["USD"] = getRandomExchangeRate()
        }, 0, 1, TimeUnit.HOURS)
    }
}
 ```

#### 2. Реализация очереди транзакций, которая обрабатывается асинхронно

Идея здесь в том, чтобы операции (переводы, пополнения, снятия и т.д.) не обрабатывались мгновенно, а добавлялись в
очередь. Эта очередь затем асинхронно обрабатывается отдельными потоками (кассами).

 ```kotlin
 import java.util.concurrent.LinkedBlockingQueue

class Bank {
    val transactionQueue = LinkedBlockingQueue<Transaction>()

    init {
        // Запускаем потоки-кассы для обработки очереди
    }
}

class Cashier : Thread() {
    override fun run() {
        while (true) {
            val transaction = Bank.transactionQueue.take()
            // Обрабатываем транзакцию
        }
    }
}
 ```

#### 3. Применение паттерна Observer для логгирования

Паттерн Observer позволяет оповестить зарегистрированные объекты (наблюдатели) о событиях, происходящих в системе. В
данной задаче, его можно использовать для логгирования всех операций и событий, таких как переводы, пополнения баланса и
изменения курса валют.

Сначала создайте интерфейс Observer:

```kotlin
 interface Observer {
    fun update(message: String)
}
 ```

Затем добавьте его реализацию, которая будет записывать логи:

 ```kotlin
 class Logger : Observer {
    override fun update(message: String) {
        // Здесь ваш код для логгирования, например:
        println("Log: $message")
    }
}
 ```

В классе `Bank`, добавьте методы для регистрации и оповещения наблюдателей:

 ```kotlin
 class Bank {
    private val observers = mutableListOf<Observer>()

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun notifyObservers(message: String) {
        observers.forEach {
            it.update(message)
        }
    }
}
 ```

Теперь, каждый раз при изменении курса валюты или выполнении транзакции, вызывайте `notifyObservers` с соответствующим
сообщением:

 ```kotlin
 fun deposit(clientId: Int, amount: Double) {
    // ... ваш код
    notifyObservers("Deposit successful for client $clientId, amount: $amount")
}
 ```

#### Примерная структура кода:

 ```kotlin
 class Client(val id: Int, var balance: Double, var currency: String)

class Cashier(val id: Int, val bank: Bank) : Thread() {
    fun deposit(clientId: Int, amount: Double) { /* ... */
    }
    fun withdraw(clientId: Int, amount: Double) { /* ... */
    }
    fun exchangeCurrency(clientId: Int, fromCurrency: String, toCurrency: String, amount: Double) { /* ... */
    }
    fun transferFunds(senderId: Int, receiverId: Int, amount: Double) { /* ... */
    }
}

class Bank {
    val clients = ConcurrentHashMap<Int, Client>()
    val cashiers = ArrayList<Cashier>()
    val exchangeRates = ConcurrentHashMap<String, Double>()

    // Другие функции и механизмы
}
 ```

#### Концепции

В данном домашнем задании, многопоточность будет активно использоваться в нескольких местах:

1. Кассы (Cashiers):
   Каждая касса будет работать в своём потоке, обрабатывая транзакции из общей очереди.
   Потоки нужно будет синхронизировать таким образом, чтобы не возникло проблем с конкурентным доступом к данным
   клиентов.

2. Автоматическое обновление курсов валют:
   Используя ScheduledThreadPoolExecutor, вы можете создать отдельный поток, который будет регулярно обновлять курсы
   валют.
   Здесь ключевой момент в том, чтобы корректно обновлять информацию, доступ к которой могут иметь другие потоки.


3. Асинхронная очередь транзакций:
   Элементы этой очереди будут обрабатываться асинхронно кассами.
   Важно удостовериться, что операции с очередью потокобезопасны.


4. Логгирование с использованием паттерна Observer:
   В этом случае, многопоточность может косвенно затрагивать логгирование, так как обновления могут приходить из разных
   потоков.
   Нужно обеспечить, чтобы метод update в Observer был потокобезопасным.

Все эти многопоточные операции требуют аккуратной синхронизации и возможно использование примитивов синхронизации для
обеспечения корректного доступа к ресурсам

#### Что ожидается:

1. Код должен быть написан с учетом принципов ООП и SOLID.


2. Аккуратное логгирование всех операций и ошибок.


3. Предоставить документацию и инструкцию по запуску.


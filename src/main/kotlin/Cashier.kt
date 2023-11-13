
abstract class Transaction(val clientId : Long, val amount: Double)

class ExchangeCurrencyTransaction(clientId: Long, val newCurrency : String, val currencyFromFactor: Double, val currencyToFactor : Double?, amount: Double) : Transaction(clientId, amount)
class DepositTransaction(clientId: Long, amount: Double) : Transaction(clientId, amount)
class WithdrawTransaction(clientId: Long, amount: Double) : Transaction(clientId, amount)
class TransferTransaction(senderId: Long, val receiverId: Long, val currencyFromFactor: Double, val currencyToFactor : Double, amount: Double) : Transaction(senderId, amount)

class Cashier(private val cashierId: Int, val bank: Bank) : Thread() {
    private fun sendLog(
        transaction: Transaction,
        sender: Client,
        receiver: Client? = null,
        toCurrency: String? = null
    ) {
        bank.notifyObservers(
            when (transaction) {
                is WithdrawTransaction ->
                    "Operation: withdraw, Cashier: $id, Client: ${sender.id}, taken: ${transaction.amount}"

                is DepositTransaction ->
                    "Operation: deposit, Cashier: $id, Client: ${sender.id}, put: ${transaction.amount}"

                is TransferTransaction -> {
                    receiver?.let {
                        "Operation: transfer, Cashier: $id, sender: ${sender.id}, receiver: ${receiver.id}, transfer amount: ${transaction.amount}"
                    } ?: "$cashierId ${sender.id}: broken log info"
                }

                is ExchangeCurrencyTransaction -> {
                    toCurrency?.let {
                        "Operation: exchange, Cashier: $id, client: ${sender.id}, exchange amount: ${transaction.amount}"
                    } ?: "$cashierId ${sender.id}: broken log info"
                }

                else -> {
                    "log error: unknown transaction"
                }
            }
        )
    }

    override fun run() {
        while (true) {
            val transaction = bank.transactionQueue.take()
            with(transaction) {
                val client = bank.clients[clientId] ?: error("Unregistered Client")
                when (this) {
                    is ExchangeCurrencyTransaction -> {
                        if (!bank.currencies.containsKey(newCurrency)) {
                            bank.notifyObservers("Wrong currency exchange name")
                            return
                        }
                        if (amount < 0 || amount > client.balance) {
                            bank.notifyObservers("Wrong exchange currency amount")
                            return
                        }

                        synchronized(client.lock) {
                            sleep(3000)
                            client.balance = client.balance * currencyFromFactor / currencyToFactor!!
                            client.currency = newCurrency
                            sendLog(transaction, client, null, newCurrency);

                        }
                    }

                    is DepositTransaction -> {
                        if (amount < 0) {
                            bank.notifyObservers("Wrong deposit amount")
                            return
                        }

                        synchronized(client.lock) {
                            sleep(3000)
                            client.balance += amount
                            sendLog(transaction, client);

                        }
                    }

                    is WithdrawTransaction -> {
                        if (amount < 0 || amount > client.balance) {
                            bank.notifyObservers("Wrong withdraw amount")
                            return
                        }

                        synchronized(client.lock) {
                            sleep(3000)
                            client.balance -= amount
                            sendLog(transaction, client);
                        }
                    }

                    is TransferTransaction -> {
                        /* here we are about to catch deadlock when doing
                    receiver.lock.lock()
                    sender.lock.lock()
                    to prevent it, we can simply lock depending on Client's id
                    however, as it was said, it isn't necessary since all fields are volatile
                     */
                        val receiver = bank.clients[receiverId]!!
                        if (amount < 0 || amount > client.balance) {
                            bank.notifyObservers("Incorrect transfer amount")
                            return
                        }
                        receiver.balance += amount * currencyFromFactor / currencyToFactor
                        client.balance -= amount
                        sendLog(transaction, client, receiver);

                    }
                }
            }
        }
    }

    fun deposit(clientId: Long, amount: Double) {
        bank.transactionQueue.add(DepositTransaction(clientId, amount))
    }


    fun withdraw(clientId: Long, amount: Double) {
        bank.transactionQueue.add(WithdrawTransaction(clientId, amount))
    }


    fun exchangeCurrency(clientId: Long, toCurrency: String, amount: Double) {
        with(bank) {
            val client = bank.clients[clientId] ?: error("Unregistered Client")
            val toCurrencyFactor = currencies[toCurrency] ?: error("Wrong currency")
            transactionQueue.add(
                ExchangeCurrencyTransaction(
                    clientId,
                    toCurrency,
                    currencies[client.currency]!!,
                    toCurrencyFactor,
                    amount
                )
            )
        }
    }

    fun transferFunds(senderId: Long, receiverId: Long, amount: Double) {
        val sender = bank.clients[senderId] ?: error("Unregistered Client")
        val receiver = bank.clients[receiverId] ?: error("Unregistered Client")

        bank.transactionQueue.add(
            TransferTransaction(
                senderId,
                receiverId,
                bank.currencies[sender.currency]!!,
                bank.currencies[receiver.currency]!!,
                amount
            )
        )

    }
}
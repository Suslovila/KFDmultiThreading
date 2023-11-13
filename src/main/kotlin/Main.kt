
fun main(args: Array<String>) {
    val bank = Bank()
    bank.addObserver(Logger())
    val clients = mutableListOf(Client(bank, 5500.5), Client(bank, 20.0), Client(bank, 10.0)).forEach{
        bank.addClient(it)
    }


    bank.cashiers[0].exchangeCurrency(1, "rth", 5500.0)
    bank.startWorking()
}
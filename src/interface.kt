import kotlin.concurrent.thread

fun main() {
    val humans = arrayOf(
        Human("Petya", "Ivanov", "Petrovich", 444, 20, 1.5),
        Human("Kirill", "Krachmalniy", "Vladimirovich", 344, 21, 1.2),
        Human("Mihail", "Sinicha", "Alecksandrovich", 443, 23, 2.0)
    )

    val driver = Driver("Nikita", "Krivolapov", "Alekseevich", 433, 19, 3.0)

    val all = humans + driver

    println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")

    val threads = all.map { h ->
        thread {
            for (i in 1..5) {
                h.move()
            }
        }
    }

    for (thread in threads) {
        thread.join()
    }

    println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
    println("\nФинальные позиции:")
    for (person in all) {
        println("${person.name}: (${"%.1f".format(person.x)}, ${"%.1f".format(person.y)})")
    }
}

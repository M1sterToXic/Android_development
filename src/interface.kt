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
            repeat(5) {
                h.move()
            }
        }
    }

    threads.forEach { it.join() }

    println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
    println("\nФинальные позиции:")
    all.forEach {
        println("${it.name}: (${"%.1f".format(it.x)}, ${"%.1f".format(it.y)})")
    }
}

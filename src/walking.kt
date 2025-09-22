import kotlin.random.Random
import kotlin.concurrent.thread

open class Human {
    var name: String = ""
    var surname: String = ""
    var second_name: String = ""
    var age: Int = 0
    var currentSpeed: Double = 0.0

    var group_number: Int = -1
    var x = 0.0
    var y = 0.0

    constructor(_name: String, _surname: String, _second: String, _gn: Int, _age: Int, _speed: Double) {
        name = _name
        surname = _surname
        second_name = _second
        group_number = _gn
        age = _age
        currentSpeed = _speed
        println("Создан: $name")
    }

    open fun move() {
        val direction = Random.nextDouble(0.0, 2 * Math.PI)
        val randomSpeed = currentSpeed * Random.nextDouble(0.5, 1.5)
        x += randomSpeed * Math.cos(direction)
        y += randomSpeed * Math.sin(direction)
        println("$name переместился в (${"%.1f".format(x)}, ${"%.1f".format(y)})")
    }
}

class Driver : Human {
    var car: String = "car"

    constructor(_name: String, _surname: String, _second: String, _gn: Int, _age: Int, _speed: Double) :
            super(_name, _surname, _second, _gn, _age, _speed)

    override fun move() {
        val direction = 0.0
        x += currentSpeed * Math.cos(direction)
        y += currentSpeed * Math.sin(direction)
        println("$name (Driver) переместился в (${"%.1f".format(x)}, ${"%.1f".format(y)})")
    }
}

fun main() {
    val humans = arrayOf(
        Human("Petya", "Ivanov", "Petrovich", 444, 20, 1.5),
        Human("Kirill", "Krachmalniy", "Vladimirovich", 344, 21, 1.2),
        Human("Mihail", "Sinicha", "Alecksandrovich", 443, 23, 2.0)
    )

    val driver = Driver("Nikita", "Krivolapov", "Alekseevich", 433, 19, 3.0)

    val all = humans + driver

    val threads = all.map { h ->
        thread {
            repeat(5) {
                h.move()
            }
        }
    }

    threads.forEach { it.join() }

    println("\nФинальные позиции:")
    all.forEach {
        println("${it.name}: (${"%.1f".format(it.x)}, ${"%.1f".format(it.y)})")
    }
}

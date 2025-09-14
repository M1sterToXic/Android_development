import kotlin.random.Random

class Human
{
    var name: String = ""
    var surname: String = ""
    var second_name: String = ""
    var age: Int = 0
    var currentSpeed: Double = 0.0

    var group_number: Int = -1
    var x = 0.0
    var y = 0.0

    constructor(_name: String, _surname: String, _second: String, _gn: Int, _age: Int, _speed: Double){
        name = _name
        surname = _surname
        second_name = _second
        group_number = _gn
        age = _age
        currentSpeed = _speed
        println("Создан: $name")
    }

    fun move()
    {
        val direction = Random.nextDouble(0.0, 2 * Math.PI)
        val randomSpeed = currentSpeed * Random.nextDouble(0.5, 1.5)

        x += randomSpeed * Math.cos(direction)
        y += randomSpeed * Math.sin(direction)

        println("$name переместился в (${"%.1f".format(x)}, ${"%.1f".format(y)})")
    }

    fun moveTo(_toX: Int, _toY: Int)
    {
        x = _toX.toDouble()
        y = _toY.toDouble()
        println("$name перемещен В: $x,$y")
    }
}

fun main(){
    val people = arrayOf(
        Human("Petya","Ivanov","Petrovich",444, 20, 1.5),
        Human("Zui-ay","Nguen","Kueevich",434, 22, 1.8),
        Human("Kirril","Krachmalniy","Vladimirovich",344, 21, 1.2),
        Human("Mihail","Sinicha","Alecksandrovich",443, 23, 2.0),
        Human("Nikita","Krivolapov","Alekseevich",433, 19, 1.6)
    )

    val simulationTime = 5
    val timeSteps = 10

    println("Симуляция началась на $simulationTime секунд")
    println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")

    for (step in 1..timeSteps) {
        println("\nШаг $step:")
        println("▼-▼-▼-▼-▼-▼-▼-▼-▼-▼")
        people.forEach { it.move() }
        println("▼-▼-▼-▼-▼-▼-▼-▼-▼-▼")
    }

    println("\n=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
    println("Финальные позиции:")
    people.forEach {
        println("${it.name}: (${"%.1f".format(it.x)}, ${"%.1f".format(it.y)})")
    }
}
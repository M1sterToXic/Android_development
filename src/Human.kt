import kotlin.random.Random

open class Human : Movable {
    override var x: Double = 0.0
    override var y: Double = 0.0
    override var currentSpeed: Double = 0.0

    var name: String = ""
    var surname: String = ""
    var second_name: String = ""
    var age: Int = 0
    var group_number: Int = -1

    constructor(_name: String, _surname: String, _second: String, _gn: Int, _age: Int, _speed: Double) {
        name = _name
        surname = _surname
        second_name = _second
        group_number = _gn
        age = _age
        currentSpeed = _speed
        println("Создан: $name")
    }

    override fun move() {
        val direction = Random.nextDouble(0.0, 2 * Math.PI)
        val randomSpeed = currentSpeed * Random.nextDouble(0.5, 1.5)
        x += randomSpeed * Math.cos(direction)
        y += randomSpeed * Math.sin(direction)
        println("$name переместился в (${"%.1f".format(x)}, ${"%.1f".format(y)})")
    }
}
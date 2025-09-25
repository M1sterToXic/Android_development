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
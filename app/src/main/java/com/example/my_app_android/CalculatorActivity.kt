package com.example.my_app_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class CalculatorActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private var currentInput = StringBuilder()
    private var currentOperator = ""
    private var firstNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        tvDisplay = findViewById(R.id.tvDisplay)

        setupNumberButtons()
        setupOperationButtons()
        setupSpecialButtons()
    }

    private fun setupNumberButtons() {
        findViewById<Button>(R.id.btn0).setOnClickListener { onNumberButtonClick("0") }
        findViewById<Button>(R.id.btn1).setOnClickListener { onNumberButtonClick("1") }
        findViewById<Button>(R.id.btn2).setOnClickListener { onNumberButtonClick("2") }
        findViewById<Button>(R.id.btn3).setOnClickListener { onNumberButtonClick("3") }
        findViewById<Button>(R.id.btn4).setOnClickListener { onNumberButtonClick("4") }
        findViewById<Button>(R.id.btn5).setOnClickListener { onNumberButtonClick("5") }
        findViewById<Button>(R.id.btn6).setOnClickListener { onNumberButtonClick("6") }
        findViewById<Button>(R.id.btn7).setOnClickListener { onNumberButtonClick("7") }
        findViewById<Button>(R.id.btn8).setOnClickListener { onNumberButtonClick("8") }
        findViewById<Button>(R.id.btn9).setOnClickListener { onNumberButtonClick("9") }
    }

    private fun setupOperationButtons() {
        findViewById<Button>(R.id.btnAdd).setOnClickListener { onOperatorClick("+") }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { onOperatorClick("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperatorClick("*") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperatorClick("/") }
    }

    private fun setupSpecialButtons() {
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            clearCalculator()
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            calculateResult()
        }
    }

    private fun onNumberButtonClick(number: String) {
        currentInput.append(number)
        updateDisplay()
    }

    private fun onOperatorClick(operator: String) {
        if (currentInput.isNotEmpty()) {
            if (firstNumber.isNotEmpty() && currentOperator.isNotEmpty()) {
                calculateResult()
            }

            firstNumber = currentInput.toString()
            currentOperator = operator
            currentInput.clear()
            tvDisplay.text = "$firstNumber $currentOperator"
        }
    }

    private fun calculateResult() {
        if (firstNumber.isNotEmpty() && currentOperator.isNotEmpty() && currentInput.isNotEmpty()) {
            try {
                val num1 = firstNumber.toDouble()
                val num2 = currentInput.toString().toDouble()
                var result = 0.0

                when (currentOperator) {
                    "+" -> result = num1 + num2
                    "-" -> result = num1 - num2
                    "*" -> result = num1 * num2
                    "/" -> {
                        if (num2 != 0.0) {
                            result = num1 / num2
                        } else {
                            tvDisplay.text = "Ошибка"
                            resetCalculator()
                            return
                        }
                    }
                }

                currentInput.clear()
                if (result % 1 == 0.0) {
                    currentInput.append(result.toInt().toString())
                } else {
                    currentInput.append(result.toString())
                }
                updateDisplay()

                currentOperator = ""
                firstNumber = ""

            } catch (e: Exception) {
                tvDisplay.text = "Ошибка"
                resetCalculator()
            }
        }
    }

    private fun updateDisplay() {
        if (currentInput.isEmpty()) {
            tvDisplay.text = "0"
        } else {
            tvDisplay.text = currentInput.toString()
        }
    }

    private fun clearCalculator() {
        resetCalculator()
        tvDisplay.text = "0"
    }

    private fun resetCalculator() {
        currentInput.clear()
        currentOperator = ""
        firstNumber = ""
    }
}
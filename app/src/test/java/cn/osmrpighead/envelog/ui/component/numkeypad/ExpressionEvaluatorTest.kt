package cn.osmrpighead.envelog.ui.component.numkeypad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ExpressionEvaluatorTest {
    @Test
    fun precedenceAndParentheses() {
        assertValue("14", "2+3×4")
        assertValue("20", "(2+3)×4")
        assertValue("8", "10-6÷3")
        assertValue("6", "1+2+3")
        assertValue("1", "8÷4÷2")
        assertValue("-5", "1-(2×3)")
    }

    @Test
    fun emptyParenthesesUseOperatorPriority() {
        assertValue("4", "1+()×3")
        assertValue("4", "1×()+3")
        assertValue("-2", "1-()+3")
        assertValue("-2", "1+()-3")
        assertValue("0.33", "1÷()×3")
        assertValue("4", "1+()3")
        assertValue("12", "1()2")
    }

    @Test
    fun incompleteOperatorsAreIgnored() {
        assertValue("1", "1+")
        assertValue("1", "1-")
        assertValue("1", "1×")
        assertValue("1", "1÷")
        assertValue("3", "×3")
        assertValue("3", "÷3")
        assertValue("15", "5××3")
        assertValue("0", "()")
        assertValue("0", "")
    }

    @Test
    fun unaryOperators() {
        assertValue("-2", "1+-3")
        assertValue("4", "1--3")
        assertValue("-3", "1×-3")
        assertValue("-3", "-5+2")
        assertValue("-10", "(-5)×2")
        assertValue("-1", "1+(-2)")
    }

    @Test
    fun divisionByZeroIsReported() {
        assertError(NumKeypadError.DivisionByZero, "1÷0")
        assertError(NumKeypadError.DivisionByZero, "1÷(2-2)")
    }

    @Test
    fun roundingUsesHalfEven() {
        assertValue("0.33", "1÷3")
        assertValue("0.67", "2÷3")
        assertValue("1", "1.005")
        assertValue("1.02", "1.015")
        assertValue("2", "1.5", scale = 0)
    }

    @Test
    fun formattingRemovesTrailingZeros() {
        assertEquals("1", ExpressionEvaluator.format(BigDecimal("1.00"), 2))
        assertEquals("0", ExpressionEvaluator.format(BigDecimal("0.00"), 2))
        assertEquals("1000", ExpressionEvaluator.format(BigDecimal("1000.00"), 2))
        assertEquals("0.3", ExpressionEvaluator.format(BigDecimal("0.30"), 2))
        assertEquals("-2.5", ExpressionEvaluator.format(BigDecimal("-2.50"), 2))
    }

    @Test
    fun malformedExpressions() {
        assertError(NumKeypadError.InvalidExpression, "(")
        assertError(NumKeypadError.InvalidExpression, "1+2)")
        assertError(NumKeypadError.InvalidExpression, "1.2.3")
        assertError(NumKeypadError.InvalidExpression, "1..2")
        assertError(NumKeypadError.InvalidExpression, "1a2")
    }

    @Test
    fun wellFormedIgnoresDivisionByZero() {
        assertTrue(ExpressionEvaluator.isWellFormed("1+2"))
        assertTrue(ExpressionEvaluator.isWellFormed("1÷0"))
        assertTrue(ExpressionEvaluator.isWellFormed("1+"))
        assertFalse(ExpressionEvaluator.isWellFormed("1.2.3"))
        assertFalse(ExpressionEvaluator.isWellFormed("("))
        assertFalse(ExpressionEvaluator.isWellFormed("1+2)"))
    }

    private fun assertValue(expected: String, expression: String, scale: Int = 2) {
        val outcome = ExpressionEvaluator.evaluate(expression, scale)
        assertTrue("Expected a value for '$expression', got $outcome", outcome is EvaluationOutcome.Value)
        val expectedValue = BigDecimal(expected).setScale(scale, RoundingMode.HALF_EVEN)
        assertEquals(expectedValue, (outcome as EvaluationOutcome.Value).value)
    }

    private fun assertError(expected: NumKeypadError, expression: String, scale: Int = 2) {
        val outcome = ExpressionEvaluator.evaluate(expression, scale)
        assertTrue("Expected an error for '$expression', got $outcome", outcome is EvaluationOutcome.Error)
        assertEquals(expected, (outcome as EvaluationOutcome.Error).error)
    }
}

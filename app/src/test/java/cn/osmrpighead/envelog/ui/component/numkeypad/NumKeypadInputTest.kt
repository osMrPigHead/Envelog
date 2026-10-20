package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class NumKeypadInputTest {
    private val expressionState = NumKeypadViewModel(mode = NumKeypadMode.Expression)

    @Test
    fun digitInputNormalizesLeadingZeros() {
        assertValue("5", 1, update(value("0", 1), NumKeypadAction.Digit('5')).value)
        assertValue("0", 1, update(value("0", 1), NumKeypadAction.Digit('0')).value)
        assertValue("1.23", 4, update(value("1.2", 3), NumKeypadAction.Digit('3')).value)
        assertValue("0.52", 4, update(value("00.5", 4), NumKeypadAction.Digit('2')).value)
    }

    @Test
    fun decimalInputMovesExistingPointToCursor() {
        assertValue("0.", 2, update(value("0", 1), NumKeypadAction.Decimal).value)
        assertValue("123.", 4, update(value("1.23", 4), NumKeypadAction.Decimal).value)
        assertValue("1.23", 2, update(value("1.23", 1), NumKeypadAction.Decimal).value)
        assertValue("12.", 3, update(value("12", 2), NumKeypadAction.Decimal).value)
        assertValue("1+0.2", 4, update(value("1+2", 2), NumKeypadAction.Decimal).value)
    }

    @Test
    fun decimalInputReplacesSelection() {
        val selected = TextFieldValue("1.23", TextRange(2, 4))
        assertValue("1.", 2, update(selected, NumKeypadAction.Decimal, expressionState).value)
    }

    @Test
    fun operatorReplacesAdjacentOperator() {
        assertValue("1×", 2, update(value("1+", 2), NumKeypadAction.Operator('×')).value)
    }

    @Test
    fun calculatorOperatorEvaluatesBeforeInserting() {
        assertValue("3+", 2, update(value("1+2", 3), NumKeypadAction.Operator('+')).value)
    }

    @Test
    fun operatorIsDisabledInCalculatorMiddle() {
        assertFalse(isEnabled(value("1+2", 1), NumKeypadViewModel(), NumKeypadAction.Operator('+'), 2))
        assertTrue(isEnabled(value("1+2", 3), NumKeypadViewModel(), NumKeypadAction.Operator('+'), 2))
    }

    @Test
    fun multiplyAndDivideRequireLeftOperand() {
        assertFalse(isEnabled(value("+", 1), NumKeypadViewModel(), NumKeypadAction.Operator('×'), 2))
        assertTrue(isEnabled(value("1+", 2), NumKeypadViewModel(), NumKeypadAction.Operator('×'), 2))
        assertFalse(
            isEnabled(
                value("(1", 1),
                expressionState,
                NumKeypadAction.Operator('×'),
                2,
            ),
        )
        assertTrue(
            isEnabled(
                value("(1)", 3),
                expressionState,
                NumKeypadAction.Operator('×'),
                2,
            ),
        )
    }

    @Test
    fun expressionModeInsertsOperatorAtCursor() {
        assertValue(
            "1×+2",
            2,
            update(value("1+2", 1), NumKeypadAction.Operator('×'), expressionState).value,
        )
    }

    @Test
    fun parenthesisKeyStateFollowsCursorContext() {
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Insert, enabled = true),
            parenthesisKeyState(value("1+2", 0), expressionState),
        )
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Extend, enabled = true),
            parenthesisKeyState(value("(1+2)×3", 5), expressionState),
        )
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Extend, enabled = false),
            parenthesisKeyState(value("(1+2)", 5), expressionState),
        )
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Extend, enabled = false),
            parenthesisKeyState(value("(1+2)×(3)", 5), expressionState),
        )
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Close, enabled = true),
            parenthesisKeyState(value("(1+2)", 3), expressionState),
        )
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Close, enabled = true),
            parenthesisKeyState(value("(1+)", 3), expressionState),
        )
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Close, enabled = false),
            parenthesisKeyState(value("(12)", 2), expressionState),
        )
        assertEquals(
            ParenthesisKeyState(ParenthesisKeyKind.Close, enabled = false),
            parenthesisKeyState(value("((1))", 3), expressionState),
        )
        assertNull(parenthesisKeyState(value("(12)", 2), NumKeypadViewModel()))
    }

    @Test
    fun parenthesisKeyActions() {
        assertValue(
            "1+2()",
            4,
            update(value("1+2", 3), NumKeypadAction.Parenthesis, expressionState).value,
        )
        assertValue(
            "(1)+2",
            4,
            update(value("(1+2)", 3), NumKeypadAction.Parenthesis, expressionState).value,
        )
        assertValue(
            "(1+)",
            4,
            update(value("(1+)", 3), NumKeypadAction.Parenthesis, expressionState).value,
        )
        assertValue(
            "(1+2×3)",
            7,
            update(value("(1+2)×3", 5), NumKeypadAction.Parenthesis, expressionState).value,
        )
        assertTrue(
            reduce(
                value("(12)", 2),
                expressionState,
                NumKeypadAction.Parenthesis,
                2,
            ) is NumKeypadTransition.Ignored,
        )
    }

    @Test
    fun backspaceRules() {
        assertValue("1", 1, update(value("12", 2), NumKeypadAction.Backspace).value)
        assertValue("12", 2, update(value("(12)", 4), NumKeypadAction.Backspace).value)
        assertValue("12", 1, update(value("1+2", 2), NumKeypadAction.Backspace).value)
        assertValue("1.23", 3, update(value("1.2+.3", 4), NumKeypadAction.Backspace).value)
        assertValue("1-3", 2, update(value("1+2-3", 3), NumKeypadAction.Backspace).value)
    }

    @Test
    fun clearRules() {
        assertValue("0", 1, update(value("123", 3), NumKeypadAction.Clear).value)

        val errorState = NumKeypadViewModel(error = NumKeypadError.DivisionByZero)
        val restored = update(value("1÷0", 3), NumKeypadAction.Clear, errorState)
        assertValue("1÷0", 3, restored.value)
        assertNull(restored.state.error)
    }

    @Test
    fun switchModeRules() {
        val toCalculator = update(
            value("1+2", 3),
            NumKeypadAction.SwitchMode,
            NumKeypadViewModel(mode = NumKeypadMode.Expression),
        )
        assertValue("3", 1, toCalculator.value)
        assertEquals(NumKeypadMode.Calculator, toCalculator.state.mode)

        val toExpression = update(
            value("1+2", 3),
            NumKeypadAction.SwitchMode,
            NumKeypadViewModel(mode = NumKeypadMode.Calculator),
        )
        assertEquals("1+2", toExpression.value.text)
        assertEquals(NumKeypadMode.Expression, toExpression.state.mode)

        val restored = update(
            value("1÷0", 3),
            NumKeypadAction.SwitchMode,
            NumKeypadViewModel(mode = NumKeypadMode.Calculator, error = NumKeypadError.DivisionByZero),
        )
        assertValue("1÷0", 3, restored.value)
        assertEquals(NumKeypadMode.Expression, restored.state.mode)
        assertNull(restored.state.error)
    }

    @Test
    fun expressionModeSwitchIsDisabledWhenEvaluationFails() {
        assertFalse(
            isEnabled(
                value("1÷0", 3),
                expressionState,
                NumKeypadAction.SwitchMode,
                2,
            ),
        )
        assertTrue(
            isEnabled(
                value("1+2", 3),
                expressionState,
                NumKeypadAction.SwitchMode,
                2,
            ),
        )
    }

    @Test
    fun errorStateDisablesInputButNotClearOrSwitch() {
        val errorState = NumKeypadViewModel(error = NumKeypadError.DivisionByZero)
        assertFalse(isEnabled(value("1", 1), errorState, NumKeypadAction.Digit('2'), 2))
        assertFalse(isEnabled(value("1", 1), errorState, NumKeypadAction.Backspace, 2))
        assertTrue(isEnabled(value("1", 1), errorState, NumKeypadAction.Clear, 2))
        assertTrue(isEnabled(value("1", 1), errorState, NumKeypadAction.SwitchMode, 2))
    }

    @Test
    fun evaluationMappingIncludesLockFlag() {
        assertEquals(
            NumKeypadEvaluation.Failure(NumKeypadError.DivisionByZero, locksInput = true),
            NumKeypadViewModel(error = NumKeypadError.DivisionByZero)
                .toEvaluation(EvaluationOutcome.Value(BigDecimal.ONE)),
        )
        assertEquals(
            NumKeypadEvaluation.Success(BigDecimal("2.00")),
            NumKeypadViewModel().toEvaluation(EvaluationOutcome.Value(BigDecimal("2.00"))),
        )
        assertEquals(
            NumKeypadEvaluation.Failure(NumKeypadError.InvalidExpression, locksInput = false),
            NumKeypadViewModel().toEvaluation(EvaluationOutcome.Error(NumKeypadError.InvalidExpression)),
        )
    }

    private fun value(text: String, cursor: Int = text.length): TextFieldValue =
        TextFieldValue(text, TextRange(cursor))

    private fun update(
        value: TextFieldValue,
        action: NumKeypadAction,
        state: NumKeypadViewModel = NumKeypadViewModel(),
        scale: Int = 2,
    ): NumKeypadTransition.Update {
        val transition = reduce(value, state, action, scale)
        assertTrue("Expected an update for $action, got $transition", transition is NumKeypadTransition.Update)
        return transition as NumKeypadTransition.Update
    }

    private fun assertValue(expectedText: String, expectedCursor: Int, actual: TextFieldValue) {
        assertEquals(expectedText, actual.text)
        assertEquals(expectedCursor, actual.selection.start)
        assertEquals(expectedCursor, actual.selection.end)
    }
}

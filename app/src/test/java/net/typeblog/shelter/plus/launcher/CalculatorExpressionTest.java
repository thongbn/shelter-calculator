package net.typeblog.shelter.plus.launcher;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CalculatorExpressionTest {
    @Test public void respectsPrecedenceAndParentheses() {
        assertNumeric("14", "2+3*4");
        assertNumeric("7", "(1+2.5)*4/2");
    }

    @Test public void supportsSignsDecimalsAndRemainder() {
        assertNumeric("-2", "-4+2");
        assertNumeric("1", "10%3");
    }

    @Test public void rejectsMalformedExpressionsAndDivisionByZero() {
        assertInvalid("1+", "1/0", "(3+2", "4 5", ".");
    }

    private void assertInvalid(String... expressions) {
        for (String expression : expressions) {
            try { new CalculatorExpression(expression).evaluate(); fail("accepted: " + expression); }
            catch (IllegalArgumentException expected) { }
        }
    }

    private void assertNumeric(String expected, String expression) {
        assertEquals(0, new BigDecimal(expected).compareTo(new CalculatorExpression(expression).evaluate()));
    }
}

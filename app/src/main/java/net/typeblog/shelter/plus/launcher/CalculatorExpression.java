package net.typeblog.shelter.plus.launcher;

import java.math.BigDecimal;
import java.math.MathContext;

/** Small offline decimal parser for the public calculator. */
final class CalculatorExpression {
    private static final MathContext MC = MathContext.DECIMAL64;
    private final String text;
    private int at;
    CalculatorExpression(String text) { this.text = text; }
    BigDecimal evaluate() {
        BigDecimal value = sum();
        if (at != text.length()) throw new IllegalArgumentException();
        return value;
    }
    private BigDecimal sum() {
        BigDecimal x = product();
        while (take('+') || take('-')) { char op = text.charAt(at - 1); BigDecimal y = product(); x = op == '+' ? x.add(y, MC) : x.subtract(y, MC); }
        return x;
    }
    private BigDecimal product() {
        BigDecimal x = unary();
        while (take('*') || take('/') || take('%')) {
            char op = text.charAt(at - 1); BigDecimal y = unary();
            if ((op == '/' || op == '%') && y.signum() == 0) throw new IllegalArgumentException();
            x = op == '*' ? x.multiply(y, MC) : op == '/' ? x.divide(y, MC) : x.remainder(y, MC);
        }
        return x;
    }
    private BigDecimal unary() {
        if (take('+')) return unary();
        if (take('-')) return unary().negate(MC);
        if (take('(')) { BigDecimal value = sum(); if (!take(')')) throw new IllegalArgumentException(); return value; }
        int start = at;
        boolean dot = false;
        while (at < text.length()) {
            char c = text.charAt(at);
            if (c == '.' && !dot) { dot = true; at++; }
            else if (Character.isDigit(c)) at++;
            else break;
        }
        if (start == at || (text.charAt(start) == '.' && at - start == 1)) throw new IllegalArgumentException();
        try { return new BigDecimal(text.substring(start, at), MC); }
        catch (NumberFormatException badNumber) { throw new IllegalArgumentException(); }
    }
    private boolean take(char expected) { if (at < text.length() && text.charAt(at) == expected) { at++; return true; } return false; }
}

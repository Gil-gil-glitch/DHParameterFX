package com.example.dhparameterfx;


public class ExpressionParser {

    /**
     * Parses expressions like:
     * - Decimals: "10.5", "-4.2"
     * - Fractions: "1/2", "-3/4"
     * - Pi expressions: "pi", "pi/2", "2*pi", "3pi/4", "-pi/6", "1.5pi"
     *
     * @return parsed double value
     * @throws NumberFormatException if the expression is invalid
     */

    public static double parse(String input) throws NumberFormatException {

        if (input == null || input.trim().isEmpty()) {

            throw new NumberFormatException("Empty input");

        }

        String expr = input.trim().toLowerCase().replaceAll("\\s+", "");


        expr = expr.replace("π", "pi");  // Replace Greek symbol π with 'pi'

        // Handle pi expressions
        if (expr.contains("pi")) {

            return parsePiExpression(expr);

        }

        // Handle standard fractions or numbers
        return parseFractionOrDouble(expr);

    }

    private static double parsePiExpression(String expr) {

        if (expr.equals("pi") || expr.equals("+pi")) return Math.PI; // Handle standalone or negated pi
        if (expr.equals("-pi")) return -Math.PI;


        if (expr.contains("/")) {  // Split by '/' if it's a fraction with pi
            String[] parts = expr.split("/");
            if (parts.length != 2) throw new NumberFormatException("Invalid fraction format");

            double numerator = parsePiNumerator(parts[0]);
            double denominator = parseFractionOrDouble(parts[1]);

            if (denominator == 0) throw new ArithmeticException("Division by zero");
            return numerator / denominator;

        }

        return parsePiNumerator(expr);
    }

    private static double parsePiNumerator(String expr) {
        if (expr.equals("pi") || expr.equals("+pi")) return Math.PI;
        if (expr.equals("-pi")) return -Math.PI;

        String coeffStr = expr.replace("*pi", "").replace("pi", "");
        if (coeffStr.isEmpty() || coeffStr.equals("+")) return Math.PI;
        if (coeffStr.equals("-")) return -Math.PI;

        double coeff = Double.parseDouble(coeffStr);
        return coeff * Math.PI;

    }

    private static double parseFractionOrDouble(String expr) {
        if (expr.contains("/")) {
            String[] parts = expr.split("/");
            if (parts.length != 2) throw new NumberFormatException("Invalid fraction");
            double num = Double.parseDouble(parts[0]);
            double den = Double.parseDouble(parts[1]);

            if (den == 0) throw new ArithmeticException("Division by zero");
            return num / den;
        }
        return Double.parseDouble(expr);

    }
}
package sample;

public class SimpleDiscountCalculator {

    public double calculate(double amount) {
        if (amount > 100) {
            return 0.10;
        }

        return 0.0;
    }
}

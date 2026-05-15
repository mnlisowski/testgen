package sample;

public class MultiBranchCalculator {

    public double calculate(double amount) {
        if (amount > 100) {
            return 0.10;
        }

        if (amount > 50) {
            return 0.05;
        }

        return 0.0;
    }

    public boolean isPositive(int value) {
        if (value > 0) {
            return true;
        }

        return false;
    }
}

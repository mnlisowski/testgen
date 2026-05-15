package sample;

public class LoopExamples {

    public int sumWhile(int limit) {
        int sum = 0;
        int value = 0;

        while (value < limit) {
            sum += value;
            value++;
        }

        return sum;
    }

    public int sumFor(int limit) {
        int sum = 0;

        for (int value = 0; value < limit; value++) {
            sum += value;
        }

        return sum;
    }
}

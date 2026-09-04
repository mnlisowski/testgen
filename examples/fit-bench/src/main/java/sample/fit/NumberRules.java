package sample.fit;

public class NumberRules {
    public String classifyAmount(int amount) {
        if (amount < 0) {
            return "negative";
        }
        if (amount == 0) {
            return "zero";
        }
        if (amount < 10) {
            return "tiny";
        }
        if (amount < 100) {
            return "normal";
        }
        if (amount == 100) {
            return "boundary";
        }
        return "large";
    }

    public int boundedScore(int value, int min, int max) {
        if (min > max) {
            return -1;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        if (value == 42) {
            return value + 1;
        }
        return value;
    }

    public boolean isInteresting(int left, int right, boolean strict) {
        if (strict && left == right) {
            return false;
        }
        if (left < 0 || right < 0) {
            return false;
        }
        if (left + right > 100) {
            return true;
        }
        return left * right > 50;
    }

    public int loopScore(int limit) {
        if (limit < 0) {
            return -1;
        }
        int score = 0;
        int index = 0;
        while (index < limit) {
            score += index;
            if (index == 3) {
                score += 10;
            }
            index++;
        }
        return score;
    }
}

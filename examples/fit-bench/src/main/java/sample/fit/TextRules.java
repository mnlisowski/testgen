package sample.fit;

public class TextRules {
    public String classifyCode(String code) {
        if (code == null) {
            return "missing";
        }
        if (code.isBlank()) {
            return "blank";
        }
        if (code.equals("ADMIN")) {
            return "admin";
        }
        if (code.startsWith("VIP")) {
            return "vip";
        }
        if (code.contains("-")) {
            return "compound";
        }
        if (code.length() > 8) {
            return "long";
        }
        return "plain";
    }

    public boolean isAllowedCountry(String countryCode) {
        if (countryCode == null) {
            return false;
        }
        switch (countryCode) {
            case "PL":
            case "DE":
            case "FR":
                return true;
            case "XX":
                return false;
            default:
                return countryCode.length() == 2;
        }
    }

    public int similarityBucket(String left, String right) {
        if (left == null || right == null) {
            return -1;
        }
        if (left.equals(right)) {
            return 100;
        }
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int same = 0;
        int index = 0;
        while (index < left.length() && index < right.length()) {
            if (left.charAt(index) == right.charAt(index)) {
                same++;
            }
            index++;
        }
        return same;
    }
}

package sample;

public class StructureExample {
    private final String prefix;

    public StructureExample() {
        this.prefix = "";
    }

    public StructureExample(String prefix) {
        this.prefix = prefix;
    }

    public String format(String value, int count) {
        return prefix + value + count;
    }

    private void helper() {
    }
}

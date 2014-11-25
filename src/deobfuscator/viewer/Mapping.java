package deobfuscator.viewer;

/**
 * Created by sjohnson on 11/25/14.
 */
public class Mapping {
    public final String original;
    public final String transformed;
    public final double percentage;

    public Mapping(String original, String transformed, double percentage) {
        this.original = original;
        this.transformed = transformed;
        this.percentage = percentage;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Mapping) {
            Mapping map = (Mapping) obj;
            return map.original.equals(original) && map.transformed.equals(transformed) && map.percentage == percentage;
        }
        return false;
    }
    @Override
    public int hashCode() {
        return (original+"->"+transformed+":"+percentage).hashCode();
    }
}

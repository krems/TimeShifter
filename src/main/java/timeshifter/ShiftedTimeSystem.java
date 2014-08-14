package timeshifter;

/**
 * Class, containing methods to replace standard java.lang.System methods
 */
public class ShiftedTimeSystem {
    public static long currentTimeMillis() {
        if (MainClass.verbose) {
            System.out.println("Replacing currentMillis method called");
        }
        long shift = ShiftExtractor.getShiftMillis();
        return System.currentTimeMillis() + shift;
    }

    public static long nanoTime() {
        if (MainClass.verbose) {
            System.out.println("Replacing nanoTime method called");
        }
        long shift = ShiftExtractor.getShiftMillis() * 1_000_000;
        return System.nanoTime() + shift;
    }
}

package timeshifter;

/**
 * Class, containing methods to replace standard java.lang.System methods
 */
public class ShiftedTimeSystem {
    public static long currentTimeMillis() {
        if (MainClass.verbose) {
            System.out.println("Timeshifter: Replacing currentMillis method called");
        }
        long shift = ShiftExtractor.getShiftMillis();
        if (MainClass.verbose) {
            System.out.println("Timeshifter: currentMillis shift = " + shift);
        }
        return System.currentTimeMillis() + shift;
    }

    public static long nanoTime() {
        if (MainClass.verbose) {
            System.out.println("Timeshifter: Replacing nanoTime method called");
        }
        long shift = ShiftExtractor.getShiftMillis() * 1_000_000;
        if (MainClass.verbose) {
            System.out.println("Timeshifter: nanoTime shift = " + shift);
        }
        return System.nanoTime() + shift;
    }
}

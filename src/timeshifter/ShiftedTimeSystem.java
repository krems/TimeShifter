package timeshifter;

public class ShiftedTimeSystem {
    public static long currentTimeMillis() {
        if (MainClass.verbose) {
            System.out.println("Replacing method called");
        }
        long shift = ShiftExtractor.getShift();
        return System.currentTimeMillis() + shift;
    }
}

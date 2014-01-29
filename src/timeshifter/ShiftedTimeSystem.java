package timeshifter;

public class ShiftedTimeSystem {
    public static long currentTimeMillis() {
        long shift = ShiftExtractor.getShift();
        return System.currentTimeMillis() + shift;
    }
}

package timeshifter;

public class ShiftedTimeSystem {
    public static long currentTimeMillis() {
        long currentTimeMillis = System.currentTimeMillis();
        long shift = ShiftExtractor.getShift();
        return currentTimeMillis + shift;
    }
}

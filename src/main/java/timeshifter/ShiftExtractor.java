package timeshifter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class that calculates shift value to be used in time shifting.
 * Shift value is specified in new_date.conf file.
 */
class ShiftExtractor {

    private static AtomicLong lastModified = new AtomicLong(0);
    private static volatile long timeShift;
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat(MainClass.FORMAT, Locale.ROOT);
                }
            };

    /**
     * Returns shift in millis. Checks if file new_date.conf was modified and reloads new shift value from it.
     * @return time shift in millis
     */
    public static long getShiftMillis() {
        long lastModifiedOld = lastModified.get();
        if (lastModifiedOld > 0 && !MainClass.CONF_FILE.exists()) {
            lastModified.set(0);
            timeShift = 0;
            return timeShift;
        }
        if (lastModifiedOld < MainClass.CONF_FILE.lastModified()) {
            // While current thread processing update the others can just work
            // with the old timeShift value
            if (lastModified.compareAndSet(lastModifiedOld, MainClass.CONF_FILE.lastModified())) {
                if (MainClass.verbose) {
                    System.out.println("Timeshifter: File modification detected");
                }
                timeShift = readShiftFromFile();
                if (MainClass.verbose) {
                    System.out.println("Timeshifter: New shift = " + timeShift);
                }
            }
        }
        return timeShift;
    }

    private static long readShiftFromFile() {
        if (MainClass.verbose) {
            System.out.println("Timeshifter: Reading data from file " + MainClass.CONF_FILE.getAbsolutePath());
        }
        BufferedReader dateFile = null;
        try {
            dateFile = new BufferedReader(new FileReader(MainClass.CONF_FILE));
            String dateLine = dateFile.readLine();
            if (dateLine != null && !dateLine.trim().isEmpty()) {
                try {
                    Date date = DATE_FORMAT.get().parse(dateLine);
                    if (MainClass.verbose) {
                        System.out.println("Timeshifter: Date from file: " + date);
                    }
                    return date.getTime() - System.currentTimeMillis();
                } catch (ParseException e) {
                    System.out.println("Timeshifter: ParseException: ");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Timeshifter: File is empty");
            }
        } catch (IOException e) {
            System.out.println("Timeshifter: File read error: ");
            e.printStackTrace();
        } finally {
            if (dateFile != null) {
                try {
                    dateFile.close();
                } catch (IOException e) {
                    System.out.println("Timeshifter: File Closing Error: ");
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
}

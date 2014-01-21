package timeshifter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ShiftExtractor {

    private static volatile long lastModified = 0;
    private static volatile long timeShift = 0;
    private static final Logger log =
            LoggerFactory.getLogger(ShiftExtractor.class);
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat(MainClass.FORMAT, Locale.ROOT);
                }
            };

    public static long getShift() {
        if ((lastModified > 0 && !MainClass.FILE.exists()) ||
                lastModified < MainClass.FILE.lastModified()) {
            System.out.println("File modification detected");
            synchronized (MainClass.FILE) {
                if (MainClass.FILE.exists()) {
                    lastModified = MainClass.FILE.lastModified();

                    long newTime = readShiftFromFile();
                    if (newTime > 0) {
                        timeShift = newTime;
                    }
                } else {
                    lastModified = 0;
                    timeShift = 0;
                }
            }
        }
        return timeShift;
    }

    private static long readShiftFromFile() {
        log.info("Reading data from file '{}'",
                MainClass.FILE.getAbsolutePath());
        long shift = 0;
        BufferedReader dateFile = null;
        try {
            dateFile = new BufferedReader(new FileReader(MainClass.FILE));
            String dateLine = dateFile.readLine();
            if (dateLine != null && !dateLine.trim().isEmpty()) {

                try {
                    Date date = DATE_FORMAT.get().parse(dateLine);
                    log.info("Loaded date from file: '{}'", date);
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    Calendar now = Calendar.getInstance();
//                    long offset = c.get(Calendar.ZONE_OFFSET) +
                            c.get(Calendar.DST_OFFSET);
//                    log.info("Offset: {}", offset);
//                    res += offset;
                    shift = c.getTime().getTime() - now.getTime().getTime();
                } catch (ParseException e) {
                    log.error("ParseException: ", e);
                }
            } else {
                log.error("File is empty");
            }
        } catch (IOException e) {
            log.error("File read error: ", e);
        } finally {
            if (dateFile != null) {
                try {
                    dateFile.close();
                } catch (IOException e) {
                    log.error("File Closing Error: ", e);
                }
            }
        }

        return shift;
    }
}

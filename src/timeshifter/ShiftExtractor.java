package timeshifter;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ShiftExtractor {

    private static volatile long lastModified;
    private static volatile long timeShift;
//    private static final Logger log =
//            LoggerFactory.getLogger(ShiftExtractor.class);
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
            synchronized (MainClass.FILE) {
                if (((lastModified > 0 && !MainClass.FILE.exists()) ||
                            lastModified < MainClass.FILE.lastModified())) {
                    return timeShift;
                }
                if (MainClass.verbose) {
//                    log.info("File modification detected");
                    System.out.println(
                           "Timeshifter: File modification detected");
                }
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
//        log.info("Reading data from file {}",
//                MainClass.FILE.getAbsolutePath());
        if (MainClass.verbose) {
            System.out.println("Timeshifter: Reading data from file " +
                    MainClass.FILE.getAbsolutePath());
        }
        long shift = 0;
        BufferedReader dateFile = null;
        try {
            dateFile = new BufferedReader(new FileReader(MainClass.FILE));
            String dateLine = dateFile.readLine();
            if (dateLine != null && !dateLine.trim().isEmpty()) {

                try {
                    Date date = DATE_FORMAT.get().parse(dateLine);
//                    log.info("Loaded date from file: {}", date);
                    if (MainClass.verbose) {
                        System.out.println(
                                "Timeshifter: Reading data from file " + date);
                    }
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    Calendar now = Calendar.getInstance();
                    shift = c.getTime().getTime() - now.getTime().getTime();
                } catch (ParseException e) {
//                    log.error("ParseException: ", e);
                    System.out.println("Timeshifter: ParseException: ");
                    e.printStackTrace();
                }
            } else {
//                log.error("File is empty");
                System.out.println("Timeshifter: File is empty");
            }
        } catch (IOException e) {
//            log.error("File read error: ", e);
            System.out.println("Timeshifter: File read error: ");
            e.printStackTrace();
        } finally {
            if (dateFile != null) {
                try {
                    dateFile.close();
                } catch (IOException e) {
//                    log.error("File Closing Error: ", e);
                    System.out.println("Timeshifter: File Closing Error: ");
                    e.printStackTrace();
                }
            }
        }

        return shift;
    }
}

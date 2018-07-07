package cn.ocoop.framework.id;

import com.google.common.base.Preconditions;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class Id {
    private static final long EPOCH = LocalDateTime.of(2018, 5, 29, 13, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_BITS = 10L;
    static final long WORKER_ID_MAX_VALUE = 1L << WORKER_ID_BITS;
    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;
    static Long WORKER_ID;
    private static long SEQUENCE;
    private static long LAST_TIME;

    public Id(Long workerId) {
        WORKER_ID = workerId;
    }

    public static long next() {
        return nextKey();
    }

    public static String nextString() {
        return String.valueOf(next());
    }

    private static synchronized long nextKey() {
        long currentMillis = System.currentTimeMillis();
        Preconditions.checkState(LAST_TIME <= currentMillis, "Clock is moving backwards, last time is %d milliseconds, current time is %d milliseconds", LAST_TIME, currentMillis);
        if (LAST_TIME == currentMillis) {
            if (0L == (SEQUENCE = (SEQUENCE + 1) & SEQUENCE_MASK)) {
                currentMillis = waitUntilNextTime(currentMillis);
            }
        } else {
            SEQUENCE = 0;
        }
        LAST_TIME = currentMillis;
        return (currentMillis - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS | (WORKER_ID << WORKER_ID_LEFT_SHIFT_BITS) | SEQUENCE;
    }

    private static long waitUntilNextTime(final long lastTime) {
        long time = System.currentTimeMillis();
        while (time <= lastTime) {
            time = System.currentTimeMillis();
        }
        return time;
    }

}

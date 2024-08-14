package com.arangodb.springframework.core.convert;

import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JavaTimeUtilTest {
    private static final DateFormat OLD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        OLD_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private Date oldParse(final String source) throws ParseException {
        return OLD_DATE_FORMAT.parse(source);
    }

    private static String oldFormat(final Date date) {
        return OLD_DATE_FORMAT.format(date);
    }

    @Test
    void roundTrip() throws ParseException {
        var input = "2018-04-16T15:17:21.005Z";
        Date d = JavaTimeUtil.parse(input);
        String f = JavaTimeUtil.format(d);
        assertThat(f).isEqualTo(input);
    }

    @Test
    void oldRoundTrip() throws ParseException {
        var input = "2018-04-16T15:17:21.005Z";
        Date d = oldParse(input);
        String f = oldFormat(d);
        assertThat(f).isEqualTo(input);
    }

    @Test
    void parse() throws ParseException {
        var input = "2018-04-16T15:17:21.005Z";
        assertThat(JavaTimeUtil.parse(input)).isEqualTo(oldParse(input));
    }

    @Test
    void oldParseException() {
        var input = "2018/04/16T15:17:21.005Z";
        ParseException e = assertThrows(ParseException.class, () -> oldParse(input));
        assertThat(e.getMessage()).contains(input);
        assertThat(e.getErrorOffset()).isEqualTo(4);
    }

    @Test
    void parseException() {
        var input = "2018/04/16T15:17:21.005Z";
        ParseException e = assertThrows(ParseException.class, () -> JavaTimeUtil.parse(input));
        assertThat(e.getMessage()).contains(input);
        assertThat(e.getErrorOffset()).isEqualTo(4);
    }

    @Test
    void format() {
        var input = new Date(1723642733350L);
        assertThat(JavaTimeUtil.format(input)).isEqualTo(oldFormat(input));
    }

    @Test
    void concurrentParse() {
        testConcurrent(date -> JavaTimeUtil.parse(JavaTimeUtil.format(date)));
    }

    @Test
    void oldConcurrentParse() {
        assertThrows(RuntimeException.class, () -> testConcurrent(date -> oldParse(JavaTimeUtil.format(date))));
    }

    @Test
    void concurrentFormat() {
        testConcurrent(JavaTimeUtil::format);
    }

    @Test
    void oldConcurrentFormat() {
        assertThrows(RuntimeException.class, () -> testConcurrent(JavaTimeUtilTest::oldFormat));
    }

    private void testConcurrent(ThrowingConsumer<Date> fn) {
        AtomicReference<Throwable> e = new AtomicReference<>();
        Date[] dates;
        try {
            dates = new Date[]{
                    JavaTimeUtil.parse("2018-04-16T15:17:21.005Z"),
                    JavaTimeUtil.parse("2019-04-16T15:17:21.020Z")
            };
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }

        var threads = IntStream.range(0, 16)
                .mapToObj(i -> dates[i % dates.length])
                .map(date -> new Thread(() -> {
                    for (int j = 0; j < 10_000; j++) {
                        fn.accept(date);
                    }
                }))
                .toList();

        for (Thread t : threads) {
            t.setUncaughtExceptionHandler((th, ex) -> {
                e.set(ex);
            });
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        Throwable thrown = e.get();
        if (thrown != null) {
            throw new RuntimeException(thrown);
        }
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> extends Consumer<T> {

        @Override
        default void accept(final T elem) {
            try {
                acceptThrows(elem);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        void acceptThrows(T elem) throws Exception;
    }

}

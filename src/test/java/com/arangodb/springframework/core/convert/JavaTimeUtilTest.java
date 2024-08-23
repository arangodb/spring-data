package com.arangodb.springframework.core.convert;

import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;

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
    void formatInstant() {
        Instant instant = Instant.ofEpochMilli(1723642733351L);
        assertThat(JavaTimeUtil.format(instant)).isEqualTo("2024-08-14T13:38:53.351Z");
    }

    @Test
    void parseInstant() {
        Instant instant = JavaTimeUtil.parseInstant("2024-08-14T13:38:53.351Z");
        assertThat(instant).isEqualTo(Instant.ofEpochMilli(1723642733351L));
    }

    @Test
    void formatLocalDate() {
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(1723642733351L), ZoneOffset.UTC);
        assertThat(JavaTimeUtil.format(date)).isEqualTo("2024-08-14");
    }

    @Test
    void parseLocalDate() {
        LocalDate date = JavaTimeUtil.parseLocalDate("2024-08-14");
        assertThat(date)
                .hasDayOfMonth(14)
                .hasMonth(Month.AUGUST)
                .hasYear(2024);
    }

    @Test
    void formatOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(1723642733351L), ZoneOffset.UTC);
        assertThat(JavaTimeUtil.format(odt)).isEqualTo("2024-08-14T13:38:53.351Z");
    }

    @Test
    void parseOffsetDateTime() {
        OffsetDateTime odt = JavaTimeUtil.parseOffsetDateTime("2024-08-14T13:38:53.351Z");
        assertThat(odt.toInstant().toEpochMilli()).isEqualTo(1723642733351L);
    }

    @Test
    void formatLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(1723642733351L), ZoneOffset.UTC);
        assertThat(JavaTimeUtil.format(ldt)).isEqualTo("2024-08-14T13:38:53.351");
    }

    @Test
    void parseLocalDateTime() {
        LocalDateTime ldt = JavaTimeUtil.parseLocalDateTime("2024-08-14T13:38:53.351");
        assertThat(ldt.toInstant(ZoneOffset.UTC).toEpochMilli()).isEqualTo(1723642733351L);
    }

    @Test
    void formatLocalTime() {
        LocalTime lt = LocalTime.ofInstant(Instant.ofEpochMilli(1723642733351L), ZoneOffset.UTC);
        assertThat(JavaTimeUtil.format(lt)).isEqualTo("13:38:53.351");
    }

    @Test
    void parseLocalTime() {
        LocalTime lt = JavaTimeUtil.parseLocalTime("13:38:53.351");
        assertThat(lt)
                .hasHour(13)
                .hasMinute(38)
                .hasSecond(53)
                .hasNano(351_000_000);
    }

    @Test
    void formatZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1723642733351L), ZoneOffset.UTC);
        assertThat((JavaTimeUtil.format(zdt))).isEqualTo("2024-08-14T13:38:53.351Z");
    }

    @Test
    void parseZonedDateTime() {
        ZonedDateTime zdt = JavaTimeUtil.parseZonedDateTime("2024-08-14T13:38:53.351Z");
        assertThat(zdt.toInstant().toEpochMilli()).isEqualTo(1723642733351L);
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
        var input = new Date(1723642733351L);
        assertThat(JavaTimeUtil.format(input)).isEqualTo(oldFormat(input));
    }

}

package org.tasks.repeats

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.analytics.Firebase
import java.util.Locale

/**
 * JVM unit tests for [RepeatRuleToString].
 *
 * The production class delegates all user-facing strings to Android Context /
 * Resources, so we mock those and verify that the correct string-resource IDs
 * are resolved and that the formatter picks the right branch (single vs.
 * plural, with/without days, with/without count/until).
 */
class RepeatRuleToStringTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var firebase: Firebase
    private lateinit var formatter: RepeatRuleToString

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        resources = mock(Resources::class.java)
        firebase = mock(Firebase::class.java)
        `when`(context.resources).thenReturn(resources)

        // --- single-frequency strings ---
        `when`(context.getString(R.string.repeats_minutely)).thenReturn("minutely")
        `when`(context.getString(R.string.repeats_hourly)).thenReturn("hourly")
        `when`(context.getString(R.string.repeats_daily)).thenReturn("daily")
        `when`(context.getString(R.string.repeats_weekly)).thenReturn("weekly")
        `when`(context.getString(R.string.repeats_monthly)).thenReturn("monthly")
        `when`(context.getString(R.string.repeats_yearly)).thenReturn("yearly")

        // --- "Repeats %s" ---
        `when`(context.getString(eq(R.string.repeats_single), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]}" }

        // --- "Repeats every %s" ---
        `when`(context.getString(eq(R.string.repeats_plural), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]}" }

        // --- "Repeats %1$s on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_single_on), anyString(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]} on ${it.arguments[2]}" }

        // --- "Repeats every %1$s on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_on), anyString(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]} on ${it.arguments[2]}" }

        // --- "Repeats %1$s, occurs %2$d %3$s" ---
        `when`(context.getString(eq(R.string.repeats_single_number_of_times), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]}, occurs ${it.arguments[2]} ${it.arguments[3]}" }

        // --- "Repeats every %1$s, occurs %2$d %3$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_number_of_times), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]}, occurs ${it.arguments[2]} ${it.arguments[3]}" }

        // --- "Repeats %1$s on %2$s, occurs %3$d %4$s" ---
        `when`(context.getString(eq(R.string.repeats_single_on_number_of_times), anyString(), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]} on ${it.arguments[2]}, occurs ${it.arguments[3]} ${it.arguments[4]}" }

        // --- "Repeats every %1$s on %2$s, occurs %3$d %4$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_on_number_of_times), anyString(), anyString(), anyInt(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]} on ${it.arguments[2]}, occurs ${it.arguments[3]} ${it.arguments[4]}" }

        // --- "Repeats %1$s, ends on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_single_until), anyString(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]}, ends on ${it.arguments[2]}" }

        // --- "Repeats every %1$s, ends on %2$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_until), anyString(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]}, ends on ${it.arguments[2]}" }

        // --- "Repeats %1$s on %2$s, ends on %3$s" ---
        `when`(context.getString(eq(R.string.repeats_single_on_until), anyString(), anyString(), anyString()))
            .thenAnswer { "Repeats ${it.arguments[1]} on ${it.arguments[2]}, ends on ${it.arguments[3]}" }

        // --- "Repeats every %1$s on %2$s, ends on %3$s" ---
        `when`(context.getString(eq(R.string.repeats_plural_on_until), anyString(), anyString(), anyString()))
            .thenAnswer { "Repeats every ${it.arguments[1]} on ${it.arguments[2]}, ends on ${it.arguments[3]}" }

        // --- list separator ---
        `when`(context.getString(R.string.list_separator_with_space)).thenReturn(", ")

        // --- plurals for frequency intervals ---
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_minutes), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} minutes" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_hours), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} hours" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_days), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} days" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_weeks), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} weeks" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_months), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} months" }
        `when`(resources.getQuantityString(eq(R.plurals.repeat_n_years), anyInt(), anyInt()))
            .thenAnswer { "${it.arguments[2]} years" }

        // --- plurals for count ("time"/"times") ---
        `when`(resources.getQuantityString(eq(R.plurals.repeat_times), anyInt()))
            .thenAnswer { if (it.arguments[1] as Int == 1) "time" else "times" }

        // --- monthly week names ---
        `when`(context.getString(R.string.repeat_monthly_first_week)).thenReturn("first")
        `when`(context.getString(R.string.repeat_monthly_second_week)).thenReturn("second")
        `when`(context.getString(R.string.repeat_monthly_third_week)).thenReturn("third")
        `when`(context.getString(R.string.repeat_monthly_fourth_week)).thenReturn("fourth")
        `when`(context.getString(R.string.repeat_monthly_fifth_week)).thenReturn("fifth")
        `when`(context.getString(R.string.repeat_monthly_last_week)).thenReturn("last")

        // --- "every %1$s %2$s" for monthly day-of-nth-week ---
        `when`(context.getString(eq(R.string.repeat_monthly_every_day_of_nth_week), anyString(), anyString()))
            .thenAnswer { "every ${it.arguments[1]} ${it.arguments[2]}" }

        formatter = RepeatRuleToString(context, Locale.US, firebase)
    }

    // ------------------------------------------------------------------ //
    //  Null / blank input
    // ------------------------------------------------------------------ //

    @Test
    fun nullInput_returnsNull() {
        assertNull(formatter.toString(null as String?))
    }

    @Test
    fun emptyInput_returnsNull() {
        assertNull(formatter.toString(""))
    }

    @Test
    fun blankInput_returnsNull() {
        assertNull(formatter.toString("   "))
    }

    // ------------------------------------------------------------------ //
    //  Single-interval frequencies
    // ------------------------------------------------------------------ //

    @Test
    fun daily_singleInterval() {
        assertEquals("Repeats daily", formatter.toString("RRULE:FREQ=DAILY"))
    }

    @Test
    fun weekly_singleInterval() {
        assertEquals("Repeats weekly", formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1"))
    }

    @Test
    fun monthly_singleInterval() {
        assertEquals("Repeats monthly", formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1"))
    }

    @Test
    fun yearly_singleInterval() {
        assertEquals("Repeats yearly", formatter.toString("RRULE:FREQ=YEARLY;INTERVAL=1"))
    }

    @Test
    fun hourly_singleInterval() {
        assertEquals("Repeats hourly", formatter.toString("RRULE:FREQ=HOURLY;INTERVAL=1"))
    }

    @Test
    fun minutely_singleInterval() {
        assertEquals("Repeats minutely", formatter.toString("RRULE:FREQ=MINUTELY;INTERVAL=1"))
    }

    // ------------------------------------------------------------------ //
    //  Plural-interval frequencies
    // ------------------------------------------------------------------ //

    @Test
    fun daily_pluralInterval() {
        assertEquals("Repeats every 3 days", formatter.toString("RRULE:FREQ=DAILY;INTERVAL=3"))
    }

    @Test
    fun weekly_pluralInterval() {
        assertEquals("Repeats every 2 weeks", formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=2"))
    }

    @Test
    fun monthly_pluralInterval() {
        assertEquals("Repeats every 6 months", formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=6"))
    }

    @Test
    fun yearly_pluralInterval() {
        assertEquals("Repeats every 5 years", formatter.toString("RRULE:FREQ=YEARLY;INTERVAL=5"))
    }

    @Test
    fun hourly_pluralInterval() {
        assertEquals("Repeats every 4 hours", formatter.toString("RRULE:FREQ=HOURLY;INTERVAL=4"))
    }

    @Test
    fun minutely_pluralInterval() {
        assertEquals("Repeats every 15 minutes", formatter.toString("RRULE:FREQ=MINUTELY;INTERVAL=15"))
    }

    // ------------------------------------------------------------------ //
    //  Weekly with BYDAY
    // ------------------------------------------------------------------ //

    @Test
    fun weekly_withSingleDay() {
        assertEquals(
            "Repeats weekly on Mon",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO")
        )
    }

    @Test
    fun weekly_withMultipleDays() {
        assertEquals(
            "Repeats weekly on Mon, Wed, Fri",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR")
        )
    }

    @Test
    fun weekly_withAllWeekdays() {
        assertEquals(
            "Repeats weekly on Mon, Tue, Wed, Thu, Fri",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR")
        )
    }

    @Test
    fun weekly_pluralWithDays() {
        assertEquals(
            "Repeats every 2 weeks on Tue, Thu",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,TH")
        )
    }

    @Test
    fun weekly_preservesDayOrder() {
        assertEquals(
            "Repeats weekly on Fri, Thu, Wed",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=FR,TH,WE")
        )
    }

    // ------------------------------------------------------------------ //
    //  Monthly with BYDAY (nth weekday)
    // ------------------------------------------------------------------ //

    @Test
    fun monthly_firstMonday() {
        assertEquals(
            "Repeats monthly on every first Monday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=1MO")
        )
    }

    @Test
    fun monthly_secondTuesday() {
        assertEquals(
            "Repeats monthly on every second Tuesday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=2TU")
        )
    }

    @Test
    fun monthly_thirdWednesday() {
        assertEquals(
            "Repeats monthly on every third Wednesday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=3WE")
        )
    }

    @Test
    fun monthly_fourthThursday() {
        assertEquals(
            "Repeats monthly on every fourth Thursday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=4TH")
        )
    }

    @Test
    fun monthly_fifthFriday() {
        assertEquals(
            "Repeats monthly on every fifth Friday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=5FR")
        )
    }

    @Test
    fun monthly_lastSunday() {
        assertEquals(
            "Repeats monthly on every last Sunday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=-1SU")
        )
    }

    @Test
    fun monthly_lastWednesday() {
        assertEquals(
            "Repeats monthly on every last Wednesday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=-1WE")
        )
    }

    @Test
    fun monthly_pluralWithDay() {
        assertEquals(
            "Repeats every 2 months on every first Thursday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=1TH")
        )
    }

    // ------------------------------------------------------------------ //
    //  With COUNT
    // ------------------------------------------------------------------ //

    @Test
    fun daily_withCount() {
        assertEquals(
            "Repeats daily, occurs 5 times",
            formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1;COUNT=5")
        )
    }

    @Test
    fun weekly_pluralWithCount() {
        assertEquals(
            "Repeats every 3 weeks, occurs 10 times",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=3;COUNT=10")
        )
    }

    @Test
    fun weekly_withDaysAndCount() {
        assertEquals(
            "Repeats weekly on Mon, Wed, occurs 8 times",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE;COUNT=8")
        )
    }

    @Test
    fun monthly_pluralWithDayAndCount() {
        assertEquals(
            "Repeats every 2 months on every first Monday, occurs 12 times",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=1MO;COUNT=12")
        )
    }

    // ------------------------------------------------------------------ //
    //  RRULE prefix handling
    // ------------------------------------------------------------------ //

    @Test
    fun withoutRRulePrefix() {
        assertEquals("Repeats daily", formatter.toString("FREQ=DAILY"))
    }

    @Test
    fun withRRulePrefix() {
        assertEquals("Repeats daily", formatter.toString("RRULE:FREQ=DAILY"))
    }

    // ------------------------------------------------------------------ //
    //  Weekend days
    // ------------------------------------------------------------------ //

    @Test
    fun weekly_weekendDays() {
        assertEquals(
            "Repeats weekly on Sat, Sun",
            formatter.toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=SA,SU")
        )
    }

    @Test
    fun monthly_lastSaturday() {
        assertEquals(
            "Repeats monthly on every last Saturday",
            formatter.toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=-1SA")
        )
    }

    // ------------------------------------------------------------------ //
    //  Large interval values
    // ------------------------------------------------------------------ //

    @Test
    fun daily_largeInterval() {
        assertEquals("Repeats every 30 days", formatter.toString("RRULE:FREQ=DAILY;INTERVAL=30"))
    }

    @Test
    fun yearly_largeInterval() {
        assertEquals("Repeats every 10 years", formatter.toString("RRULE:FREQ=YEARLY;INTERVAL=10"))
    }

    // ------------------------------------------------------------------ //
    //  Single count (time vs times)
    // ------------------------------------------------------------------ //

    @Test
    fun daily_withCountOne() {
        assertEquals(
            "Repeats daily, occurs 1 time",
            formatter.toString("RRULE:FREQ=DAILY;INTERVAL=1;COUNT=1")
        )
    }
}

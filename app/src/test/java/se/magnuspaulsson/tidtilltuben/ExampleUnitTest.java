package se.magnuspaulsson.tidtilltuben;

import org.junit.Test;

import se.magnuspaulsson.tidtilltuben.helpers.ParseHelper;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void paseDate() throws Exception {
        assertNotNull(ParseHelper.parseDateTimeString("2018-01-05T09:44:30"));
    }
}
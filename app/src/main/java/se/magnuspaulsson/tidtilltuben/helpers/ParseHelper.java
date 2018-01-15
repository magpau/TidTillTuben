package se.magnuspaulsson.tidtilltuben.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by magnuspaulsson on 2018-01-05.
 */

public class ParseHelper {
    public static Date parseDateTimeString(String input) {

        try {
            SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
            return df.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}

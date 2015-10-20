package com.ukiuni.slite.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by tito on 2015/10/11.
 */
public class JSONDate {
    public static Date parse(String input) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
        if (input.endsWith("Z")) {
            input = input.substring(0, input.length() - 1) + "GMT-00:00";
        } else {
            int inset = 6;
            String first = input.substring(0, input.length() - inset);
            String last = input.substring(input.length() - inset, input.length());
            input = first + "GMT" + last;
        }

        try {
            return df.parse(input);
        } catch (ParseException e) {
            return null;
        }

    }
}

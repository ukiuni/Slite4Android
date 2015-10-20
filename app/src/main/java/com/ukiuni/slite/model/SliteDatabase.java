package com.ukiuni.slite.model;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Created by tito on 2015/10/11.
 */
@Database(name = SliteDatabase.NAME, version = SliteDatabase.VERSION)
public class SliteDatabase {
    public static final String NAME = "Slite";

    public static final int VERSION = 1;
}

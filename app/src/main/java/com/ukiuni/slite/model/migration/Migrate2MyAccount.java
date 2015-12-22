package com.ukiuni.slite.model.migration;

import android.database.sqlite.SQLiteDatabase;

import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;
import com.ukiuni.slite.model.SliteDatabase;

/**
 * Created by tito on 2015/12/20.
 */
@Migration(version = 2, databaseName = SliteDatabase.NAME)
public class Migrate2MyAccount extends BaseMigration {

    @Override
    public void migrate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("alter table MyAccount add column pushDeviceKey text");
    }
}

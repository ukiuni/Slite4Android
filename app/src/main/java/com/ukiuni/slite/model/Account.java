package com.ukiuni.slite.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;

/**
 * Created by tito on 2015/10/11.
 */
@Table(databaseName = SliteDatabase.NAME)
public class Account extends BaseModel{
    @Column
    @PrimaryKey
    public long id;

    @Column
    public String name;

    @Column
    public String iconUrl;

    @Column
    public Date createdAt;
}

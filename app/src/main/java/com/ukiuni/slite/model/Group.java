package com.ukiuni.slite.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.List;

/**
 * Created by tito on 2015/10/11.
 */
@Table(databaseName = SliteDatabase.NAME)
public class Group extends BaseModel {
    @Column
    public long id;
    @Column
    @PrimaryKey(autoincrement = true)
    public long localId;
    @Column
    public String name;
    @Column
    public String accessKey;
    @Column
    public String iconUrl;
    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "local_owner_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = true)
    public MyAccount localOwner;

    transient public List<Content> contents;
}

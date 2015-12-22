package com.ukiuni.slite.model;


import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;

/**
 * Created by tito on 2015/10/11.
 */
@Table(databaseName = SliteDatabase.NAME)
public class Content extends BaseModel {
    public static String TYPE_CALENDAR = "calendar";
    public static String TYPE_MARKDOWN = "markdown";

    @Column
    @PrimaryKey
    public long id;

    @Column
    public String accessKey;

    @Column
    public String title;

    @Column
    public String article;

    @Column
    public String imageUrl;

    @Column
    public String type;

    @Column
    public Date createdAt;

    @Column
    public Date updatedAt;

    @Column
    public String tags;

    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "group_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = true)
    public Group group;

    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "owner_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = true)
    public Account owner;

    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "load_account_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = true)
    public MyAccount loadAccount;

    // @Column
    // @ForeignKey(references = {@ForeignKeyReference(columnName = "updator_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = false)
    //  public Account updator;

    @Column
    public Date uploadedAt;

    @Column
    public Date localUpdatedAt;

    @Column
    public boolean editable;
}

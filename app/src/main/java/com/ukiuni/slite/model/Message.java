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
public class Message extends BaseModel {
    @Column
    public long id;
    @Column
    @PrimaryKey(autoincrement = true)
    public long localId;
    @Column
    public String type;
    @Column
    public String body;
    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "owner_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = true)
    public Account owner;
    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "local_owner_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = true)
    public MyAccount localOwner;
    @Column
    @ForeignKey(references = {@ForeignKeyReference(columnName = "channel_id", columnType = Long.class, foreignColumnName = "id")}, saveForeignKeyModel = true)
    public Channel channel;
    @Column
    public Date createdAt;
    @Column
    public Date updatedAt;
}

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
public class TempContent extends BaseModel {

    @Column
    @PrimaryKey(autoincrement = true)
    public long id;

    @Column
    public long serverId;

    @Column
    public String accessKey;

    @Column
    public String title;

    @Column
    public String article;

    @Column
    public String imageUrl;

    @Column
    public Date createdAt;

    @Column
    public Date updatedAt;

}

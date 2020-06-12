package org.farring.gcs.maps.providers.GoogleMap.offline.Database;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.NotNull;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.annotation.Unique;
import com.litesuits.orm.db.enums.AssignType;

import java.io.Serializable;

@Table("googleMapOffline")
public class TileBean implements Serializable {

    public static final String COL_URL = "_url";
    public static final String COL_DATA = "_data";

    @PrimaryKey(AssignType.BY_MYSELF)
    @Unique
    @NotNull
    @Column(COL_URL)
    public String url;

    @Column(COL_DATA)
    public byte[] data;

    public TileBean(String url, byte[] data) {
        this.url = url;
        this.data = data;
    }

    @Override
    public String toString() {
        return "TileBean{" +
                ", url='" + url + '\'' +
                ", data=" + data.length +
                '}';
    }
}

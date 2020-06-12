package com.tlog.bmob;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by Administrator on 2016/4/9.
 * 用于Bmob日志同步管理
 */
public class TLogFileBmobObject extends BmobObject {

    // 日志文件对象
    private BmobFile bmobFile;

    // 用户归属
    private String userName;

    // 文件的MD5码
    private String fileMD5;

    public TLogFileBmobObject(BmobFile bmobFile, String userName, String fileMD5) {
        this.bmobFile = bmobFile;
        this.userName = userName;
        this.fileMD5 = fileMD5;
    }

    public BmobFile getBmobFile() {
        return bmobFile;
    }

    public void setBmobFile(BmobFile bmobFile) {
        this.bmobFile = bmobFile;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileMD5() {
        return fileMD5;
    }

    public void setFileMD5(String fileMD5) {
        this.fileMD5 = fileMD5;
    }

    @Override
    public String toString() {
        return "TLogFileBmobObject{" +
                "bmobFile=" + bmobFile +
                ", userName='" + userName + '\'' +
                ", fileMD5='" + fileMD5 + '\'' +
                '}';
    }
}


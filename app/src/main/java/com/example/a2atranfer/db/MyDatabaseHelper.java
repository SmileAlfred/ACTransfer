package com.example.a2atranfer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    //文件名   文件类型    文件大小    创建时间    修改时间    是否上传至服务器
    public static final String ID = "_id";
    final String CREATE_TABLE_SQL =
            "create table memento_tb(" + ID + " integer primary "
                    + " key autoincrement,date,time,worker,mode,railNumber,measurePoints,"
                    + "sideWear,verticalWear,totalWear,swQ,vwQ,twQ,generalQ,valid)";


    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) {
    }
}
package com.example.android.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.inventory.data.InventoryContract.Product;
import com.example.android.inventory.data.InventoryContract.Supplier;

public class InventoryOpenHelper extends SQLiteOpenHelper {

    InventoryOpenHelper(Context context) {
        super(context, InventoryContract.DATABASE_NAME, null, InventoryContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_SUPPLIER = "CREATE TABLE " + Supplier.TABLE_NAME + " ( "
                + Supplier._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Supplier.COLUMN_NAME + " TEXT NOT NULL, "
                + Supplier.COLUMN_CONTACT + " TEXT NOT NULL);";
        db.execSQL(SQL_CREATE_SUPPLIER);

        String SQL_CREATE_PRODUCT = "CREATE TABLE " + Product.TABLE_NAME + " ( "
                + Product._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Product.COLUMN_NAME + " TEXT NOT NULL, "
                + Product.COLUMN_PRICE + " REAL NOT NULL, "
                + Product.COLUMN_QUANTITY + " INTEGER NOT NULL, "
                + Product.COLUMN_SUPPLIER + " TEXT NOT NULL, "
                + Product.COLUMN_PICTURE + " TEXT NOT NULL);";
        db.execSQL(SQL_CREATE_PRODUCT);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // no update required at the moment
    }
}

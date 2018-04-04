package com.example.android.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

/**
 * Created by Nate on 11/3/2017.
 */

/**
 * Database helper for inventory app. Manages database creation and version management.
 */
public class InventoryDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = InventoryDbHelper.class.getSimpleName();

    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "inventory.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 2;

    // Create a String that contains the SQL statement to create the inventory table
    private static final String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE "
            + InventoryEntry.TABLE_NAME + " ("
            + InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + InventoryEntry.COLUMN_ITEM_NAME + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_ITEM_IMAGE + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_ITEM_PRICE + " INTEGER NOT NULL, "
            + InventoryEntry.COLUMN_ITEM_SUPPLIER + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_SUPPLIER_EMAIL + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0);";

    private static final String DATABASE_ALTER_INVENTORY_1 = "ALTER TABLE "
            + InventoryEntry.TABLE_NAME + " ADD COLUMN " + InventoryEntry.COLUMN_SUPPLIER_EMAIL
            + " TEXT NOT NULL DEFAULT 'please add a valid email';";

    /**
     * Constructs a new instance of {@link InventoryDbHelper}.
     *
     * @param context of the app
     */
    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Execute the SQL statement
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        switch (oldVersion) {
            case 1:
                db.execSQL(DATABASE_ALTER_INVENTORY_1);
                Log.e("db version", "version= " + oldVersion);
        }
    }
}

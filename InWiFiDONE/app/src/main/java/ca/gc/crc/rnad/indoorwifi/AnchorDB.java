package ca.gc.crc.rnad.indoorwifi;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AnchorDB {
    DBHandler dbHandler;
    SQLiteDatabase sqLiteDatabase;

    String shortCode;
    String frequency;
    String speed;
    String strength;


    public AnchorDB(Context context) {
        dbHandler = new DBHandler(context);
    }

    public void openDB() {
        sqLiteDatabase = dbHandler.getWritableDatabase();
    }

    public void closeDB(){
        sqLiteDatabase.close();
    }

    public void insertRecord(String SC, String freq, String speed, String strength){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHandler.Key_ShortCode,SC);
        contentValues.put(DBHandler.Key_Frequency,freq);
        contentValues.put(DBHandler.Key_Strength,strength);
        contentValues.put(DBHandler.Key_Speed,speed);
        sqLiteDatabase.insert(DBHandler.Table_Anchor_Detail,null,contentValues);
    }

  /*  public AnchorDB( String shortCode, String frequency, String speed, String strength){
      this.shortCode = shortCode;
      this.frequency=frequency;
      this.speed=speed;
      this.strength=strength;
    } */

}
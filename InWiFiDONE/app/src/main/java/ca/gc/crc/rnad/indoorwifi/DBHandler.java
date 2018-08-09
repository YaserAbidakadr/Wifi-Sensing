 package ca.gc.crc.rnad.indoorwifi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHandler extends SQLiteOpenHelper {
   private static final int DATABASE_VERSION = 1;
   private static final String DATABASE_NAME = "Anchor";
   public static final String Table_Anchor_Detail = "AnchorDetails";


    public static final  String  Key_ShortCode = "Short_Code";
    public static final String   Key_Strength = "strength";
    public static final String   Key_Speed = "Signal_Speed";
    public  static final String   Key_Frequency = "frequency";



    public DBHandler(Context context ) {
        super(context,  DATABASE_NAME, null,DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      String CREATE_ANCHOR_DETAILS = " CREATE TABLE " + Table_Anchor_Detail + "("
              + Key_Strength + " TEXT, "
              + Key_Frequency + " TEXT,"
              + Key_ShortCode + " TEXT, "
              + Key_Speed + " TEXT" + ")";
      db.execSQL(CREATE_ANCHOR_DETAILS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
       db.execSQL("DROP TABLE IF EXISTS " + Table_Anchor_Detail );
       onCreate(db);
    }
}


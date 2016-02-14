package gr3go.pcswitch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PCManagerSQLiteHelper extends SQLiteOpenHelper {

	Logger LOG = LoggerFactory.getLogger(PCManagerSQLiteHelper.class);
	
	private static final String DATABASE_NAME = "persistency.db";
	private static final int DATABASE_VERSION = 3;
	
	public static final String TABLE_REMOTEPCS = "remotepcs";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_ADDRESS = "address";
	public static final String COLUMN_MAC = "mac";
    public static final String COLUMN_NAME = "name";
	
	private static final String DATABASE_CREATE = "create table "
		      + TABLE_REMOTEPCS + "(" + COLUMN_ID  + " integer primary key autoincrement, "
			  + COLUMN_ADDRESS + " text not null,"
		      + COLUMN_MAC + " text not null,"
              + COLUMN_NAME + " text not null);";
	
	public PCManagerSQLiteHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	  }
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		LOG.info("Upgrading database from version " + oldVersion + " to " + newVersion + " which will destroy all old data");
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMOTEPCS);
	    onCreate(db);
	}
}

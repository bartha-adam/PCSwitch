package gr3go.pcswitch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pcswitch.common.MACAddress;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class PCManagerDataSource {

	Logger LOG = LoggerFactory.getLogger(PCManagerDataSource.class);
	
	 private SQLiteDatabase database;
	 private PCManagerSQLiteHelper dbHelper;
	 
	 public PCManagerDataSource(Context context) {
		 dbHelper = new PCManagerSQLiteHelper(context);
	 }
	 
	 public void open() throws SQLException {
	    database = dbHelper.getWritableDatabase();
	 }
	 
	 public void close() {
	    dbHelper.close();
	 }
	 
	 public boolean AddPC(PC pc) {
		 ContentValues values = new ContentValues();
		 values.put(PCManagerSQLiteHelper.COLUMN_ADDRESS, pc.GetAddressStr());
		 MACAddress macAddress = pc.GetMACAddress();
		 if(macAddress == null) {
             return false;
         }
		 values.put(PCManagerSQLiteHelper.COLUMN_MAC, macAddress.toString());
		 long insertId = database.insert(PCManagerSQLiteHelper.TABLE_REMOTEPCS, null, values);
		 pc.SetDBId(insertId);
		 LOG.info("Added new " + pc.toString());
		 return true;
	 }
	 
	 public boolean DeletePC(PC pc) {
		 long id = pc.GetDBId();
		 if(id < 0) {
             return false;
         }
		 database.delete(PCManagerSQLiteHelper.TABLE_REMOTEPCS,
                 PCManagerSQLiteHelper.COLUMN_ID + " = " + id, null);
		 LOG.info("Deleted " + pc.toString());
		 return true;
	  }
	 
	 Vector<PC> GetPCs() {
		 Vector<PC> result = new Vector<PC>();
		 String [] columns = {
                 PCManagerSQLiteHelper.COLUMN_ID,
                 PCManagerSQLiteHelper.COLUMN_ADDRESS,
                 PCManagerSQLiteHelper.COLUMN_MAC};
		 Cursor cursor = database.query(PCManagerSQLiteHelper.TABLE_REMOTEPCS, columns, null, null,
                 null, null, null);
		 cursor.moveToFirst();
		 while (!cursor.isAfterLast()) {
			 PC pc = cursorToPC(cursor);
			 LOG.info("Load " + pc.toString());
			 result.add(pc);
			 cursor.moveToNext();
		 }
		 LOG.info("Loaded " + result.size() + " instances");
		 return result;
	 }
	 
	 private PC cursorToPC(Cursor cursor) {
		 try {
			InetAddress address = InetAddress.getByName(cursor.getString(1));
			PC pc = new PC(address);
			String macStr = cursor.getString(2);
			MACAddress mac = new MACAddress(macStr);
			pc.SetMACAddress(mac);
			pc.SetDBId(cursor.getLong(0));
			return pc;
		 } catch (UnknownHostException e) {
			 e.printStackTrace();
			 LOG.error("Failed to read PC from cursor ex=" + e.toString());
			 return null;
		 }
	  }
}

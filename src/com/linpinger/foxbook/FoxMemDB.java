package com.linpinger.foxbook;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class FoxMemDB {
	private String dbpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "FoxBook.db3";
	private int nowDBnum = 0 ;
	
	private boolean isMemDB = true ;
	private SQLiteDatabase db ;
	
	FoxMemDB(boolean inisMemDB) {  // �޲δ�Ĭ�����ݿ�
		this.isMemDB = inisMemDB;
		createMemDB(dbpath);
	}
	
	FoxMemDB(boolean inisMemDB, String fileDBPath) { // �вδ�ָ�����ݿ��ļ�
		this.isMemDB = inisMemDB;
		createMemDB(fileDBPath);
	}

	public SQLiteDatabase getDB() {
		return db;
	}
	
	public void closeMemDB() {
		vacuumMemDB();
		if ( this.isMemDB ) {
			FoxMemDBBackupAndRestore(db, dbpath, true); // ���ɵ����ݿⱣ�浽������
		}
		db.close();
	}

	public String switchMemDB() { // �л����ݿ�·��
		String oldDBPath = dbpath;
		String rootDir = Environment.getExternalStorageDirectory().getPath() + File.separator ;
		ArrayList<String> dbList = getDBList(rootDir);
		
        int countDBs = dbList.size();
        ++nowDBnum;
        if ( nowDBnum >= countDBs ) {
            nowDBnum = 0 ;
        }
        dbpath = dbList.get(nowDBnum) ;
        
        vacuumMemDB();
		if ( this.isMemDB ) {
        	FoxMemDBBackupAndRestore(db, oldDBPath, true); // ���ɵ����ݿⱣ�浽������
		}
        db.close();
        
        createMemDB(dbpath); // �����µ�
        return dbpath;
	}

	public void execSQL(String inSQL) {
		db.execSQL(inSQL);
	}
	
	public String getOneCell(String inSQL) { // ��ȡһ��cell
		String outStr = "";
		Cursor cursor = db.rawQuery(inSQL, null);
		if (cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				outStr = cursor.getString(0);
				if ( null == outStr ) { outStr = ""; }
			} while (cursor.moveToNext());
		}
		cursor.close();
		return outStr;
	}

	
	// �÷���ʹ����ע��key�Ĵ�Сд�����߿�����SQL��ʹ�� as ��������ʽ
	public HashMap<String, String> getOneRow(String inSQL) { // ��ȡһ��
		HashMap<String, String> ree = new HashMap<String, String>();
		Cursor cursor = db.rawQuery(inSQL, null);
		String nowValue = "";
		if ( cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					nowValue = cursor.getString(i) ;
					if ( null == nowValue ) {
						ree.put(cursor.getColumnName(i), "");
					} else {
						ree.put(cursor.getColumnName(i), nowValue);
					}
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return ree;
	}
	
	// ���ۣ�3.x ���� getType
	/*
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public ArrayList<HashMap<String, String>> getTable(String inSQL) { // ��ȡһ��
		ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>(200);
		
		Cursor cursor = db.rawQuery(inSQL, null);
		int nowColumnType = 0;
		String nowValue = "";
		int nowColumnCount = 0;
		if ( cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				HashMap<String, String> ree = new HashMap<String, String>();
				nowColumnCount = cursor.getColumnCount();
				for (int i = 0; i < nowColumnCount; i++) {
					nowColumnType = cursor.getType(i);
					switch (nowColumnType) {
						case Cursor.FIELD_TYPE_NULL:
							nowValue = "";
							break;
						case Cursor.FIELD_TYPE_INTEGER:
							nowValue = String.valueOf(cursor.getInt(i));
							break;
						case Cursor.FIELD_TYPE_FLOAT:
							nowValue = String.valueOf(cursor.getFloat(i));
							break;
						case Cursor.FIELD_TYPE_STRING:
							nowValue = cursor.getString(i) ;
							break;
						default:
							nowValue = "";
							break;
					}
					ree.put(cursor.getColumnName(i), nowValue);
				}
				data.add(ree);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}
	*/

	private ArrayList<String> getDBList(String DBDir) {
        ArrayList<String> retList = new ArrayList<String>(9); // ���9��·�����Ժ���԰����޸�
        retList.add(DBDir + "FoxBook.db3");

        File xx = new File(DBDir);
        File[] fff = xx.listFiles(new FileFilter() {
            public boolean accept(File ff) {
                if (ff.isFile()) {
                    if (ff.toString().endsWith(".db3")) {
                        if (ff.getName().equalsIgnoreCase("FoxBook.db3")) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        int fc = fff.length;
        for (int i = 0; i < fc; i++) {
            retList.add(fff[i].getAbsolutePath());
        }
        return retList;
    }
    
	private void vacuumMemDB() { // �ͷ����ݿ���пռ�
		db.execSQL("vacuum");
	}
	
	private void createMemDB(String fileDBPath) { // �������ݿⲢ�����ļ����ݿ�
		if ( this.isMemDB ) {
			db = SQLiteDatabase.create(null); // �����յ��ڴ����ݿ�
		} else {
			db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		}

		File fileDB = new File(fileDBPath);
		if ( fileDB.exists() ) { // ���ھ͵���
			if ( this.isMemDB ) {
				FoxMemDBBackupAndRestore(db, fileDBPath, false) ; // File -> Mem
			}
		} else { // �����ھʹ�����ṹ
			db.execSQL("CREATE TABLE Book (ID integer primary key, Name Text, URL text, DelURL text, DisOrder integer, isEnd integer, QiDianID text, LastModified text);");
			db.execSQL("CREATE TABLE config (ID integer primary key, Site text, ListRangeRE text, ListDelStrList text, PageRangeRE text, PageDelStrList text, cookie text);");
			db.execSQL("CREATE TABLE Page (ID integer primary key, BookID integer, Name text, URL text, CharCount integer, Content text, DisOrder integer, DownTime integer, Mark text);");
		}
	}
	
	
	private void FoxMemDBBackupAndRestore(SQLiteDatabase oDBMem, String DBPath, boolean isBackupMemDBToFileDB) { // �ڴ����ݿ⵼�뵼��
		String FromDB = "main" ;
		String ToDB = "FoxAttach" ;
		File fileDB = new File(DBPath);
		if ( isBackupMemDBToFileDB ) { // Mem -> File
			FromDB = "main" ;
			ToDB = "FoxAttach" ;
			
			if ( fileDB.exists() ) { // ���ݿ�.db3����Ϊ .db3.old
				File oldFileDB = new File(DBPath + ".old");
				if (oldFileDB.exists()) {
					oldFileDB.delete();
				}
				fileDB.renameTo(oldFileDB);
			}
		} else { // File -> Mem
			FromDB = "FoxAttach" ;
			ToDB = "main" ;
		}
		
		oDBMem.execSQL("Attach '" + DBPath + "' as FoxAttach");
		oDBMem.execSQL("drop table if exists main.android_metadata; drop table if exists FoxAttach.android_metadata;"); // �ñ�Ĵ��ڻᵼ���쳣���ñ��Ѵ���
		String newSQL = "";
		String tbName = "";
		String oldSQL = "";
		try {
			Cursor cursor = oDBMem.rawQuery("select tbl_name,sql from " + FromDB + ".sqlite_master", null);
			if (cursor.moveToFirst()) {
				do {
					tbName = cursor.getString(0); // ����
					oldSQL = cursor.getString(1); // SQL : create table ...
					Matcher mat = Pattern.compile("(?i)^.*" + tbName + "(.*)$").matcher(oldSQL);
					while (mat.find()) {
						newSQL = "create table " + ToDB + "." + tbName + " " + mat.group(1);
					}
					oDBMem.execSQL(newSQL); //����ԭ���ݿ��񴴽��±�
					oDBMem.execSQL("insert into " + ToDB + "." + tbName + " select * from " + FromDB + "." + tbName ) ; //����ԭ�����ݵ��±�
				} while (cursor.moveToNext());
			}
			cursor.close();
		} catch (Exception e) {
			e.toString();
		}
		oDBMem.execSQL("Detach FoxAttach"); //���������ݿ�
	}

}

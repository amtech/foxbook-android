package com.linpinger.foxbook;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class FoxMemDB {
	private Context cc;
	private File fDB ;
	private int nowDBnum = 0 ;
	
	private boolean isMemDB = true ;  // �Ƿ����ڴ����ݿ�
	private boolean isIntDB = true ;  // �Ƿ����ڲ��洢���ݿ⣬��������/dataĿ¼��
	private SQLiteDatabase db ;
	
	FoxMemDB(boolean inisMemDB, boolean inisIntDB, Context ct) {  // �޲δ�Ĭ�����ݿ�
		this.cc = ct;
		this.isMemDB = inisMemDB;
		this.isIntDB = inisIntDB;
		if (isIntDB) {
			this.fDB = cc.getFileStreamPath("FoxBook.db3");
		} else {
			this.fDB = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "FoxBook.db3");
		}
		createMemDB(fDB);
	}
	FoxMemDB(File DBFile, Context ct) { // ��DB3/���txt
		this.cc = ct;
		this.isMemDB = true ;
		this.isIntDB = false;
		this.fDB = DBFile;
		createMemDB(fDB);
	}

	public SQLiteDatabase getDB() {
		return db;
	}
	
	public void closeMemDB() {
		vacuumMemDB();
		if ( this.isMemDB ) {
			FoxMemDBBackupAndRestore(db, fDB, true); // ���ɵ����ݿⱣ�浽������
		}
		db.close();
	}

	public File switchMemDB() { // �л����ݿ�·��
		File oldDBPath = fDB;
		ArrayList<File> dbList ;
		if ( this.isIntDB ) {
			dbList = getDBList(cc.getFilesDir());
		} else {
			dbList = getDBList(Environment.getExternalStorageDirectory());
		}
		
        int countDBs = dbList.size();
        ++nowDBnum;
        if ( nowDBnum >= countDBs ) {
            nowDBnum = 0 ;
        }
        fDB = dbList.get(nowDBnum) ;
        
        vacuumMemDB();
		if ( this.isMemDB ) {
        	FoxMemDBBackupAndRestore(db, oldDBPath, true); // ���ɵ����ݿⱣ�浽������
		}
        db.close();
        
        createMemDB(fDB); // �����µ�
        return fDB;
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

// ToDo ��table�Ļ�ȡ
/*
	public List<Map<String, Object>> getTable(String inSQL) { // ��ȡҳ���б�
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		Map<String, Object> item;
		Cursor cursor = db.rawQuery(inSQL, null);
		int colCount = cursor.getColumnCount();
		int i = 0 ;
		for ( i = 0; i < colCount; i++ ) {
			if ( FieldType.Null == cursor.getType(i) ) {
				
			}
		}		
		if (cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				for ( i = 0; i < colCount; i++ ) {
					cursor.getType(i);

				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}
*/
	private ArrayList<File> getDBList(File DBDir) {
        ArrayList<File> retList = new ArrayList<File>(9); // ���9��·�����Ժ���԰����޸�
        retList.add(new File(DBDir.getAbsolutePath() + File.separator + "FoxBook.db3"));

        File[] fff = DBDir.listFiles(new FileFilter() {
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
            retList.add(fff[i]);
        }
        return retList;
    }
    
	private void vacuumMemDB() { // �ͷ����ݿ���пռ�
		db.execSQL("vacuum");
	}
	
	private void createMemDB(File fileDB) { // �������ݿⲢ�����ļ����ݿ�
		if ( this.isMemDB ) {
			db = SQLiteDatabase.create(null); // �����յ��ڴ����ݿ�
		} else {
			db = SQLiteDatabase.openOrCreateDatabase(fDB, null);
		}

		if ( fileDB.exists() ) { // ���ھ͵���
			if ( this.isMemDB ) {
				FoxMemDBBackupAndRestore(db, fileDB, false) ; // File -> Mem
			}
		} else { // �����ھʹ�����ṹ
			db.execSQL("CREATE TABLE Book (ID integer primary key, Name Text, URL text, DelURL text, DisOrder integer, isEnd integer, QiDianID text, LastModified text);");
			db.execSQL("CREATE TABLE config (ID integer primary key, Site text, ListRangeRE text, ListDelStrList text, PageRangeRE text, PageDelStrList text, cookie text);");
			db.execSQL("CREATE TABLE Page (ID integer primary key, BookID integer, Name text, URL text, CharCount integer, Content text, DisOrder integer, DownTime integer, Mark text);");
		}
	}
	
	public void SD2Int(boolean isSD2Int) {
		String SDRoot = Environment.getExternalStorageDirectory().getPath() + File.separator ;
		ArrayList<File> dbListSD = getDBList(Environment.getExternalStorageDirectory());
		ArrayList<File> dbListInt = getDBList(cc.getFilesDir());
		
		if ( isSD2Int ) { // SD -> �ڲ�
			for (File f : dbListInt) {
				f.delete(); // ��ɾ��SD���ϵľ����ݿ�
			}
			for (File ff : dbListSD) {
				try {
					FileInputStream fosfrom = new FileInputStream(ff);

					FileOutputStream fosto = cc.openFileOutput(ff.getName(), Context.MODE_PRIVATE);
		            byte bt[] = new byte[1024];
		            int c;
		            while ((c = fosfrom.read(bt)) > 0) {
		                fosto.write(bt, 0, c);
		            }
		            fosfrom.close();
		            fosto.close();
				} catch (Exception e) {
					e.toString();
				}
			}
		} else { // �ڲ� -> SD
			for (File f : dbListSD) { // �ȱ���SD���ϵľ����ݿ�
				File oldFileDB = new File(f.getAbsolutePath() + ".old");
				if (oldFileDB.exists()) {
					oldFileDB.delete();
				}
				f.renameTo(oldFileDB);
			}
			for (File ff : dbListInt) {
				try {
					FileInputStream fosfrom = cc.openFileInput(ff.getName());

					FileOutputStream fosto = new FileOutputStream(SDRoot + ff.getName());
		            byte bt[] = new byte[1024];
		            int c;
		            while ((c = fosfrom.read(bt)) > 0) {
		                fosto.write(bt, 0, c);
		            }
		            fosfrom.close();
		            fosto.close();
		            
				} catch (Exception e) {
					e.toString();
				}
			}
		}
		

	}
	
	private void FoxMemDBBackupAndRestore(SQLiteDatabase oDBMem, File fileDB, boolean isBackupMemDBToFileDB) { // �ڴ����ݿ⵼�뵼��
		String FromDB = "main" ;
		String ToDB = "FoxAttach" ;
		if ( isBackupMemDBToFileDB ) { // Mem -> File
			FromDB = "main" ;
			ToDB = "FoxAttach" ;
			
			if ( fileDB.exists() ) { // ���ݿ�.db3����Ϊ .db3.old
				if (this.isIntDB) {
					fileDB.delete();
				} else {
					File oldFileDB = new File(fileDB.getAbsolutePath() + ".old");
					if (oldFileDB.exists()) {
						oldFileDB.delete();
					}
					fileDB.renameTo(oldFileDB);
				}
			}
		} else { // File -> Mem
			FromDB = "FoxAttach" ;
			ToDB = "main" ;
		}
		
		oDBMem.execSQL("Attach '" + fileDB.getAbsolutePath() + "' as FoxAttach");
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
						newSQL = "create table " + ToDB + "." + tbName + mat.group(1);
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

}

package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class FoxDB {
	// private static String dbpath = "/sdcard/FoxBook.db3";
	private static String dbpath = Environment.getExternalStorageDirectory()
			.getPath() + File.separator + "FoxBook.db3";
	private static int dbNum = 1 ;

	public static List<Map<String, Object>> getUMDArray(){
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		Cursor cursor = db.rawQuery("select page.name, page.content, book.name, book.id from book,page where book.id = page.bookid and page.content is not null order by page.bookid,page.id", null);
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(200);
		Map<String, Object> item;
		long preBookID = 0 ;
		long nowBookID = 0 ;
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				nowBookID = cursor.getLong(3) ;
				if ( preBookID != nowBookID ) { // ����ID���ϴβ�ͬ��˵������Ŀ�ͷ
					item.put("title", cursor.getString(2) + ":" + cursor.getString(0));
					preBookID = nowBookID ;
				} else {
					item.put("title", cursor.getString(0));
				}
				item.put("content", "����" + cursor.getString(1).replace("\n", "\n����"));
				data.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return data;
	}
	
	public static void deleteBook(int bookid) { // ɾ��һ����
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		db.execSQL("Delete From Page where BookID = " + bookid);
		db.execSQL("Delete From Book where ID = " + bookid);
		db.close();
	}
	
	public static int insertbook(String bookname, String bookurl) { // ����һ�����飬������bookid
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		ContentValues xxx = new ContentValues();
		xxx.put("Name", bookname);
		xxx.put("URL", bookurl);
		long bookid = db.insert("book", null, xxx);
		db.close();
		
		if ( -1 == bookid ){
			return 0;
		} else {
			return (int)bookid;
		}
	}
	
	//"select book.ID from Book left join page on book.id=page.bookid group by book.id order by count(page.id),book.isEnd,book.ID"
	public static void regenID(int sortmode) { // ��������bookid ,pageid
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);

		String sSQL = "";
		switch ( sortmode )
		{
		case 1: // �鼮ҳ��˳��
			sSQL = "select book.ID from Book left join page on book.id=page.bookid group by book.id order by count(page.id),book.isEnd,book.ID" ;
			break;
		case 2: // �鼮ҳ������
			sSQL = "select book.ID from Book left join page on book.id=page.bookid group by book.id order by count(page.id) desc,book.isEnd,book.ID" ;
			break;
		case 9:  // ����bookid��������pageid
			sSQL = "select id from page order by bookid,id";
			break;
		}
		
		int nStartID = 99999 ;
		if ( 9 == sortmode ) {
			nStartID = 5 + Integer.valueOf(FoxDB.getOneCell("select max(id) from page", db));
		} else {
			nStartID = 5 + Integer.valueOf(FoxDB.getOneCell("select max(id) from book", db));
		}
		int nStartID1 = nStartID;
		int nStartID2 = nStartID;
		
		// ��ȡid�б�������
		Cursor cursor = db.rawQuery(sSQL, null);
		int nRow = cursor.getCount();
		int [] ids = new int[nRow];
		int i = 0 ;
		if (cursor.moveToFirst()) {
			do {
				ids[i] = cursor.getInt(0) ;
				++ i;
			} while (cursor.moveToNext());
		}
		cursor.close();
		
		db.beginTransaction();// ��������
		try {
			for (i=0; i<nRow; i++) {
				++nStartID1;
				if ( 9 == sortmode ) {
					db.execSQL("update page set id=" + nStartID1 + " where id=" + ids[i]);
				} else {
					db.execSQL("update page set bookid=" + nStartID1 + " where bookid=" + ids[i]);
					db.execSQL("update book set id=" + nStartID1 + " where id=" + ids[i]);
				}
			}
			db.setTransactionSuccessful(); 
		} finally {
			db.endTransaction(); 
		}
		
		db.beginTransaction();
		try {
			for (i=1; i<=nRow; i++) {
				++nStartID2;
				if ( 9 == sortmode ) {
					db.execSQL("update page set id=" + i + " where id=" + nStartID2);
				} else {
					db.execSQL("update page set bookid=" + i + " where bookid=" + nStartID2);
					db.execSQL("update book set id=" + i + " where id=" + nStartID2);
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction(); 
		}
		if ( 9 != sortmode ) {
			db.execSQL("update Book set Disorder=ID");
		}
		db.close();
	}
	
	public static List<Map<String, Object>> getBookList() { // ��ȡ�鼮�б�
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
//		String sql = "select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by count Desc";
		String sql ="select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by book.DisOrder";
		Cursor cursor = db.rawQuery(sql, null);

		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(25);
		Map<String, Object> item;
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				item.put("name", cursor.getString(0));
				item.put("count", String.valueOf(cursor.getInt(1)));
				item.put("id", cursor.getInt(2));
				item.put("url", cursor.getString(3));
				item.put("isend", cursor.getInt(4));
				data.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return data;
	}

	public static List<Map<String, Object>> getPageList(String sqlWhereStr) { // ��ȡҳ���б�
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
		String sql = "select name, ID, URL,Bookid from page " + sqlWhereStr ;
		Cursor cursor = db.rawQuery(sql, null);

		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(25);
		Map<String, Object> item;
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				item.put("name", cursor.getString(0));
				item.put("id", cursor.getInt(1));
				item.put("url", cursor.getString(2));
				item.put("bookid", cursor.getInt(3));
				data.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return data;
	}

	public static List<Map<String, Object>> getBookNewPages(int bookid) { // ��ȡ���ݿ����������½�
		List<Map<String, Object>> xx = new ArrayList<Map<String, Object>>(10);
		Map<String, Object> item;

		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
		Cursor cursor = db.rawQuery("select id,url from page where ( bookid="
				+ String.valueOf(bookid)
				+ " ) and ( (content isnull) or ( length(content) < 5 ) )",
				null);
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				item.put("id", cursor.getInt(0));
				item.put("url", cursor.getString(1));
				// Log.e("FoxDB2", "new : " + cursor.getString(1));
				xx.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();

		return xx;
	}
	
	
	public static synchronized void inserNewPages(ArrayList<HashMap<String, Object>> data,
			int bookid) { // �����½ڵ����ݿ�
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);

		db.beginTransaction();// ��������
		try {
			String sbookid = String.valueOf(bookid);
			Iterator<HashMap<String, Object>> itr = data.iterator();
			HashMap<String, Object> mm;
			String nowName, nowURL;
			while (itr.hasNext()) {
				mm = (HashMap<String, Object>) itr.next();
				nowName = (String) mm.get("name");
				nowURL = (String) mm.get("url");
				// Log.e("FoxDB1", "new : " + nowURL);
				db.execSQL("insert into page(bookid,url,name) values(?,?,?)",
						new Object[] { sbookid, nowURL, nowName });
			}
			db.setTransactionSuccessful();// ��������ı�־ΪTrue
		} finally {
			db.endTransaction();// ��������,�����������commit,rollback,
			// ������ύ��ع���������ı�־������,�������ı�־ΪTrue������ͻ��ύ�����ع�,Ĭ�����������ı�־ΪFalse
		}
		// Log.e("FoxDB1", "close db");
		db.close();
	}

	public static void delete_nowupdown_Pages(int pageid, boolean bLE, boolean bUpdateDelList) { // ɾ��ĳ�½������½�
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		int bookid = Integer.valueOf(getOneCell("select bookid from page where id=" + pageid, db));
		if (bUpdateDelList) { // �޸� DelURL
			String oldDelStr = getOneCell("select DelURL from book where id = " + bookid, db);
			String newDelStr = "";
			if ( bLE ) {
				newDelStr = getPageListStr_notDel(" where bookid=" + bookid + " and id <= " + pageid, db);
			} else {
				newDelStr = getPageListStr_notDel(" where bookid=" + bookid + " and id >= " + pageid, db);
			}
			ContentValues args = new ContentValues();
			args.put("DelURL", oldDelStr.replace("\n\n", "\n") + newDelStr);
			db.update("book", args, "id=" + bookid, null);
		}
		if ( bLE ) {
			db.execSQL("Delete From Page where bookid = " + bookid + " and ID <= " + pageid);
		} else {
			db.execSQL("Delete From Page where bookid = " + bookid + " and ID >= " + pageid);
		}
		db.close();
	}
	
	public static void delete_Pages(int[] pageidlist, boolean bUpdateDelList) { // ɾ��ѡ���½�
		for (int i = 0; i < pageidlist.length; i++) { // ѭ��pageid
			delete_Pages(pageidlist[i], bUpdateDelList);
		}
	}

	public static void delete_Pages(int pageid, boolean bUpdateDelList) { // ɾ�����½�
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		if (bUpdateDelList) { // �޸� DelURL
			Map<String, String> xx = getOneRow("select book.DelURL as old, page.bookid as bid, page.url as url, page.name as name from book,page where page.id=" + pageid + " and book.id = page.bookid", db) ;
			ContentValues args = new ContentValues();
			args.put("DelURL", xx.get("old").replace("\n\n", "\n") + xx.get("url") + "|" + xx.get("name") + "\n");
			db.update("book", args, "id=" + xx.get("bid"), null);
		}
		db.execSQL("Delete From Page where ID = " + pageid);
		db.close();
	}
	
	public static void update_cell(String tbn, ContentValues cv, String argx) { // �޸ĵ����ֶ�
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		db.update(tbn, cv, argx, null);
		db.close();
	}

	public static void delete_Book_All_Pages(int bookid, boolean bUpdateDelList) { // ���book���½��б�
		String sbookid = String.valueOf(bookid);
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
		if (bUpdateDelList) { // �޸� DelURL
			ContentValues args = new ContentValues();
			args.put("DelURL", getPageListStr(bookid, db));
			db.update("book", args, "id=" + sbookid, null);
		}
		db.execSQL("Delete From Page where BookID = " + sbookid);
		db.close();
	}

	public static synchronized void setPageContent(int pageid, String text) { // �޸�ָ���½ڵ�����
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
		ContentValues args = new ContentValues();
		args.put("CharCount", text.length());
		args.put("Mark", "text");
		args.put("Content", text);
		db.update("page", args, "id=" + String.valueOf(pageid), null);
		db.close();
	}

	public static String getPageListStr(int bookid) { // ��ȡbookid�е����� url,name List
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
		String dellist = getPageListStr(bookid, db);
		db.close();
		return dellist;
	}

	private static String getPageListStr(int bookid, SQLiteDatabase db) { // ˽��:  ��ȡ url,name �б�
		return getPageListStr_Del(bookid, db) + getPageListStr_notDel(bookid, db);
	}

	private static String getPageListStr_Del(int bookid, SQLiteDatabase db) { // ˽��:  ��ȡ ��ɾ�� url,name �б�
		String dellist = "";
		// ��ȡ��ɾ���б�
		Cursor cursor = db.rawQuery("select DelURL from book where id = "
				+ String.valueOf(bookid), null);
		if (cursor.moveToFirst()) {
			do {
				dellist = cursor.getString(0);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return dellist.replace("\n\n", "\n");
	}

	private static String getPageListStr_notDel(int bookid, SQLiteDatabase db) { // ˽��: ��ȡ δɾ��url,name�б�
		return getPageListStr_notDel("where bookid = " + String.valueOf(bookid), db);
	}
	
	private static String getPageListStr_notDel(String sqlWhereStr, SQLiteDatabase db) { // ˽��: ��ȡ δɾ��url,name�б�
		String addDelList = "";
		Cursor cursor = db.rawQuery("select url, name from page " + sqlWhereStr, null);
		if (cursor.moveToFirst()) {
			do {
				addDelList += cursor.getString(0) + "|" + cursor.getString(1) + "\n";
			} while (cursor.moveToNext());
		}
		cursor.close();
		return addDelList;
	}

	public static void createDBIfNotExist() { // ������ݿⲻ���ڣ�������ṹ
		File oDB = new File(dbpath);
		if ( ! oDB.exists() ) {
			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(oDB, null);
			db.execSQL("CREATE TABLE Book (ID integer primary key, Name Text, URL text, DelURL text, DisOrder integer, isEnd integer, QiDianID text, LastModified 'text');");
			db.execSQL("CREATE TABLE config (ID integer primary key, Site text, ListRangeRE text, ListDelStrList text, PageRangeRE text, PageDelStrList text, cookie text);");
			db.execSQL("CREATE TABLE Page (ID integer primary key, BookID integer, Name text, URL text, CharCount integer, Content text, DisOrder integer, DownTime integer, Mark 'text');");
			db.close();
		}
	}

	public static void vacuumDB() { // �ͷ����ݿ���пռ�
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
		db.execSQL("vacuum");
		db.close();
	}
	public static void switchDB() { // �л����ݿ�·��
		String rootDir = Environment.getExternalStorageDirectory().getPath() + File.separator ;
		switch (dbNum) {
		case 1:
			dbpath = rootDir + "FoxBook.db3.old";
			dbNum = 2;
			createDBIfNotExist();
			break;
		case 2:
			dbpath = rootDir + "FoxBook.db3";
			dbNum = 1;
			createDBIfNotExist();
			break;
		}
	}

	public static String getOneCell(String inSQL) { // ��ȡһ��cell
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				new File(dbpath), null);
		String xx = getOneCell(inSQL, db);
		db.close();
		return xx;
	}

	private static String getOneCell(String inSQL, SQLiteDatabase db) { // ��ȡһ��cell
		String outStr = "";
		Cursor cursor = db.rawQuery(inSQL, null);
		if (cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				outStr = cursor.getString(0);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return outStr;
	}

	// �÷���ʹ����ע��key�Ĵ�Сд�����߿�����SQL��ʹ�� as ��������ʽ
	public static Map<String, String> getOneRow(String inSQL) { // ��ȡһ��
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbpath), null);
		Map<String, String> ree = getOneRow(inSQL, db);
		db.close();
		return ree;
	}
	
	private static Map<String, String> getOneRow(String inSQL, SQLiteDatabase db) { // ��ȡһ��
		Map<String, String> ree = new HashMap<String, String>();
		Cursor cursor = db.rawQuery(inSQL, null);
		if ( cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					ree.put(cursor.getColumnName(i), cursor.getString(i));
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return ree;
	}

}

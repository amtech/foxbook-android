package com.linpinger.foxbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class FoxMemDBHelper {


	public static List<Map<String, Object>> getEbookChaters(boolean isHTMLOut, FoxMemDB db){
		Cursor cursor = db.getDB().rawQuery("select page.name, page.content, book.name, book.id from book,page where book.id = page.bookid and page.content is not null order by page.bookid,page.id", null);
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(200);
		Map<String, Object> item;
		long preBookID = 0 ;
		long nowBookID = 0 ;
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				nowBookID = cursor.getLong(3) ;
				if ( preBookID != nowBookID ) { // ����ID���ϴβ�ͬ��˵������Ŀ�ͷ
					item.put("title", "��" + cursor.getString(2) + "��" + cursor.getString(0));
					preBookID = nowBookID ;
				} else {
					item.put("title", cursor.getString(0));
				}
				if (isHTMLOut) {
					item.put("content", "\n����" + cursor.getString(1).replace("\n", "<br/>\n����"));
				} else {
					item.put("content", "����" + cursor.getString(1).replace("\n", "\n����"));
				}
				data.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}
	
	public static void deleteBook(int bookid, FoxMemDB db) { // ɾ��һ����
		db.execSQL("Delete From Page where BookID = " + bookid);
		db.execSQL("Delete From Book where ID = " + bookid);
	}
	
	public static int insertbook(String bookname, String bookurl, FoxMemDB db) { // ����һ�����飬������bookid
		ContentValues xxx = new ContentValues();
		xxx.put("Name", bookname);
		xxx.put("URL", bookurl);
		long bookid = db.getDB().insert("book", null, xxx);
		
		if ( -1 == bookid ){
			return 0;
		} else {
			return (int)bookid;
		}
	}
	
	public static void regenID(int sortmode, FoxMemDB oDB) { // ��������bookid ,pageid
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
			nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from page"));
		} else {
			nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from book"));
		}
		int nStartID1 = nStartID;
		int nStartID2 = nStartID;
		
		// ��ȡid�б�������
		SQLiteDatabase db = oDB.getDB();
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
	}
	
	public static List<Map<String, Object>> getBookList(FoxMemDB db) { // ��ȡ�鼮�б�
//		String sql = "select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by count Desc";
		String sql ="select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by book.DisOrder";
		Cursor cursor = db.getDB().rawQuery(sql, null);

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
		return data;
	}

	public static List<Map<String, Object>> getPageList(String sqlWhereStr, FoxMemDB db) { // ��ȡҳ���б�
		String sql = "select name, ID, URL,Bookid from page " + sqlWhereStr ;
		Cursor cursor = db.getDB().rawQuery(sql, null);

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
		return data;
	}

	public static List<Map<String, Object>> getBookNewPages(int bookid, FoxMemDB db) { // ��ȡ���ݿ����������½�
		List<Map<String, Object>> xx = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> item;

		String bookurl = db.getOneCell("select url from book where id = " + bookid);
		try {
			Cursor cursor = db.getDB().rawQuery("select id,url from page where ( bookid="
				+ String.valueOf(bookid)
				+ " ) and ( (content is null) or ( length(content) < 9 ) )",
				null);
			if (cursor.moveToFirst()) {
				do {
					item = new HashMap<String, Object>();
					item.put("id", cursor.getInt(0));
					item.put("url", FoxBookLib.getFullURL(bookurl,cursor.getString(1)));
					xx.add(item);
				} while (cursor.moveToNext());
			}
			cursor.close();
		} catch (Exception e) {
			e.toString();
		}
		return xx;
	}
		
	public static synchronized void inserNewPages(ArrayList<HashMap<String, Object>> data, int bookid, FoxMemDB oDB) { // �����½ڵ����ݿ�
		SQLiteDatabase db = oDB.getDB();
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
	}

	public static void delete_nowupdown_Pages(int pageid, boolean bLE, boolean bUpdateDelList, FoxMemDB oDB) { // ɾ��ĳ�½������½�
		SQLiteDatabase db = oDB.getDB();
		int bookid = Integer.valueOf(oDB.getOneCell("select bookid from page where id=" + pageid));
		if (bUpdateDelList) { // �޸� DelURL
			String oldDelStr = oDB.getOneCell("select DelURL from book where id = " + bookid);
			String newDelStr = "";
			if ( bLE ) {
				newDelStr = getPageListStr_notDel(" where bookid=" + bookid + " and id <= " + pageid, oDB);
			} else {
				newDelStr = getPageListStr_notDel(" where bookid=" + bookid + " and id >= " + pageid, oDB);
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
	}
	
	public static void delete_Pages(int[] pageidlist, boolean bUpdateDelList, FoxMemDB db) { // ɾ��ѡ���½�
		for (int i = 0; i < pageidlist.length; i++) { // ѭ��pageid
			delete_Pages(pageidlist[i], bUpdateDelList, db);
		}
	}

	public static void delete_Pages(int pageid, boolean bUpdateDelList, FoxMemDB db) { // ɾ�����½�
		if (bUpdateDelList) { // �޸� DelURL
			Map<String, String> xx = db.getOneRow("select book.DelURL as old, page.bookid as bid, page.url as url, page.name as name from book,page where page.id=" + pageid + " and book.id = page.bookid") ;
			ContentValues args = new ContentValues();
			args.put("DelURL", xx.get("old").replace("\n\n", "\n") + xx.get("url") + "|" + xx.get("name") + "\n");
			db.getDB().update("book", args, "id=" + xx.get("bid"), null);
		}
		db.execSQL("Delete From Page where ID = " + pageid);
	}
	
	public static void update_cell(String tbn, ContentValues cv, String argx, FoxMemDB db) { // �޸ĵ����ֶ�
		db.getDB().update(tbn, cv, argx, null);
	}

	public static void delete_Book_All_Pages(int bookid, boolean bUpdateDelList, FoxMemDB db) { // ���book���½��б�
		String sbookid = String.valueOf(bookid);
		if (bUpdateDelList) { // �޸� DelURL
			ContentValues args = new ContentValues();
			args.put("DelURL", getPageListStr(bookid, db));
			db.getDB().update("book", args, "id=" + sbookid, null);
		}
		db.execSQL("Delete From Page where BookID = " + sbookid);
	}

	public static synchronized void setPageContent(int pageid, String text, FoxMemDB db) { // �޸�ָ���½ڵ�����
		String aNow = (new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(new java.util.Date()) ;
		ContentValues args = new ContentValues();
		args.put("CharCount", text.length());
		args.put("Mark", "text");
		args.put("Content", text);
		args.put("DownTime", aNow);
		db.getDB().update("page", args, "id=" + String.valueOf(pageid), null);
	}

	public static String getPageListStr(int bookid, FoxMemDB db) { // ��ȡ url,name �б�
		return db.getOneCell("select DelURL from book where id = " + bookid).replace("\n\n", "\n") + getPageListStr_notDel("where bookid = " + bookid, db);
	}

	private static String getPageListStr_notDel(String sqlWhereStr, FoxMemDB db) { // ˽��: ��ȡ δɾ��url,name�б�
		String addDelList = "";
		Cursor cursor = db.getDB().rawQuery("select url, name from page " + sqlWhereStr, null);
		if (cursor.moveToFirst()) {
			do {
				addDelList += cursor.getString(0) + "|" + cursor.getString(1) + "\n";
			} while (cursor.moveToNext());
		}
		cursor.close();
		return addDelList;
	}


 
}

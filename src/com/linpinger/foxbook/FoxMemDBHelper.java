package com.linpinger.foxbook;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ray.tools.umd.builder.Umd;
import com.ray.tools.umd.builder.UmdChapters;
import com.ray.tools.umd.builder.UmdHeader;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class FoxMemDBHelper {

	public static String importQidianEpub(FoxEpubReader epub, FoxMemDB oDB) { // �������epub
		HashMap<String, Object> qhm = epub.getQiDianEpubInfo();
		String qidianid = qhm.get("qidianid").toString();
		String sBookid = String.valueOf(FoxMemDBHelper.insertbook(qhm.get("bookname").toString()
				, site_qidian.qidian_getIndexURL_Mobile(Integer.valueOf(qidianid)), qidianid, oDB));

		// �������ݵ�RamDB
		String fileName ;
		String pageTitle ;
		String pageText ;
		SQLiteDatabase db = oDB.getDB();
		db.beginTransaction();// ��������
		for ( HashMap<String, Object> hm : epub.getQiDianEpubTOC() ) {
			fileName = hm.get("name").toString();
			pageTitle = hm.get("title").toString();
			pageText = epub.getQiDianEpubPage(fileName);
			db.execSQL("insert into page(bookid,name, url,content,CharCount) values(?,?,?,?,?)",
					new Object[] { sBookid, pageTitle
					, site_qidian.qidian_getPageURL(Integer.valueOf(hm.get("pageid").toString()), Integer.valueOf(hm.get("bookid").toString()))
					, pageText, String.valueOf(pageText.length()) });
		}
		db.setTransactionSuccessful();// ��������ı�־ΪTrue
		db.endTransaction();
		return qhm.get("bookname").toString();
	}
	public static String importQidianTxt(String txtPath, FoxMemDB oDB) {
		return importQidianTxt(txtPath, oDB, true); // ʹ���·�ʽ������ 
	}
    public static String importQidianTxt(String txtPath, FoxMemDB oDB, boolean isNew) {
    	// boolean isNew = true;
    	//long sTime = System.currentTimeMillis();
        // ��һ�������룬��GBK����UTF-8���������迼��
        String txtEnCoding = ToolJava.detectTxtEncoding(txtPath) ; // �²������ı����� ����: "GBK" �� "UTF-8"
        String txt = ToolJava.readText(txtPath, txtEnCoding).replace("\r", "").replace("��", ""); // Ϊ���txtԤ����

        if ( ! txt.contains("����ʱ��") ) // ������ı�
			return importNormalTxt(txtPath, oDB, txtEnCoding);

        SQLiteDatabase db = oDB.getDB();
        String sQidianid = (new File(txtPath)).getName().replace(".txt", ""); // �ļ���
        String sQidianURL = site_qidian.qidian_getIndexURL_Mobile(Integer.valueOf(sQidianid)); // URL
        String sBookName = sQidianid;

        // �°�Ҫ��ܶ࣬��������ͷ���������½�
        // ��������Ժ����txt�нṹ�䶯�Ļ�����Ӧ�Կ��ܲ���ɰ棬�ʱ����ɰ�
        if ( isNew ) { // �°棬������bug
    		String line[] = txt.split("\n");
    		int lineCount = line.length;
    		sBookName = line[0] ;
    		String sBookid = String.valueOf(insertbook(sBookName, sQidianURL, sQidianid, oDB)); // �����鼮 �� ��ȡid
    		int titleNum = 0 ; // base:0 ����
    		int headNum = 0 ; // base:0  ����
			int lastEndNum = 0 ;
    		db.beginTransaction();// ��������
    		for ( int i=3; i<lineCount; i++) { // �ӵ����п�ʼ
    			if (line[i].startsWith("����ʱ��")) { // ��һ��Ϊ������
    				titleNum = i - 1 ;
    				headNum = i + 2 ;
    			} else { // �Ǳ�����
    				if ( line[i].startsWith("<a href=") ) {
						if ( i - lastEndNum < 5 ) // ��Щtxt�½�β�����������ӣ�����
							continue;
    					// �������ȡ�����У�������
    					// System.out.println(titleNum + " : " + headNum + " - " + i );
    					StringBuilder sbd = new StringBuilder();
    					for ( int j=headNum; j<i; j++)
    						sbd.append(line[j]).append("\n");
    					sbd.append("\n");
    					db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
            					new Object[] { sBookid, line[titleNum], sbd.toString(), String.valueOf(sbd.length()) });
						lastEndNum = i;
    				}
    			}
    		}
    		db.setTransactionSuccessful();// ��������ı�־ΪTrue
    		db.endTransaction();
        } else { // �ɰ棬��΢��һ��
        	try {  // ��һ������
        		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath), "GBK"));
        		sBookName =  br.readLine() ;
        		br.close();
        	} catch (Exception e) {
        		e.toString();
        	}
        	String sBookid = String.valueOf(insertbook(sBookName, sQidianURL, sQidianid, oDB)); // �����鼮 �� ��ȡid

        	String txtContent = site_qidian.qidian_getTextFromPageJS(txt) + "\n<end>\n" ;
        	db.beginTransaction();// ��������
        	try {
        		Matcher mat = Pattern.compile("(?mi)^([^\\r\\n]+)[\\r\\n]{1,2}����ʱ��.*$[\\r\\n]{2,4}([^\\a]+?)(?=(^([^\\r\\n]+)[\\r\\n]{1,2}����ʱ��)|^<end>$)").matcher(txtContent);
        		while (mat.find()) {
        			db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
        					new Object[] { sBookid, mat.group(1), mat.group(2).replace("\n\n", "\n"), String.valueOf(mat.group(2).length()) });
        		}
        		db.setTransactionSuccessful();// ��������ı�־ΪTrue
        	} finally {
        		db.endTransaction();// ��������,�����������commit,rollback,// ������ύ��ع���������ı�־������,�������ı�־ΪTrue������ͻ��ύ�����ع�,Ĭ�����������ı�־ΪFalse
        	}
        }
        // Log.e("XX", "��ʱ: " + (System.currentTimeMillis() - sTime));
        return sBookName;
    }

	private static String importNormalTxt(String txtPath, FoxMemDB oDB, String txtEnCoding) {
//        String txtEnCoding = ToolBookJava.detectTxtEncoding(txtPath) ; // �²������ı����� ����: "GBK" �� "UTF-8"
//        String txt = ToolBookJava.readText(txtPath, txtEnCoding) ;

        String fileName = (new File(txtPath)).getName().replace(".txt", ""); // �ļ���
        SQLiteDatabase db = oDB.getDB();
        String sBookid = String.valueOf(insertbook(fileName, "txt", "00", oDB)); // �����鼮 �� ��ȡid

        db.beginTransaction();// ��������
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath), txtEnCoding));
            StringBuilder chunkStr = new StringBuilder(65536);
            int chunkLen = 0;
            int chunkCount = 0;
            String line = null;
			int lineLen = 0;
            while ((line = br.readLine()) != null) {
                if ( line.startsWith("����") ) // ȥ����ͷ�Ŀհ�
                    line = line.replaceFirst("����*", "");

				lineLen = line.length() ;
                chunkLen = chunkStr.length();
                if ( chunkLen > 2200 && lineLen < 22 && ( line.startsWith("��") || line.contains("��") || line.contains("��") || line.contains("��") || line.contains("��") || line.contains("Ʒ") || lineLen > 2 ) ) {
                    ++ chunkCount;
                      db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
                          new Object[] { sBookid, txtEnCoding + "_" + String.valueOf(chunkCount), chunkStr.toString(), String.valueOf(chunkStr.length()) });
                    chunkStr = new StringBuilder(65536);
                 }
                 chunkStr.append(line).append("\n");
            }
            if ( chunkStr.length() > 0 ) {
                 ++ chunkCount;
                  db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
                          new Object[] { "1", txtEnCoding + "_" + String.valueOf(chunkCount), chunkStr.toString(), String.valueOf(chunkStr.length()) });
            }
            br.close();
            db.setTransactionSuccessful();// ��������ı�־ΪTrue
        } catch (IOException e) {
            e.toString();
         } finally {
            db.endTransaction();// ��������,�����������commit,rollback,  ������ύ��ع���������ı�־������,�������ı�־ΪTrue������ͻ��ύ�����ع�,Ĭ�����������ı�־ΪFalse
        }
        return fileName;
	}

    public static List<Map<String, Object>> getEbookChaters(boolean isHTMLOut, FoxMemDB db){
        return getEbookChaters(isHTMLOut, "all", db);
    }
    public static List<Map<String, Object>> getEbookChaters(boolean isHTMLOut, String iBookID, FoxMemDB db){
        String addSQL = "" ;
        if ( ! iBookID.equalsIgnoreCase("all") ) {
            addSQL = " and page.bookid = " + iBookID ;
        }
        Cursor cursor = db.getDB().rawQuery("select page.name, page.content, book.name, book.id from book,page where book.id = page.bookid and page.content is not null " + addSQL + " order by page.bookid,page.id", null);
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

    public static int insertbook(String bookname, String bookurl, String qidianid, FoxMemDB db) { // ����һ�����飬������bookid
        ContentValues xxx = new ContentValues();
        xxx.put("Name", bookname);
        xxx.put("URL", bookurl);
		if (qidianid != null) {
        	xxx.put("QiDianID", qidianid);
		}
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

        // ��ȡid�б�������
        SQLiteDatabase db = oDB.getDB();
        Cursor cursor = db.rawQuery(sSQL, null);
        int nRow = cursor.getCount();
        if ( nRow == 0 ) {
            return;
        }
        int [] ids = new int[nRow];
        int i = 0 ;
        if (cursor.moveToFirst()) {
            do {
                ids[i] = cursor.getInt(0) ;
                ++ i;
            } while (cursor.moveToNext());
        }
        cursor.close();

        // ���ID
        int nStartID = 99999 ;
        if ( 9 == sortmode ) {
            nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from page"));
        } else {
            nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from book"));
        }
        int nStartID1 = nStartID;
        int nStartID2 = nStartID;

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

    // ��ȡ�����µ����б�,�����ص�����Ԫ��: bookid, bookname, bookurl
    public static ArrayList<HashMap<String, Object>> compareShelfToGetNew(FoxMemDB db) {
        // ���������ҳ, ��Ҫ ��ܵ�ַ��cookie
        // ���������ҳ���ϳ� �õ� ���ַ, ����, ���½ڵ�ַ, ���½���
        // �����ݿ�õ� bookid, bookname, bookurl, pageUrlList
        // �Ƚ��鼮�� ���½ڵ�ַ�Ƿ��� pageUrlList �У���ͼ��뷵���б���
        ArrayList<HashMap<String, Object>> book = getBookListForShelf(db);

        String SiteURL = (String) book.get(0).get("url");
        int SiteType = 0 ;
        String cookieSQL = "";
        String urlShelf = "";
        String reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"([^\"]*)\"[^>]*>([^<]*)<";
        if ( SiteURL.contains(".13xs.com") ) {
            SiteType = 13 ;
            cookieSQL = ".13xs." ;
            urlShelf = "http://www.13xs.com/shujia.aspx";
            reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)&index.*?\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"[^\"]*cid=([0-9]*)\"[^>]*>([^<]*)<" ;
        }
        if ( SiteURL.contains(".biquge.com.tw") ) {
            SiteType = 2 ;
            cookieSQL = ".biquge." ;
            urlShelf = "http://www.biquge.com.tw/modules/article/bookcase.php";
            reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"([^\"]*)\"[^>]*>([^<]*)<";
        }
        if ( SiteURL.contains(".dajiadu.net") ) {
            SiteType = 4 ;
            cookieSQL = ".dajiadu." ;
            urlShelf = "http://www.dajiadu.net/modules/article/bookcase.php" ;
            reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)&index.*?\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"[^\"]*cid=([0-9]*)\"[^>]*>([^<]*)<" ;
        }
        if ( SiteURL.contains("m.qreader.me") ) {
            SiteType = 99 ;
            urlShelf = "http://m.qreader.me/update_books.php" ;
            reShelf = "(?smi)\"id\":([0-9]*),\"status\":([0-9]*).*?\"chapter_i\":([0-9]*),\"chapter_n\":\"([^\"]*)\"";
        }

        if ( 0 == SiteType ) {
            return null;
        }

        String html = "";
        if ( 99 == SiteType ) {
            Iterator<HashMap<String, Object>> itrQQ = book.iterator();
            String qindexURL ;
            String postData = "{\"books\":[" ;
            Pattern p = Pattern.compile("bid=([0-9]+)");
            while (itrQQ.hasNext()) {
                qindexURL = (String) ( (HashMap<String, Object>) itrQQ.next() ).get("url");
                Matcher m = p.matcher(qindexURL) ;
                while(m.find())
                    postData = postData + "{\"t\":0,\"i\":" + m.group(1) + "},";
            }
            if ( postData.endsWith(",") )
                postData = postData.substring(0, postData.length()-1) ;
            postData = postData + "]}";
            html = ToolBookJava.downhtml(urlShelf, "", postData);
        } else {
            String cookie = db.getOneCell("SELECT cookie from config where site like '%" + cookieSQL + "%' ") ;
            html = ToolBookJava.downhtml(urlShelf, "gbk", "GET", ToolBookJava.cookie2Field(cookie)) ;
        }
        if ( html.length() < 5 )
            return null ;

        HashMap<String, String> shelfBook = new HashMap<String, String>(30); // ���� -> ���½ڵ�ַ
        Matcher mat = Pattern.compile(reShelf).matcher(html);
        while (mat.find()) {
            switch (SiteType) {
            case 13:
                shelfBook.put(mat.group(2), mat.group(3) + ".html");
                break;
            case 2:
                shelfBook.put(mat.group(2), mat.group(3));
                break;
            case 4:
                shelfBook.put(mat.group(2), mat.group(3) + ".html");
                break;
            case 99: // BID -> ���½ڵ�ַ
                shelfBook.put("BID" + mat.group(1), "#" + mat.group(3));
                break;
            }
        }

        ArrayList<HashMap<String, Object>> newPages = new ArrayList<HashMap<String, Object>>(30);
        Iterator<HashMap<String, Object>> itr = book.iterator();
        HashMap<String, Object> mm;
        String nowName, nowURL, nowPageList;
        Pattern pp = Pattern.compile("bid=([0-9]+)");
        while (itr.hasNext()) {
            mm = (HashMap<String, Object>) itr.next();
//            nowBID =  String.valueOf((Integer)mm.get("id"));
            nowName = (String) mm.get("name");
            nowPageList = (String) mm.get("pagelist");
            if ( 99 == SiteType ) {
                nowURL = (String) mm.get("url");
                Matcher m = pp.matcher(nowURL) ;
                while(m.find()) {
                    if ( ! nowPageList.contains("\n" + (String)shelfBook.get("BID" + m.group(1)) + "|") ) {
                        newPages.add(mm);
                    }
                }
            } else {
                if ( ! nowPageList.contains("\n" + (String)shelfBook.get(nowName) + "|") ) {
                    newPages.add(mm);
                }
            }
        }
        return newPages;
    }

    public static ArrayList<HashMap<String, Object>> getBookListForShelf(FoxMemDB db) { // ��ȡ�Ƚ������Ҫ������
        ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>(30);
        String sql = "select id,name,url,DelURL from book where ( isEnd isnull or isEnd = '' or isEnd < 1 )" ;

        Cursor cursor = db.getDB().rawQuery(sql, null);
        HashMap<String, Object> item;
        if (cursor.moveToFirst()) {
            do {
                item = new HashMap<String, Object>();
                item.put("id", cursor.getInt(0));
                item.put("name", cursor.getString(1));
                item.put("url", cursor.getString(2));
                item.put("pagelist", (cursor.getString(3) + "\n" + getPageListStr_notDel("where bookid = " + cursor.getInt(0), db)).replace("\n\n", "\n"));
                data.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return data;
    }

    public static List<Map<String, Object>> getBookList(FoxMemDB db) { // ��ȡ�鼮�б�
//        String sql = "select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by count Desc";
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
		return getPageList(sqlWhereStr, 0, db) ;
	}
    public static List<Map<String, Object>> getPageList(String sqlWhereStr, int sMode, FoxMemDB db) { // ��ȡҳ���б�
        String sql = "select name, ID, URL,Bookid, length(content) from page " + sqlWhereStr ;
		if ( sMode == 26 ) { // ZIPר�ã���ʾ����
			sql = "select name, ID, URL,Bookid, CharCount from page " + sqlWhereStr ;
			sMode = 0;
		}
        Cursor cursor = db.getDB().rawQuery(sql, null);

		int lastBID = 0 ;
		int nowBID = 0 ;
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(25);
        Map<String, Object> item;
        if (cursor.moveToFirst()) {
            do {
                item = new HashMap<String, Object>();
				nowBID = cursor.getInt(3) ;
				if ( 0 == sMode ) {
                	item.put("name", cursor.getString(0));
				} else {
					if ( nowBID == lastBID ) {
                		item.put("name", cursor.getString(0));
					} else {
                		item.put("name", "��" + cursor.getString(0));
					}
				}
                item.put("id", cursor.getInt(1));
                item.put("url", cursor.getString(2));
                item.put("bookid", nowBID);
                item.put("count", cursor.getInt(4));
                data.add(item);
				lastBID = nowBID ;
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
                    item.put("url", ToolBookJava.getFullURL(bookurl,cursor.getString(1)));
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

    // �����������DelURL
    public static void simplifyAllDelList(FoxMemDB oDB) {
        SQLiteDatabase db = oDB.getDB();
        db.beginTransaction();// ��������
        try {
            Cursor cursor = db.rawQuery("select ID, DelURL from book where length(DelURL) > 128", null);
            if (cursor.moveToFirst()) {
                do {
                    db.execSQL("update Book set DelURL=? where id=" + String.valueOf(cursor.getInt(0)),
                            new Object[] { ToolBookJava.simplifyDelList(cursor.getString(1)) });
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.setTransactionSuccessful();// ��������ı�־ΪTrue
        } finally {
            db.endTransaction();
        }
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

    public static void updatepage(int pageid, FoxMemDB db) {
        Map<String, String> xx = db.getOneRow("select book.url as bu,page.url as pu from book,page where page.id=" + String.valueOf(pageid) + " and  book.id in (select bookid from page where id=" + String.valueOf(pageid) + ")");
        String fullPageURL = ToolBookJava.getFullURL(xx.get("bu"),xx.get("pu")); // ��ȡbookurl, pageurl �ϳɵõ�url

        updatepage(pageid, fullPageURL, db) ;
    }

    public static String updatepage(int pageid, String pageFullURL, FoxMemDB db) {
        String text = "";
        String html = "" ;
        int site_type = 0 ; // ����ҳ�洦�� 

        if ( pageFullURL.contains(".qidian.com") ) { site_type = 99 ; }
        if ( pageFullURL.contains("files.qidian.com") ) { site_type = 98; }   // ����ֻ�վֱ����txt��ַ����

        switch(site_type) {
            case 98:
                html = ToolBookJava.downhtml(pageFullURL, "GBK"); // ����json
                text = site_qidian.qidian_getTextFromPageJS(html);
                break;
            case 99:
                String nURL = site_qidian.qidian_toTxtURL_FromPageContent(ToolBookJava.downhtml(pageFullURL)) ; // 2015-11-17: ����ַ�䶯��ֻ��������ҳ���ٻ�ȡtxt��ַ
                if ( nURL.equalsIgnoreCase("") ) {
                    text = "" ;
                } else {
                    html = ToolBookJava.downhtml(nURL);
                    text = site_qidian.qidian_getTextFromPageJS(html);
                }
                break;
            default:
                html = ToolBookJava.downhtml(pageFullURL); // ����url
                text = ToolBookJava.pagetext(html);       // �����õ�text
        }

        if ( pageid > 0 ) { // ��pageidС��0ʱ��д�����ݿ⣬��Ҫ�������߲鿴
            FoxMemDBHelper.setPageContent(pageid, text,db); // д�����ݿ�
            return String.valueOf(0);
        } else {
            return text;
        }
    }

       public static String all2txt(FoxMemDB db) {
            return all2txt("all", db);
        }
        public static String all2txt(String iBookID, FoxMemDB db) { // �����鼮תΪtxt
            String txtPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.txt";
            return all2txt(iBookID, db, txtPath);
        }
        public static String all2txt(String iBookID, FoxMemDB db, String txtPath) { // �����鼮תΪtxt
            StringBuilder txt = new StringBuilder(81920);
            List<Map<String, Object>> data ;
            if ( iBookID.equalsIgnoreCase("all") ) {
                data = FoxMemDBHelper.getEbookChaters(false, db);
            } else {
                data = FoxMemDBHelper.getEbookChaters(false, iBookID, db);
            }
            Iterator<Map<String, Object>> itr = data.iterator();
            while (itr.hasNext()) {
                HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                txt.append(mm.get("title")).append("\n\n").append(mm.get("content")).append("\n\n\n");
            }

            ToolJava.writeText(txt.toString(), txtPath);
            return "/fox.txt"; // ��foxHTTPD������·��ʹ��
        }

        public static void all2epub(FoxMemDB db) {// �����鼮תΪepub
            FoxEpubWriter oEpub = new FoxEpubWriter(new File(Environment.getExternalStorageDirectory(), "fox.epub"));

            List<Map<String, Object>> data = FoxMemDBHelper.getEbookChaters(true, db);
            Iterator<Map<String, Object>> itr = data.iterator();
            HashMap<String, Object> mm;
            while (itr.hasNext()) {
                mm = (HashMap<String, Object>) itr.next();
                oEpub.addChapter((String)mm.get("title"), (String)mm.get("content"), -1);
            }
            oEpub.saveAll();
        }

        public static void all2umd(FoxMemDB db) { // �����鼮תΪumd
            String umdPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.umd";
            Umd umd = new Umd();

            UmdHeader uh = umd.getHeader(); // �����鼮��Ϣ
            uh.setTitle("FoxBook");
            uh.setAuthor("������֮��");
            uh.setBookType("С˵");
            uh.setYear("2014");
            uh.setMonth("04");
            uh.setDay("01");
            uh.setBookMan("������֮��");
            uh.setShopKeeper("������֮��");


            UmdChapters  cha = umd.getChapters(); // ��������
            List<Map<String, Object>> data = FoxMemDBHelper.getEbookChaters(false, db);
            Iterator<Map<String, Object>> itr = data.iterator();
            while (itr.hasNext()) {
                HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                cha.addChapter((String) mm.get("title"), (String) mm.get("content"));
            }

            File file = new File(umdPath); // ����
            try {
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    umd.buildUmd(bos);
                    bos.flush();
                 } finally {
                    fos.close();
                }
            } catch (Exception e) {
                e.toString();
            }
        }


}


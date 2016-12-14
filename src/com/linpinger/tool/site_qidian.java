package com.linpinger.tool;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class site_qidian {
    // 2015�������İ����ʧЧ
    public static String qidian_getIndexURL_Desk(int bookid) {
        return "http://read.qidian.com/BookReader/" + bookid + ".aspx";
    }
    
    // �ƶ���Ŀ¼��ַ: ����������ȡlastPageID��ĸ��£�Ϊ0��ȡ����
    public static String qidian_getIndexURL_Mobile(int bookid) {
        return "http://3g.if.qidian.com/Client/IGetBookInfo.aspx?version=2&BookId=" + bookid + "&ChapterId=0";
    }

    public static String qidian_getIndexURL_Mobile(int bookid, int lastpageid) {
        return "http://3g.if.qidian.com/Client/IGetBookInfo.aspx?version=2&BookId=" + bookid + "&ChapterId=" + lastpageid;
    }
  
    public static String qidian_getSearchURL_Mobile(String BookName) {
        String xx = "";
        try {
            xx = "http://3g.if.qidian.com/api/SearchBooksRmt.ashx?key=" + URLEncoder.encode(BookName, "UTF-8") + "&p=0";
        } catch (Exception e) {
			System.err.println(e.toString());
        }
        return xx;
    }
    
    public static List<Map<String, Object>> json2BookList(String json) {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(55);
        try {
            JSONArray slist = new JSONObject(json).getJSONObject("Data").getJSONArray("ListSearchBooks");
            int cList = slist.length();
            Map<String, Object> item;
            for (int i = 0; i < cList; i++) {
                item = new HashMap<String, Object>();
                item.put("name", slist.getJSONObject(i).getString("BookName"));
                item.put("url", qidian_getIndexURL_Mobile(slist.getJSONObject(i).getInt("BookId")));
                data.add(item);
            }
        } catch (Exception e) {
			System.err.println(e.toString());
        }
        return data;
    }
    
    public static List<Map<String, Object>> json2PageList(String json) {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(128);
        try {
            Integer bookID = new JSONObject(json).getInt("BookId");
            JSONArray slist = new JSONObject(json).getJSONArray("Chapters");
            int cList = slist.length();
            Map<String, Object> item;
            
            for (int i = 0; i < cList; i++) {
                item = new HashMap<String, Object>();
                item.put("name", slist.getJSONObject(i).getString("n"));
                item.put("url", qidian_getPageURL(slist.getJSONObject(i).getInt("c"), bookID));
                if (1 == slist.getJSONObject(i).getInt("v")) { // VIP�½�
                    break;
                }
                data.add(item);
            }
        } catch (Exception e) {
			System.err.println(e.toString());
        }
        return data;
    }
    
    /**
     *
     * @param iURL http://read.qidian.com/BookReader/3059077.aspx
     * @return ���BookID
     */
    public static int qidian_getBookID_FromURL(String iURL) {
        String sQDID = "";
        String RE = "" ;
        if ( iURL.contains("3g.if.qidian.com") ) {
            RE = "(?i)BookId=([0-9]+)&" ; // http://3g.if.qidian.com/Client/IGetBookInfo.aspx?version=2&BookId=3530623&ChapterId=0
        } else if (iURL.contains("m.qidian.com")) {
        	RE = "(?i)bookid=([0-9]+)" ;
        } else {
            RE = "(?i).*/([0-9]+)\\." ; // http://read.qidian.com/BookReader/3059077.aspx
        }
        Matcher m = Pattern.compile(RE).matcher(iURL);
        while (m.find()) {
            sQDID = m.group(1);
        }
        if ( null == sQDID | "" == sQDID ) {
        	return 0;
        } else { 
        	return Integer.valueOf(sQDID);
        }
    }
    
    /**
    *
    * @param pageid ���ҳ��id
    * @param bookid ����鼮id
    * @return http://files.qidian.com/Author7/1939238/53927617.txt
    */
    public static String qidian_getPageURL(String pageid, String bookid) {
    	return "http://files.qidian.com/Author" + ( 1 + ( Integer.valueOf(bookid) % 8 ) ) + "/" + bookid + "/" + pageid + ".txt";
    }
    public static String qidian_getPageURL(int pageid, int bookid) {
        return "http://files.qidian.com/Author" + (1 + (bookid % 8)) + "/" + bookid + "/" + pageid + ".txt";
    }
    
    /**
    *
    * @param pageInfoURL ���Ƶ�ַ /1939238,53927617.aspx
    * @return http://files.qidian.com/Author7/1939238/53927617.txt
	* ��ʱ����
    public static String qidian_toPageURL_FromPageInfoURL(String pageInfoURL)
    {
		Matcher mat = Pattern.compile("(?i)/([0-9]+),([0-9]+).aspx").matcher(pageInfoURL);
		String bid = "";
		String cid = "";
		while (mat.find()) {
			bid = mat.group(1);
			cid = mat.group(2);
		}
		if ( bid.equalsIgnoreCase("") ) {
			return "" ;
		} else {
			return qidian_getPageURL(cid, bid);
		}
    }
    */

    /**
    * ����: 2015-11-17
    * @param html ���� /b7zJ1_AnAJ41,Nw1qx8_dKSIex0RJOkJclQ2.aspx ����ҳ����
    * @return http://files.qidian.com/Author7/1939238/53927617.txt
     */
    public static String qidian_toTxtURL_FromPageContent(String html) {
        Matcher mat = Pattern.compile("(?i)(http://files.qidian.com/.*/[0-9]*/[0-9]*.txt)").matcher(html);
        String txtURL = "";
        while (mat.find()) {
            txtURL = mat.group(1);
        }
        if (txtURL.equalsIgnoreCase("")) {
            return "";
        } else {
            return txtURL;
        }
    }

    /**
    *
    * @param jsStr http://files.qidian.com/Author7/1939238/53927617.txt �е�����
    * @return �ı�����ֱ��д�����ݿ�
    */
    public static String qidian_getTextFromPageJS(String jsStr) {
        jsStr = jsStr.replace("&lt;", "<");
        jsStr = jsStr.replace("&gt;", ">");
        jsStr = jsStr.replace("document.write('", "");
        jsStr = jsStr.replace("<a>�ֻ��û��뵽m.qidian.com�Ķ���</a>", "");
        jsStr = jsStr.replace("<a href=http://www.qidian.com>���������www.qidian.com��ӭ������ѹ����Ķ������¡���졢����������Ʒ�������ԭ����</a>", "");
        jsStr = jsStr.replace("<a href=http://www.qidian.com>��������� www.qidian.com ��ӭ������ѹ����Ķ������¡���졢����������Ʒ�������ԭ����</a>", "");
        jsStr = jsStr.replace("<ahref=http://www.qidian.com>���������www.qidian.com��ӭ������ѹ����Ķ������¡���졢����������Ʒ�������ԭ����</a>", "");
        jsStr = jsStr.replace("');", "");
        jsStr = jsStr.replace("<p>", "\n");
        jsStr = jsStr.replace("����", "");
        return jsStr;
    }
}


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author guanli
 */
public class site_qidian {
    public static String qidian_getIndexURL_Desk(int bookid) {
        return "http://read.qidian.com/BookReader/" + bookid + ".aspx";
    }

    public static String qidian_getSearchURL_Mobile(String BookName) {
        String xx = "";
        try {
            xx = "http://3g.if.qidian.com/api/SearchBooksRmt.ashx?key=" + URLEncoder.encode(BookName, "UTF-8") + "&p=0";
        } catch (Exception e) {
            e.toString();
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
                item.put("url", qidian_getIndexURL_Desk(slist.getJSONObject(i).getInt("BookId")));
                data.add(item);
            }
        } catch (Exception e) {
            e.toString();
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
        Matcher m = Pattern.compile("(?i).*/([0-9]+)\\.").matcher(iURL);
        while (m.find()) {
            sQDID = m.group(1);
        }
        return Integer.valueOf(sQDID);
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
    
    /**
    *
    * @param pageInfoURL ���Ƶ�ַ /1939238,53927617.aspx
    * @return http://files.qidian.com/Author7/1939238/53927617.txt
    */
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
        jsStr = jsStr.replace("');", "");
        jsStr = jsStr.replace("<p>", "\n");
        jsStr = jsStr.replace("����", "");
        return jsStr;
    }
}


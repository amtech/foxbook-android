package com.linpinger.foxbook;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class site_zssq {
	// { ׷������
		public static String getUrlSE(String bookname) { //׷������: in:���� out: ������ַ
			try {
				return "http://api.zhuishushenqi.com/book?view=search&query=" + URLEncoder.encode(bookname, "UTF-8") ;
			} catch (UnsupportedEncodingException e) {
				e.toString();
			}
			return "";
		}
		
		public static String getUrlSL(String bookid) { //׷������: in:bookid   out: �����Դ�б��ַ
			return "http://api.zhuishushenqi.com/toc?view=summary&book=" + bookid ;
		}
		
		public static String getUrlPage(String pageURL) { //׷������: in:URL   out: ����ҳ���ַ
			try {
				return "http://chapter.zhuishushenqi.com/chapter/" + URLEncoder.encode(pageURL, "UTF-8") ;
			} catch (UnsupportedEncodingException e) {
				e.toString();
			}
			return "";
		}

		public static List<Map<String, Object>> json2PageList(String json, int cLast) { // �鿴ģʽ
			return json2PageList(json, cLast, 0);
		}
		public static List<Map<String, Object>> json2PageList(String json, int cLast, int FoxMod) { // ׷������: in:json,������ҳ,�Ǹ���ģʽô  out:ҳ���б�
			List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(55);
			try {
				JSONArray slist = new JSONObject(json).getJSONArray("chapters");
				int cList = slist.length();
				int sNum = 0 ;
				
				if ( cLast > 0 && cLast < cList ) { //��ʾ�����
					sNum = cList - cLast ;
				}

				Map<String, Object> item;
				String sURL = "";
				for(int i=sNum; i<cList; i++) {
					item = new HashMap<String, Object>();
					item.put("name", slist.getJSONObject(i).getString("title"));
					sURL = slist.getJSONObject(i).getString("link");
					if ( FoxMod == 0 ){ // ���߲鿴ģʽ
						item.put("url", getUrlPage(sURL));
					}
					if ( FoxMod == 1 ){ // ����ģʽ
						item.put("url", sURL);
					}
					data.add(item);
				}
			} catch (Exception e) {
				e.toString();
			}
			return data;
		}

		public static String json2Text(String json){ //׷������: in:json  out:ҳ������
			try {
				json = new JSONObject(json).getJSONObject("chapter").getString("body");
			} catch (JSONException e) {
				e.toString();
			}
			json = json.replace("\\n", "\n");
			json = json.replace("����", "");
			return json;
		}
		
		public static String json2BookID(String json) { // ׷������: in:json  out:BookID
			String bookid = "";
			try {
				bookid = new JSONArray(json).getJSONObject(0).getString("_id"); // ȡ������0��ID
			} catch (JSONException e) {
				e.toString();
			}
			return bookid;
		}
		
		public static List<Map<String, Object>> json2SiteList(String json) { // ׷������: in:json  out:data
			List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(55);
			try {
				JSONArray slist = new JSONArray(json);
				int cList = slist.length();
				Map<String, Object> item;
				for(int i=0; i<cList; i++) {
					item = new HashMap<String, Object>();
					item.put("url","http://api.zhuishushenqi.com/toc/" + slist.getJSONObject(i).getString("_id") + "?view=chapters");
//						item.put("url", slist.getJSONObject(i).getString("link"));
					item.put("name", slist.getJSONObject(i).getString("lastChapter"));
					data.add(item);
				}
			} catch (Exception e) {
				e.toString();
			}
			return data;
		}
		
	// } ׷������
}

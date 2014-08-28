package com.linpinger.foxbook;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.ray.tools.umd.builder.Umd;
import com.ray.tools.umd.builder.UmdChapters;
import com.ray.tools.umd.builder.UmdHeader;

import android.annotation.SuppressLint;
import android.os.Environment;

public class FoxBookLib {
	

	public static void all2txt(FoxMemDB db) { // �����鼮תΪtxt
		String txtPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.txt";
		String sContent = "" ;
		List<Map<String, Object>> data = FoxMemDBHelper.getEbookChaters(false, db);
		Iterator<Map<String, Object>> itr = data.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			sContent = sContent + (String) mm.get("title") + "\n\n" + (String) mm.get("content") + "\n\n\n" ;
		}

		try {
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(txtPath, false));
			bw1.write(sContent);
			bw1.flush();
			bw1.close();
		} catch (IOException e) {
			e.toString();
//			e.printStackTrace();
		}
	}
	
	public static void all2epub(FoxMemDB db) {// �����鼮תΪepub
		String epubPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.epub";
		FoxEpub oEpub = new FoxEpub("FoxBook", epubPath);
		
		List<Map<String, Object>> data = FoxMemDBHelper.getEbookChaters(true, db);
		Iterator<Map<String, Object>> itr = data.iterator();
		HashMap<String, Object> mm;
		while (itr.hasNext()) {
			mm = (HashMap<String, Object>) itr.next();
			oEpub.AddChapter((String)mm.get("title"), (String)mm.get("content"), -1);
		}
		oEpub.SaveTo();
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
	
	
	public static void updatepage(int pageid, FoxMemDB db) {
		Map<String, String> xx = db.getOneRow("select book.url as bu,page.url as pu from book,page where page.id=" + String.valueOf(pageid) + " and  book.id in (select bookid from page where id=" + String.valueOf(pageid) + ")");
		String fullPageURL = getFullURL(xx.get("bu"),xx.get("pu"));		// ��ȡbookurl, pageurl �ϳɵõ�url

		updatepage(pageid, fullPageURL, db) ;
	}
	
	public static String updatepage(int pageid, String pageFullURL, FoxMemDB db) {
		String text = "";
		String html = "" ;
		int site_type = 0 ; // ����ҳ�洦�� 

		if ( pageFullURL.contains(".qidian.com") ) { site_type = 99 ; }
		if ( pageFullURL.contains(".qreader.") ) { site_type = 13 ; }
		if ( pageFullURL.contains("zhuishushenqi.com") ) { site_type = 12 ; } // ����÷���qidian���棬��Ϊ��ʱ��zssq��ַ���������url

		switch(site_type) {
			case 12:
				String json = downhtml(pageFullURL, "utf-8"); // ����json
				text = site_zssq.json2Text(json);
				break;
			case 13:
				text = site_qreader.qreader_GetContent(pageFullURL);
				break;
			case 99:
				String nURL = site_qidian.qidian_toPageURL_FromPageInfoURL(pageFullURL) ;
				if ( nURL.equalsIgnoreCase("") ) {
					text = "" ;
				} else {
					html = downhtml(nURL);
					text = site_qidian.qidian_getTextFromPageJS(html);
				}
				break;
			default:
				html = downhtml(pageFullURL); // ����url
				text = pagetext(html);   	// �����õ�text
		}

		if ( pageid > 0 ) { // ��pageidС��0ʱ��д�����ݿ⣬��Ҫ�������߲鿴
			FoxMemDBHelper.setPageContent(pageid, text,db); // д�����ݿ�
			return String.valueOf(0);
		} else {
			return text;
		}
	}
	
	public static List<Map<String, Object>> getSearchEngineHref(String html, String KeyWord) {
//		String KeyWord = "����Ѫ��" ;
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(64);
		Map<String, Object> item;

		html = html.replace("\t", "");
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replaceAll("(?i)<!--[^>]+-->", "") ;
		html = html.replace("<em>", "");
		html = html.replace("</em>", "");
		html = html.replace("<b>", "");
		html = html.replace("</b>", "");
		html = html.replace("<strong>", "");
		html = html.replace("</strong>", "");


		// ��ȡ���� ������ṹ��
		Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[\"']?[^>]*> *([^<]+)<").matcher(html);
		while ( mat.find() ) {
			if ( 2 == mat.groupCount() ) {
// 				System.out.println(mat.group(1) + "|" + mat.group(2)) ;
				if ( mat.group(1).length() < 5 ) {
						continue ;
				}
				if ( mat.group(1).indexOf("http") < 0 ) {
						continue ;
				}
				if ( mat.group(2).indexOf(KeyWord) < 0 ) {
						continue ; 
				}

				item = new HashMap<String, Object>();
				item.put("url", mat.group(1));
				item.put("name", mat.group(2));
				data.add(item);
			}
		}

		return data ;
	}
	

	@SuppressLint("UseSparseArrays")
	public static List<Map<String, Object>> tocHref(String html, int lastNpage) {
		List<Map<String, Object>> ldata = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> item;
		int nowurllen = 0;
		HashMap<Integer, Integer> lencount = new HashMap<Integer, Integer>();

		// ��Щ��̬��վû��body��ǩ����javaû�ҵ� body ʱ�� �����������ҳ���ٶȺ���
		
		if (html.matches("(?smi).*<body.*")) {
			html = html.replaceAll("(?smi).*<body[^>]*>(.*)</body>.*", "$1"); // ��ȡ����
		} else {
			html = html.replaceAll("(?smi).*?</head>(.*)", "$1"); // ��ȡ����
		}
		html = html.replaceAll("(?smi)<span[^>]*>", ""); // ���<a></a>֮����span��ǩ
		html = html.replace("</span>", "");

		// ��ȡ���� ������ṹ��
		Matcher mat = Pattern.compile(
				"(?smi)href *= *[\"']?([^>\"']+)[^>]*> *([^<]+)<")
				.matcher(html);
		while (mat.find()) {
			if (2 == mat.groupCount()) {
				if ( ((String)mat.group(1)).contains("javascript:") ) continue ; // ����js����
				// System.out.println(mat.group(1) + "|" + mat.group(2)) ;
				item = new HashMap<String, Object>();
				item.put("url", mat.group(1));
				item.put("name", mat.group(2));
				nowurllen = mat.group(1).length();
				item.put("len", nowurllen);
				ldata.add(item);

				if (null == lencount.get(nowurllen)) {
					lencount.put(nowurllen, 1);
				} else {
					lencount.put(nowurllen, 1 + lencount.get(nowurllen));
				}
			}
		}

		// ����hashmap lencount ��ȡ����url����
		int maxurllencount = 0;
		int maxurllen = 0;
		Iterator iter = lencount.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			if (maxurllencount < (Integer) val) {
				maxurllencount = (Integer) val;
				maxurllen = (Integer) key;
			}
		}
		int maxurllensma = maxurllen - 1;
		int maxurllenbig = maxurllen + 1;

		List<Map<String, Object>> od = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> oi;

		// ɸѡ��������������
		int nowlen = 0;
		Iterator<Map<String, Object>> itr = ldata.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			nowlen = (Integer) mm.get("len");
			if (maxurllensma == nowlen || maxurllen == nowlen || maxurllenbig == nowlen) {
				oi = new HashMap<String, Object>();
				oi.put("url", (String) mm.get("url"));
				oi.put("name", (String) mm.get("name"));
				// oi.put("len", mm.get("len"));
				od.add(oi);
			}
		}

		if ( lastNpage > 0 ) { // ��ȡ��һ����
			int chaptercount = od.size();
			if (chaptercount > lastNpage) {
				int startnum = chaptercount - lastNpage;
				// ɸѡ��������������
				List<Map<String, Object>> odn = new ArrayList<Map<String, Object>>(100);
				Iterator<Map<String, Object>> itr1 = od.iterator();
				int ncountx = 0;
				while (itr1.hasNext()) {
					++ncountx;
					if (ncountx <= startnum) {
						itr1.next();
						continue;
					} else {
						odn.add(itr1.next());
					}
				}
				return odn;
			}
		}

		return od;
	}

	// ��pageҳ��htmlת��Ϊ�ı���ͨ�ù���
	public static String pagetext(String html) {
		// ���� novel Ӧ������<div>�����ŵ������
		// ��Щ��̬��վû��body��ǩ����javaû�ҵ�<bodyʱ��replaceAll���������html���ٶȺ���
		if (html.matches("(?smi).*<body.*")) {
			html = html.replaceAll("(?smi).*<body[^>]*>(.*)</body>.*", "$1"); // ��ȡ����
		} else {
			html = html.replaceAll("(?smi).*?</head>(.*)", "$1"); // ��ȡ����
		}

		// MinTag
		html = html.replaceAll("(?smi)<script[^>]*>.*?</script>", ""); // �ű�
		html = html.replaceAll("(?smi)<!--[^>]+-->", ""); // ע�� �ټ�
		html = html.replaceAll("(?smi)<iframe[^>]*>.*?</iframe>", ""); // ���
																		// �൱�ټ�
		html = html.replaceAll("(?smi)<h[1-9]?[^>]*>.*?</h[1-9]?>", ""); // ����
																			// �൱�ټ�
		html = html.replaceAll("(?smi)<meta[^>]*>", ""); // ���� �൱�ټ�

		// 2ѡ1,�������������֣�Ŀǰû������ô��̬�ģ�����ɾ��
		html = html.replaceAll("(?smi)<a [^>]+>.*?</a>", ""); // ɾ�����Ӽ��м�����
		// html = html.replaceAll("(?smi)<a[^>]*>", "<a>"); // �滻����Ϊ<a>

		// ��html��������,�������������������ݣ����԰������
		html = html.replaceAll("(?smi)<div[^>]*>", "<div>");
		html = html.replaceAll("(?smi)<font[^>]*>", "<font>");
		html = html.replaceAll("(?smi)<table[^>]*>", "<table>");
		html = html.replaceAll("(?smi)<td[^>]*>", "<td>");
		html = html.replaceAll("(?smi)<ul[^>]*>", "<ul>");
		html = html.replaceAll("(?smi)<dl[^>]*>", "<dl>");
		html = html.replaceAll("(?smi)<span[^>]*>", "<span>");

		html = html.toLowerCase();
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replace("\t", "");
		html = html.replace("</div>", "</div>\n");
		html = html.replace("<div></div>", "");
		html = html.replace("<li></li>", "");
		html = html.replace("  ", "");

		// getMaxLine -> ll[nMaxLine]
		String[] ll = html.split("\n");
		int nMaxLine = 0;
		int nMaxCount = 0;
		int tmpCount = 0;
		for (int i = 0; i < ll.length; i++) {
			tmpCount = ll[i].length();
			if (tmpCount > nMaxCount) {
				nMaxLine = i;
				nMaxCount = tmpCount;
			}
		}
		html = ll[nMaxLine];

		// html2txt
		html = html.replace("\t", "");
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replace("&nbsp;", "");
		html = html.replace("����", "");
		html = html.replace("<br>", "\n");
		html = html.replace("</br>", "\n");
		html = html.replace("<br/>", "\n");
		html = html.replace("<br />", "\n");
		html = html.replace("<p>", "\n");
		html = html.replace("</p>", "\n");
		html = html.replace("<div>", "\n");
		html = html.replace("</div>", "\n");
		html = html.replace("\n\n", "\n");

		// ���������е�<img��ǩ�����Խ�������������������:�޴�

		// ������վ������Է�������
		// 144��Ժ������ᵼ��������������Ҳɾ���ˣ���ʹ�������޸�
		html = html.replaceAll("(?smi)<span[^>]*>.*?</span>", ""); // ɾ��<span>�����ǻ����ַ��� ��� �ݺ����Ļ����ַ����Լ���Ҷ���β��ǩ��һ�㶼û��span��ǩ
		html = html.replaceAll("(?smi)<[^<>]+>", ""); // �������һ��������ʱ����ע��: ɾ�� html��ǩ,�Ľ��ͣ���ֹ�����в��ɶԵ�<

		return html;
	}

	public static String getFullURL(String sbaseurl, String suburl) { // ��ȡ����·��
		String allURL = "" ;
		if ( sbaseurl.contains("zhuishushenqi.com") ) {     // �������ַ������ϳ�URL�账��һ�� http://xxx.com/fskd/http://wwe.comvs.fs
			if (suburl.contains("zhuishushenqi.com")) {
				return suburl;
			} else {
				return site_zssq.getUrlPage(suburl) ;
			}
		}
		try {
			allURL = (new URL(new URL(sbaseurl), suburl)).toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return allURL;
	}

	//��ȡ�����ļ���ת�浽outPath�У�outPath��Ҫ���ļ���׺��
	public static void saveHTTPFile(String inURL,String outPath) {
		File toFile = new File(outPath);
		if ( toFile.exists() ) { toFile.delete(); }
		try {
//			toFile.createNewFile();
			FileOutputStream outImgStream = new FileOutputStream(toFile);
			outImgStream.write(downHTTP(inURL, "GET"));
			outImgStream.close();
		} catch ( Exception e ) {
			e.toString();
		}
	}

	public static String downhtml(String inURL) {
		return downhtml(inURL, "");
	}
	
	public static String downhtml(String inURL, String pageCharSet) {
		return downhtml(inURL, pageCharSet, "GET");
	}

	public static String downhtml(String inURL, String pageCharSet, String PostData) {
		byte[] buf = downHTTP(inURL, PostData) ;
		if ( buf == null ) { return ""; }
		try {
			String html = "";
			if ( pageCharSet == "" ) {
				html = new String(buf, "gbk");
				if ( html.matches("(?smi).*<meta[^>]*charset=[\"]?(utf8|utf-8)[\"]?.*") ) { // ̽�����
					html = new String(buf, "utf-8");
				}
			} else {
				html = new String(buf, pageCharSet);
			}
			return html;
		} catch ( Exception e ) { // ���� ������
//			e.toString() ;
			return "" ;
		}
	}

	public static byte[] downHTTP(String inURL, String PostData) {
		byte[] buf = null ;
		try {
			URL url = new URL(inURL) ;
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if ( "GET" != PostData ) {
				System.out.println("I am Posting ...");
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type","text/plain; charset=UTF-8");
//				conn.setInstanceFollowRedirects(true);
			}

			conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/2.14 Java/1.6.0_55"); // Android�Դ�ͷ����IE8ͷ���ᵼ��yahoo�����������Ϊ׷������
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
			conn.setConnectTimeout(5000);
			conn.connect();
			
			if ( "GET" != PostData ) {
				conn.getOutputStream().write(PostData.getBytes("UTF-8"));
				conn.getOutputStream().flush();
				conn.getOutputStream().close();
			}
			
			// �жϷ��ص��Ƿ���gzip����
			boolean bGZDate = false ;
			Map<String,List<String>> rh = conn.getHeaderFields();
			List<String> ce = rh.get("Content-Encoding") ;
			if ( null == ce ) { // ����gzip����
				bGZDate = false ;
			} else {
				bGZDate = true ;
			}
	
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int len = 0;
	
			if ( bGZDate ) { // Gzip
				InputStream in = conn.getInputStream();
				GZIPInputStream gzin = new GZIPInputStream(in);
				while ((len = gzin.read(buffer)) != -1) {
					outStream.write(buffer, 0, len);
				}
				gzin.close();
				in.close();
			} else {
				InputStream in = conn.getInputStream();
				while ((len = in.read(buffer)) != -1) {
					outStream.write(buffer, 0, len);
				}
				in.close();
			}
	
			buf = outStream.toByteArray();
			outStream.close();
		} catch ( Exception e ) { // ���� ������
			e.toString() ;
		}
		return buf;
	}

    public static List compare2GetNewPages(List<Map<String, Object>> xx, String existList) {
        existList = existList.toLowerCase();
        int xxSize = xx.size();
//              System.out.println(existList + xxSize);
        if (existList.contains("��ֹ=")) { // ���� ��ֹ ����һ�� xx
//			((ArrayList)xx).trimToSize();
            Matcher mat = Pattern.compile("(?i)��ֹ=([0-9-]+),([0-9-]+)").matcher(existList);
            int qz_1 = 0;
            int qz_2 = 0;
            while (mat.find()) {
                qz_1 = Integer.valueOf(mat.group(1));
                qz_2 = Integer.valueOf(mat.group(2));
            }

// System.out.println("size: " + xxSize + " - " + qz_2 + " + " + qz_1);

            ArrayList<Map<String, Object>> nXX = new ArrayList<Map<String, Object>>(30);
            // ����ĳ�ʼֵ���ж�˳����ò�Ҫ���䶯
            int sIdx = 0;
            int eIdx = xxSize;
            int leftIdx = 0;
            if (qz_2 > 0) {
                sIdx = qz_2;
                leftIdx = eIdx - sIdx;
            }
            if (qz_1 < 0) {
                eIdx = eIdx + qz_1;
                leftIdx = leftIdx + qz_1;
            }
            if (leftIdx > 0) {
                int nSIdx = 0;
                for (int i = 0; i < leftIdx; i++) {
                    nSIdx = sIdx + i;
                    nXX.add(xx.get(nSIdx));
                }
                xx = nXX;
            } else { // �½�����Ϊ��
                if (55 == xxSize) {
                    String jj[] = existList.split("\n");
                    if (jj.length > 2) { // ��ȡ��ɾ����¼�е�һ��֮��ļ�¼��������½�>55���ܻᱯ��
                        String sToBeComp = jj[jj.length - 2];
                        ArrayList<Map<String, Object>> nX2 = new ArrayList<Map<String, Object>>(30);
                        Iterator itr = xx.iterator();
                        String nowurl = "";
                        boolean bFillArray = false;
                        while (itr.hasNext()) {
                            HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                            nowurl = mm.get("url").toString();
                            if (sToBeComp.contains(nowurl)) {
                                bFillArray = true;
                                nX2.add(mm);
                            } else {
                                if (bFillArray) {
                                    nX2.add(mm);
                                }
                            }
                        }
                        xx = nX2;
                    } else {
                        System.out.println("error: jj < 2 : " + jj.length);
                    }
                } else {  // ����ŵĴ�����û�����½ڵĴ�����
                    return new ArrayList<HashMap<String, Object>>();
                }
            }
        }

        // �Ƚϵõ����½�
        String nowURL;
        ArrayList<HashMap<String, Object>> newPages = new ArrayList<HashMap<String, Object>>();
        Iterator<Map<String, Object>> itr = xx.iterator();
        while (itr.hasNext()) {
            HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
            nowURL = (String) mm.get("url");
            if (!existList.contains(nowURL.toLowerCase() + "|")) { // ���½�
                newPages.add(mm);
            }
        }
//			int newpagecount = newPages.size(); // ���½���������ͳ��
        return newPages;
    }

} // �����

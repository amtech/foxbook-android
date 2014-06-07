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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.ray.tools.umd.builder.Umd;
import com.ray.tools.umd.builder.UmdChapters;
import com.ray.tools.umd.builder.UmdHeader;

import android.annotation.SuppressLint;
import android.os.Environment;

public class FoxBookLib {
	

	public static void all2txt() { // �����鼮תΪtxt
		String txtPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.txt";
		String sContent = "" ;
		List<Map<String, Object>> data = FoxDB.getUMDArray();
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
	

	public static void all2umd() { // �����鼮תΪumd
		String umdPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.umd";
		Umd umd = new Umd();
		
		UmdHeader uh = umd.getHeader(); // �����鼮��Ϣ
		uh.setTitle("FoxBook�����½�");
		uh.setAuthor("������֮��");
		uh.setBookType("С˵");
		uh.setYear("2014");
		uh.setMonth("04");
		uh.setDay("01");
		uh.setBookMan("������֮��");
		uh.setShopKeeper("������֮��");

		
		UmdChapters  cha = umd.getChapters(); // ��������
		List<Map<String, Object>> data = FoxDB.getUMDArray();
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
	
	
	public static void updatepage(int pageid) {
		Map<String, String> xx = FoxDB.getOneRow("select book.url as bu,page.url as pu from book,page where page.id=" + String.valueOf(pageid) + " and  book.id in (select bookid from page where id=" + String.valueOf(pageid) + ")");
		String fullPageURL = getFullURL(xx.get("bu"),xx.get("pu"));		// ��ȡbookurl, pageurl �ϳɵõ�url

		updatepage(pageid, fullPageURL) ;
	}
	
	public static String updatepage(int pageid, String pageFullURL) {
		String text = "";
		String html = "" ;
		int site_type = 0 ; // ����ҳ�洦�� 

		if ( pageFullURL.contains(".qidian.com") ) { site_type = 99 ; }
		if ( pageFullURL.contains("zhuishushenqi.com") ) { site_type = 12 ; } // ����÷���qidian���棬��Ϊ��ʱ��zssq��ַ���������url

		switch(site_type) {
			case 12:
				String json = downhtml(pageFullURL, "utf-8"); // ����json
				text = site_zssq.json2Text(json);
				break;
			case 99:
				Matcher mat = Pattern.compile("(?i)/([0-9]+),([0-9]+).aspx").matcher(pageFullURL);
				String bid = "";
				String cid = "";
				while (mat.find()) {
					bid = mat.group(1);
					cid = mat.group(2);
				}
				String nURL = "http://files.qidian.com/Author" + ( 1 + ( Integer.valueOf(bid) % 8 ) ) + "/" + bid + "/" + cid + ".txt"  ;
				html = downhtml(nURL);
				html = html.replace("document.write('", "");
				html = html.replace("<a href=http://www.qidian.com>��������� www.qidian.com ��ӭ������ѹ����Ķ������¡���졢����������Ʒ�������ԭ����</a>", "");
				html = html.replace("<a>�ֻ��û��뵽m.qidian.com�Ķ���</a>');", "");
				html = html.replace("');", "");
				text = pagetext(html);   	// �����õ�text
				break;
			default:
				html = downhtml(pageFullURL); // ����url
				text = pagetext(html);   	// �����õ�text
		}

		if ( pageid > 0 ) { // ��pageidС��0ʱ��д�����ݿ⣬��Ҫ�������߲鿴
			FoxDB.setPageContent(pageid, text); // д�����ݿ�
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
		Iterator<Entry<Integer, Integer>>  iter = lencount.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, Integer> entry = iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			if (maxurllencount < (Integer) val) {
				maxurllencount = (Integer) val;
				maxurllen = (Integer) key;
			}
		}
		int maxurllenbig = maxurllen + 1;

		List<Map<String, Object>> od = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> oi;

		// ɸѡ��������������
		int nowlen = 0;
		Iterator<Map<String, Object>> itr = ldata.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			nowlen = (Integer) mm.get("len");
			if (maxurllen == nowlen || maxurllenbig == nowlen) {
				oi = new HashMap<String, Object>();
				oi.put("url", (String) mm.get("url"));
				oi.put("name", (String) mm.get("name"));
				// oi.put("len", mm.get("len"));
				od.add(oi);
			}
		}

		if ( lastNpage > 0 ) { // ��ʾ����
			int chaptercount = od.size();
			if (chaptercount > lastNpage) {
				int startnum = chaptercount - lastNpage;
				List<Map<String, Object>> odn = new ArrayList<Map<String, Object>>(
						100);
				Map<String, Object> oin;
				// ɸѡ��������������
				Iterator<Map<String, Object>> itr1 = od.iterator();
				int ncountx = 0;
				while (itr1.hasNext()) {
					oin = (HashMap<String, Object>) itr1.next();
					++ncountx;
					if (ncountx < startnum) {
						continue;
					} else {
						odn.add(oin);
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
			outImgStream.write(downHTTP(inURL));
			outImgStream.close();
		} catch ( Exception e ) {
			e.toString();
		}
	}

	public static String downhtml(String inURL) {
		return downhtml(inURL, "");
	}

	public static String downhtml(String inURL, String pageCharSet) {
		byte[] buf = downHTTP(inURL) ;
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

	public static byte[] downHTTP(String inURL) {
		byte[] buf = null ;
		try {
			URL url = new URL(inURL) ;
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/2.14 Java/1.6.0_55"); // Android�Դ�ͷ����IE8ͷ���ᵼ��yahoo�����������Ϊ׷������
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
			conn.setConnectTimeout(5000);
			conn.connect();
			
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


} // �����

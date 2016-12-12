package com.linpinger.foxbook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

public class FoxBookLib {
	
	public static HashMap<String, Object> getPage1024(String html) { // ����õ�1024�ı��������: title, content
		// Used by: Activity_EBook_Viewer , Activity_ShowPage4Eink 
		HashMap<String, Object> oM = new HashMap<String, Object>();

		// ����
		// <center><b>�鿴�����汾: [-- <a href="read.php?tid=21" target="_blank">[11-14] ���Ǿ��⴫</a> --]</b></center>
		Matcher mat2 = Pattern.compile("(?smi)<center><b>[^>]*?>([^<]*?)</a>").matcher(html);
		while (mat2.find()) {
			oM.put("title", mat2.group(1));
		}

		// ����
		String text = "";
		html = html.replace("<script src=\"http://u.phpwind.com/src/nc.php\" language=\"JavaScript\"></script><br>", "")
				.replaceAll("<br>[ ��]*", "<br>")
				.replace("\r", "")
				.replace("\n", "")
				.replace("<br>", "\n")
				.replace("&nbsp;", " ")
				.replace("\n\n", "\n")
				.replace("\n\n", "\n");
		Matcher mat = Pattern.compile("(?smi)\"tpc_content\">(.*?)</td>").matcher(html);
		while (mat.find()) {
			text = text + mat.group(1) + "\n-----#####-----\n" ;
		}
		oM.put("content", text);

		return oM ;
	}

    public static List compare2GetNewPages(List<Map<String, Object>> aHTML, String DelList) {
        int htmlSize = aHTML.size();
        if ( 0 == htmlSize ) // aHTMLΪ��(������ҳ����������)
            return aHTML ;
        if ( ! DelList.contains("|") ) // ��DelListΪ�գ�����ԭ����
            return aHTML ;
        
        // ��ȡ DelList ��һ�е� URL : BaseLineURL
        int fFF = DelList.indexOf("|");
        String BaseLineURL = DelList.substring(1 + DelList.lastIndexOf("\n", fFF), fFF);
        
        // �鵽����aHTML�е���BaseLineURL���кţ���ɾ��1�����кŵ�����Ԫ��
        int EndIdx = 0 ;
        String nowURL ;
        for (int nowIdx = 0; nowIdx < htmlSize; nowIdx++) {
            nowURL = (String) (( (HashMap<String, Object>)(aHTML.get(nowIdx)) ).get("url"));
            if ( BaseLineURL.equalsIgnoreCase(nowURL) ) {
                EndIdx = nowIdx ;
                break ;
            }
        }
        for (int nowIdx = EndIdx; nowIdx >= 0; nowIdx--) {
            aHTML.remove(nowIdx);
        }
        htmlSize = aHTML.size();
        
        // �Ա�ʣ���aHTML��DelList���õ��µ�aNewRet������
        ArrayList<Map<String, Object>> aNewRet = new ArrayList<Map<String, Object>>(30);
        for (int nowIdx = 0; nowIdx < htmlSize; nowIdx++) {
            nowURL = (String) (( (HashMap<String, Object>)(aHTML.get(nowIdx)) ).get("url"));
            if ( ! DelList.contains("\n" + nowURL + "|") )
                aNewRet.add(aHTML.get(nowIdx));
        }
        
        return aNewRet ;
    }

/*
    public static List compare2GetNewPages2(List<Map<String, Object>> xx, String existList) {
        existList = existList.toLowerCase();
        int xxSize = xx.size();
        if (existList.contains("��ֹ=")) { // ���� ��ֹ ����һ�� xx
            Matcher mat = Pattern.compile("(?i)��ֹ=([0-9-]+),([0-9-]+)").matcher(existList);
            int qz_1 = 0;
            int qz_2 = 0;
            while (mat.find()) {
                qz_1 = Integer.valueOf(mat.group(1));
                qz_2 = Integer.valueOf(mat.group(2));
            }

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
                            nowurl = mm.get("url").toString().toLowerCase();
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
        return newPages;
    }
*/
    public static String simplifyDelList(String DelList) { // ���� DelList
        int nLastItem = 9;
        DelList = DelList.replace("\r", "").replace("\n\n", "\n");
        String[] xx = DelList.split("\n");
        if (xx.length < (nLastItem + 2)) {
            return DelList;
        }
        int MaxLineCount = xx.length - nLastItem;

        StringBuilder newList = new StringBuilder(4096);
        for (int i = 0; i < 9; i++) {
            newList.append(xx[MaxLineCount + i]).append("\n");
        }
        return newList.toString();
    }

/*
    public static String simplifyDelList2(String DelList) { // ���� DelList
        int qi = 0;
        int zhi = 0;
        if (DelList.contains("��ֹ=")) {
            Matcher mat = Pattern.compile("(?i)��ֹ=([0-9\\-]+),([0-9\\-]+)").matcher(DelList);
            while (mat.find()) {
                qi = Integer.valueOf(mat.group(1));
                zhi = Integer.valueOf(mat.group(2));
            }
        }
        DelList = DelList.replace("\r", "").replace("\n\n", "\n");
        String[] xx = DelList.split("\n");
        if (xx.length < 15) {
            return DelList;
        }
        int MaxLineCount = xx.length - 9;

        StringBuffer newList = new StringBuffer(1024);
        for (int i = 0; i < 9; i++) {
            newList.append(xx[MaxLineCount + i]).append("\n");
        }
        if (zhi > 0) {
            return "��ֹ=" + qi + "," + String.valueOf(zhi + MaxLineCount - 1) + "\n" + newList.toString();
        } else {
            return "��ֹ=" + qi + "," + String.valueOf(zhi + MaxLineCount) + "\n" + newList.toString();
        }
    }
*/

    public static List<Map<String, Object>> tocHref(String html, int lastNpage) {
        if (html.length() < 100) { //��ҳľ����������
            return new ArrayList<Map<String, Object>>(1);
        }
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

        if (html.contains("http://read.qidian.com/BookReader/")) { // �������Ŀ¼
            html = html.replaceAll("(?smi).*<div id=\"content\">(.*)<div class=\"book_opt\">.*", "$1"); // ��ȡ�б�

            html = html.replace("\n", " ");
            html = html.replace("<a", "\n<a");
            html = html.replaceAll("(?i)<a href.*?/Book/.*?>.*?</a>", ""); // �־��Ķ�
            html = html.replaceAll("(?i)<a href.*?/BookReader/vol.*?>.*?</a>", ""); // �־��Ķ�
            html = html.replaceAll("(?i)<a.*?href.*?/vipreader.qidian.com/.*?>", ""); // vip
            html = html.replaceAll("(?smi)<span[^>]*>", ""); // ���<a></a>֮����span��ǩ
            html = html.replace("</span>", "");
        }

        // ��ȡ���� ������ṹ��
        Matcher mat = Pattern.compile(
                "(?smi)href *= *[\"']?([^>\"']+)[^>]*> *([^<]+)<")
                .matcher(html);
        while (mat.find()) {
            if (2 == mat.groupCount()) {
                if (((String) mat.group(1)).contains("javascript:")) {
                    continue; // ����js����
                }
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
//        System.out.println("MaxURLLen:" + maxurllen);

        int minLen = maxurllen - 2; // ��С����ֵ�����ֵ���Ե���
        int maxLen = maxurllen + 2; // ��󳤶�ֵ�����ֵ���Ե���

        int ldataSize = ldata.size();
        int halfLink = (int) (ldataSize / 2);

        int startDelRowNum = -9;      // ��ʼɾ������
        int endDelRowNum = 9 + ldataSize;  // ����ɾ������
        // ֻ�����ӵ�һ�룬ǰ�����ҿ�ʼ�У�������ҽ�����
        // �ҿ�ʼ
        Integer nowLen = 0;
        Integer nextLen = 0;
        for (int nowIdx = 0; nowIdx < halfLink; nowIdx++) {
            nowLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx))).get("len"));
            if ((nowLen > maxLen) || (nowLen < minLen)) {
                startDelRowNum = nowIdx;
            } else {
                nextLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx + 1))).get("len"));
                if ((nextLen - nowLen > 1) || (nextLen - nowLen < 0)) {
                    startDelRowNum = nowIdx;
                }
            }
        }
        // �ҽ��� nextLen means PrevLen here
        for (int nowIdx = ldataSize - 1; nowIdx > halfLink; nowIdx--) {
            nowLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx))).get("len"));
            if ((nowLen > maxLen) || (nowLen < minLen)) {
                endDelRowNum = nowIdx;
            } else {
                nextLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx - 1))).get("len"));
                if ((nowLen - nextLen > 1) || (nowLen - nextLen < 0)) {
                    endDelRowNum = nowIdx;
                }
            }
        }
//        System.out.println("startDelRowNum:" + startDelRowNum + " - endDelRowNum:" + endDelRowNum + " ldataSize:" + ldataSize);

        // ����ɾԪ��
        if (endDelRowNum < ldataSize) {
            for (int nowIdx = ldataSize - 1; nowIdx >= endDelRowNum; nowIdx--) {
                ldata.remove(nowIdx);
            }
        }
        if (startDelRowNum >= 0) {
            for (int nowIdx = startDelRowNum; nowIdx >= 0; nowIdx--) {
                ldata.remove(nowIdx);
            }
        }

        return getLastNPage(ldata, lastNpage); // ��ȡ��һ����
    }

    public static List<Map<String, Object>> getSearchEngineHref(String html, String KeyWord) { // String KeyWord = "����Ѫ��" ;
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(64);
        Map<String, Object> item;

        html = html.replace("\t", "");
        html = html.replace("\r", "");
        html = html.replace("\n", "");
        html = html.replaceAll("(?i)<!--[^>]+-->", "");
        html = html.replace("<em>", "");
        html = html.replace("</em>", "");
        html = html.replace("<b>", "");
        html = html.replace("</b>", "");
        html = html.replace("<strong>", "");
        html = html.replace("</strong>", "");

        // ��ȡ���� ������ṹ��
        Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[\"']?[^>]*> *([^<]+)<").matcher(html);
        while (mat.find()) {
            if (2 == mat.groupCount()) {
                if (mat.group(1).length() < 5) {
                    continue;
                }
                if (!mat.group(1).startsWith("http")) {
                    continue;
                }
                if (mat.group(1).contains("www.sogou.com/web")) {
                    continue;
                }
                if (!mat.group(2).contains(KeyWord)) {
                    continue;
                }

                item = new HashMap<String, Object>();
                item.put("url", mat.group(1));
                item.put("name", mat.group(2));
                data.add(item);
            }
        }

        return data;
    }

    // ȡ��������Ԫ�أ��������������
    private static List<Map<String, Object>> getLastNPage(List<Map<String, Object>> inArrayList, int lastNpage) {
        int aSize = inArrayList.size();
        if (aSize <= lastNpage || lastNpage <= 0) {
            return inArrayList;
        }
        List<Map<String, Object>> outList = new ArrayList<Map<String, Object>>(100);
        for (int nowIdx = aSize - lastNpage; nowIdx < aSize; nowIdx++) {
            outList.add((HashMap<String, Object>) (inArrayList.get(nowIdx)));
        }
        return outList;
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
        html = html.replaceAll("(?smi)^\n*", "");

        return html;
    }

    public static String getFullURL(String sbaseurl, String suburl) { // ��ȡ����·��
        String allURL = "";
        try {
            allURL = (new URL(new URL(sbaseurl), suburl)).toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return allURL;
    }

    //��ȡ�����ļ���ת�浽outPath�У�outPath��Ҫ���ļ���׺��
    public static void saveHTTPFile(String inURL, String outPath) {
        File toFile = new File(outPath);
        if (toFile.exists()) {
            toFile.delete();
        }
        try {
//          toFile.createNewFile();
            FileOutputStream outImgStream = new FileOutputStream(toFile);
            outImgStream.write(downHTTP(inURL, "GET", null));
            outImgStream.close();
        } catch (Exception e) {
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
        return downhtml(inURL, pageCharSet, PostData, null);
    }

    public static String downhtml(String inURL, String pageCharSet, String PostData, String iCookie) {
        byte[] buf = downHTTP(inURL, PostData, iCookie);
        if (buf == null) {
            return "";
        }
        try {
            String html = "";
            if (pageCharSet == "") {
                html = new String(buf, "gbk");
                if (html.matches("(?smi).*<meta[^>]*charset=[\"]?(utf8|utf-8)[\"]?.*")) { // ̽�����
                    html = new String(buf, "utf-8");
                }
            } else {
                html = new String(buf, pageCharSet);
            }
            return html;
        } catch (Exception e) { // ���� ������
            return "";
        }
    }

    public static byte[] downHTTP(String inURL, String PostData, String iCookie) {
        byte[] buf = null;
        try {
            URL url = new URL(inURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if ("GET" != PostData) {
//              System.out.println("I am Posting ...");
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
            }

            if ( null != iCookie ) {
                conn.setRequestProperty("Cookie", iCookie);
            }

            if (inURL.contains(".13xs.")) {
                conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/3.26"); // 2015-10-27: qqxsʹ�ü��ٱ�����Java��ͷ�ᱻ��г
            } else {
                conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/3.26 Java/1.6.0_55"); // Android�Դ�ͷ����IE8ͷ���ᵼ��yahoo�����������Ϊ׷������
            }
            if (!inURL.contains("files.qidian.com")) { // 2015-4-16: qidian txt ʹ��cdn���٣����ͷ����gzip�ͻ᷵�ش����gz����
                conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            } else {
                conn.setRequestProperty("Accept-Encoding", "*"); // Android ���Զ�����gzip����ӣ�ʹ��*����֮�����CDN������ȷ������
            }
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);    // ��ȡ��ʱ5s
            conn.setUseCaches(false);     // Cache-Control: no-cache     Pragma: no-cache
            
            conn.connect();  // ��ʼ����
            if ("GET" != PostData) {  // ����PostData
                conn.getOutputStream().write(PostData.getBytes("UTF-8"));
                conn.getOutputStream().flush();
                conn.getOutputStream().close();
            }
            
            // ����жϷ���״̬���������жϴ��󣬽���򵥵�����connect���У���������������
            /*
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
//              System.out.println("  Error Happend, responseCode: " + responseCode + "  URL: " + inURL);
                return buf;
            }
            */

            // �жϷ��ص��Ƿ���gzip����
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len = 0;   
            // ���ص��ֶ�: Content-Encoding: gzip/null �ж��Ƿ���gzip
            if (null == conn.getContentEncoding()) { // ����gzip����
                InputStream in = conn.getInputStream();
                while ((len = in.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                in.close();
            } else { // gzip ѹ������
                InputStream in = conn.getInputStream();
                GZIPInputStream gzin = new GZIPInputStream(in);
                while ((len = gzin.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                gzin.close();
                in.close();
            }

            buf = outStream.toByteArray();
            outStream.close();
        } catch (Exception e) { // ���� ������
            e.toString();
        }
        return buf;
    }
    
    // Wget Cookie תΪHTTPͷ��Cookie�ֶ�
    public static String cookie2Field(String iCookie) {
        String oStr = "" ;
        Matcher mat = Pattern.compile("(?smi)\t[0-9]*\t([^\t]*)\t([^\r\n]*)").matcher(iCookie);
        while (mat.find()) {
            oStr = oStr + mat.group(1) + "=" + mat.group(2) + "; " ;
//          System.out.println(mat.group(1) + "=" + mat.group(2));
        }
        return oStr ;
    }


    // { ͨ���ı���ȡ��д��
    // ����ʹ�������ȡ�ı�����㣬������С���Ե���һ���Դﵽ��õ��ٶ�
    public static String readText(String filePath, String inFileEnCoding) {
        // Ϊ���̰߳�ȫ�������滻StringBuilder Ϊ StringBuffer
        StringBuilder retStr = new StringBuilder(174080);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), inFileEnCoding));

            char[] chars = new char[4096]; // �����С��Ӱ���ȡ�ٶ�
            int length = 0;
            while ((length = br.read(chars)) > 0) {
                retStr.append(chars, 0, length);
            }
            /*
             // �������Ч���Եͣ������Կ��ƻ��з�
             String line = null;
             while ((line = br.readLine()) != null) {
             retStr.append(line).append("\n");
             }
             */
            br.close();
        } catch (IOException e) {
            e.toString();
        }
        return retStr.toString();
    }

    // д��ָ�����룬�ٶȿ��
    public static void writeText(String iStr, String filePath, String oFileEncoding) {
        boolean bAppend = false;
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, bAppend), oFileEncoding));
            bw.write(iStr);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.toString();
        }
    }

    /*
    // ������ڶ����и���ı��ϣ��Ѿ���importQidianTxt��ʹ��
    public static String readTextAndSplit(String filePath, String inFileEnCoding) {
        StringBuilder retStr = new StringBuilder(174080);
        StringBuilder chunkStr = new StringBuilder(65536);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), inFileEnCoding));
            String line = null;
            int chunkLen = 0;
            while ((line = br.readLine()) != null) {
                chunkLen = chunkStr.length();
                if ( chunkLen > 3000 && ( line.length() == 0 || chunkLen > 6000 || line.startsWith("��") || line.contains("��") || line.contains("��") || line.contains("��") ) ) {
                    retStr.append(chunkStr).append("\n##################################################\n\n");
                    chunkStr = new StringBuilder(65536);
                }
                chunkStr.append(line).append("\n");
            }
            if ( chunkStr.length() > 0 )
                retStr.append(chunkStr).append("\n#####LAST###########\n\n");
            br.close();
        } catch (IOException e) {
            e.toString();
        }
        return retStr.toString();
    }
    */

    public static String detectTxtEncoding(String txtPath) { // �²������ı����� ����: "GBK" �� "UTF-8"
        byte[] b = new byte[256]; // ��ȡ��ô���ֽڣ������ô���ֽڶ���Ӣ���Ǿͱ�����
        int loopTimes = 256 - 6 ; // ѭ������ = �ֽ��� - 6 ����Խ��
        try {
            FileInputStream in= new FileInputStream(txtPath);
            in.read(b);
            in.close();
        } catch (Exception e) {
            e.toString();
        }
        boolean isGBK = false ;
    if (b[0] == -17 && b[1] == -69 && b[2] == -65) { // UTF-8 BOM : EF BB BF
        isGBK = false ;
    } else {
        int aa = 0 ;
        int bb = 0 ;
        int cc = 0 ;
        int dd = 0 ;
        int ee = 0 ;
        int ff = 0 ;
        int i = 0 ;
        while ( i < loopTimes ) { // 2�ֽ�GBK��3�ֽ�UTF8������Ϊ6����ȡ6���ַ��Ƚ�
            aa = b[i] & 0x000000FF ;
            if ( aa == 0 )
                break ;
            if ( aa < 128 ) {
                i = i + 1 ;
                continue ;
            }
            if ( aa < 192 || aa > 239 ) {
                isGBK = true ;
                break ;
            }

            bb = b[i+1] & 0x000000FF ;
            if ( bb < 128 || bb > 191 ) {
                isGBK = true ;
                break ;
            }

            // �����ֽ�:Ӣ��<128��GBK:129-254��UTF8:128-191 192-239
            cc = b[i+2] & 0x000000FF ;
            if ( cc > 239 ) {
                isGBK = true ;
                break ;
            } else if ( cc < 128 ) {
                i = i + 3 ;
                continue ;
            }

            dd = b[i+3] & 0x000000FF ;
            if ( dd < 64 ) {
                i = i + 4 ;
                continue ;
            }

            ee = b[i+4] & 0x000000FF ;
            if ( ee < 64 ) {
                i = i + 5 ;
                continue ;
            }

            ff = b[i+5] & 0x000000FF ;
            i = i + 6 ;
        // GBK: : 2 : 129-254 64-254
        // UTF8 : 2 : 192-223 128-191
        // UTF8 : 3 : 224-239 128-191 128-191
        } // while
    } // if
        if ( isGBK ) {
            return "GBK" ;
        } else {
            return "UTF-8" ;
        }
    }
/*
GBK     : 1 : 81-FE = 129-254
GBK     : 2 : 40-FE = 64-254

GBK -> UTF-8 :
UTF-8 2 : 1 : C2-D1 = 194-209
UTF-8 2 : 1 : 80-BF = 128-191

UTF-8 3 : 1 : E2-EF = 224-239
UTF-8 3 : 2 : 80-BF = 128-191
UTF-8 3 : 3 : 80-BF = 128-191
*/
// UTF-8 BOM : EF BB BF

/*
1�ֽ� 0xxxxxxx
2�ֽ� 110xxxxx 10xxxxxx
3�ֽ� 1110xxxx 10xxxxxx 10xxxxxx
4�ֽ� 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
5�ֽ� 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
6�ֽ� 1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx

UTF8 : 2 : 192-223 128-191
UTF8 : 3 : 224-239 128-191 128-191
*/

    // } ͨ���ı���ȡ��д��
    
} // �����

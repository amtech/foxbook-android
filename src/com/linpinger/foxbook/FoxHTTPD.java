package com.linpinger.foxbook;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FoxHTTPD extends NanoHTTPD {
	public static FoxMemDB oDB;  // ���ⲿ��ֵ
	private File foxRootDir ;  // nanohttpd ���Ǹ�������˽�еģ��޷��̳�
	private String nowUserAgent = "kindle" ;
	private final String html_foot = "\n</body>\n</html>\n\n" ;
	
	private final String LIST_PAGES = "lp" ;
//	private final String LIST_ONLINE_PAGES = "lop" ;
	private final String SHOW_CONTENT = "sc" ;
//	private final String SHOW_ONLINE_CONTENT = "soc" ;
	private final String DOWN_TXT = "dt" ;

	public FoxHTTPD(int port, File wwwroot) throws IOException {
		super(port, wwwroot);
		this.foxRootDir = wwwroot;
	}

	// ��Ӧ
	@Override
	public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
		
		// ��ҳ�г��鼮
		if (uri.equalsIgnoreCase("/")) {
			nowUserAgent = header.getProperty("user-agent", "kindle");
			String action = parms.getProperty("a", "blank");
			String bookid = parms.getProperty("bid", "0");
			String pageid = parms.getProperty("pid", "0");
			String html = "";

			if ( action.equalsIgnoreCase("blank") )
				html = showBookList() ;  //���
			if ( action.equalsIgnoreCase(LIST_PAGES)) { // �½��б�
				html = showPageList(bookid) ; // �½��б�
			}
			if ( action.equalsIgnoreCase(SHOW_CONTENT)) { // ����
				html = showContent(pageid) ;
			}
			if ( action.equalsIgnoreCase(DOWN_TXT)) { // ����txt
				String txtPath = "/fox.txt" ;
				if ( Integer.valueOf(bookid) > 0 ) {
					txtPath = FoxBookLib.all2txt(bookid, oDB);
				} else { // ����txt
					txtPath = FoxBookLib.all2txt(oDB);
				}
				
				html = "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=" + txtPath +"\"></head><body><a href=\"" + txtPath + "\">Download txt</a></body></html>";
			}
			
			return new Response( HTTP_OK, MIME_HTML, html ) ;
		}

		if (uri.equalsIgnoreCase("/L")) {  // �г�/sdcard/
			return serveFile( "/", header, foxRootDir, true );
		}
		
		if (uri.equalsIgnoreCase("/f")) {
			if ( method.equalsIgnoreCase("get") ) { // �ϴ�ҳ��
				String title = "�ϴ������յ��ļ�";
				StringBuilder html = new StringBuilder();
				html.append("<!DOCTYPE html>\n<html>\n<head>\n\t<META http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n\t<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; minimum-scale=0.1; maximum-scale=3.0; \"/>\n\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=Edge\">\n\t<title>");
				html.append(title).append("</title>\n</head>\n\n<body bgcolor=\"#eefaee\">\n\n");
				html.append("<h3>�ϴ��ļ��� /sdcard/ ��֧�������ļ���</h3>\n\n<form action=\"/f\" enctype=\"multipart/form-data\" method=\"post\">\n\t<input name=\"filename\" type=\"file\" />\n\t<input type=\"submit\" value=\"�ϴ�\" />\n</form>\n");
				html.append("\n</body>\n</html>\n");
				return new Response( HTTP_OK, MIME_HTML, html.toString() ) ;
			} else { // �����ļ��ϴ�
				// �����޸���nanohttpd.java �������ʱ·����/sdcard/����������Ӳ���룬�Ǻ���
				String tmpFilePath = files.getProperty("filename") ; // /sdcard/NanoHTTPD-nnn.upload /data/data/com.linpinger.foxudp/cache/NanoHTTPD-561991304.upload
				String fileName = parms.getProperty("filename", "NoName.upload") ;    // testUpload.exe
				File savePath = new File("/sdcard/" + fileName);
				if ( savePath.exists() ) {
					savePath = new File("/sdcard/newXO_" + fileName);
				}
				(new File(tmpFilePath)).renameTo(savePath);
				return new Response( HTTP_OK, MIME_HTML, "<html>\n<head>\n\t<META http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n\t<title>Return Msg</title>\n</head>\n\n<body bgcolor=\"#eefaee\">\n\n" + tmpFilePath + " -> " + savePath.getAbsolutePath() + "\n\n</body>\n</html>\n") ;
			}
		}
		
//		return new Response( HTTP_OK, MIME_PLAINTEXT, "hello" ) ;
		return serveFile( uri, header, foxRootDir, true );
	}
	
	// ����
	private String showContent(String iPageID) {
		String fontSize = "22px" ;
		if ( nowUserAgent.contains("Android") ) {
			fontSize = "18px" ;
		}
		StringBuilder html = new StringBuilder();
		Map<String,String> infox = oDB.getOneRow("select bookid as bid, Content as cc, Name as naa from page where id = " + iPageID + " and Content is not null");
		if ( infox.isEmpty() ) {
			return "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=?\"></head><body>no this ID</body></html>";
		}
		String title = infox.get("naa") ;
		String bookid = infox.get("bid") ;
		String content = infox.get("cc") ;
		html.append(html_head("utf-8", title, true));
		html.append("<h2>").append(title).append("</h2>\n")
		.append("<div class=\"content\" style=\"font-size:").append(fontSize).append("; line-height:150%;font-family: Microsoft YaHei;\">\n")
		.append("<p>").append(content.replace("\n", "</p>\n<p>"))
		.append("</p>\n</div>\n")
		.append("<p>����").append(title).append("</p>\n")
		.append("<div class=\"book_switch\">\n<ul>")
		.append("\t<li><a href=\"?a=").append(SHOW_CONTENT).append("&pid=").append(Integer.valueOf(iPageID) - 1).append("\">��һ��</a></li>\n")
		.append("\t<li><a href=\"?a=").append(LIST_PAGES).append("&bid=").append(bookid).append("\">����Ŀ¼</a></li>\n")
		.append("\t<li><a href=\"?\">�������</a></li>\n")
		.append("\t<li><a href=\"?a=").append(SHOW_CONTENT).append("&pid=").append(Integer.valueOf(iPageID) + 1).append("\">��һ��</a></li>\n")
		.append("</ul>\n</div>\n\n").append(html_foot);
		return html.toString() ;
	}
	
	// �½��б�
	private String showPageList(String iBookID) {
		StringBuilder html = new StringBuilder();
		html.append(html_head("�����յ��½��б�"));
		
		List<Map<String, Object>> data ;
		if ( Integer.valueOf(iBookID) > 0 ) {
			data = FoxMemDBHelper.getPageList("where bookid = " + iBookID + " order by bookid,id", oDB);
		} else {
			data = FoxMemDBHelper.getPageList("order by bookid,id", oDB);
		}
		html.append("<ol>\n");
		Iterator<Map<String, Object>> itr = data.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			html.append("\t<li><a href=\"?a=").append(SHOW_CONTENT).append("&pid=").append(mm.get("id"))
				.append("\" title=\"").append(mm.get("url")).append("\">")
				.append(mm.get("name")).append("</a> (").append(mm.get("count")).append(")</li>\n");
		}		
		html.append("\n</ol>\n").append(html_foot);
		return html.toString() ;
	}
	// ���
	private String showBookList() {
		StringBuilder html = new StringBuilder();
		html.append(html_head("�����յ��鼮�б�"));
		
		html.append("<br>����<a href=\"?a=").append(LIST_PAGES)
			.append("&bid=0\">��ʾ�����½�</a>  <a href=\"?a=")
			.append(DOWN_TXT).append("&bid=0\">����txt</a><br>\n<ol>\n");
		List<Map<String, Object>> data = FoxMemDBHelper.getBookList(oDB); // ��ȡ�鼮�б�
		Iterator<Map<String, Object>> itr = data.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			html.append("<li><a href=\"?a=").append(LIST_PAGES).append("&bid=")
				.append(mm.get("id")).append("\" title=\"").append(mm.get("url")).append("\">")
				.append(mm.get("name")).append("</a> (").append(mm.get("count")).append(")")
//				.append(" <a href=\"?a=").append(LIST_ONLINE_PAGES).append("&c=18&url=").append(mm.get("url")).append("\" class=\"yy\">ԭ</a>")
				.append(" <a href=\"?a=").append(DOWN_TXT).append("&bid=").append(mm.get("id")).append("\" class=\"yy\">T</a></li>\n");
		}
		html.append("\n</ol>\n").append(html_foot);
		return html.toString() ;
	}
	
	private String html_head(String title) {
		return html_head("utf-8", title, false);
	}
	private String html_head(String charset, String title, boolean addJS) {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n")
			.append("<html>\n")
			.append("<head>\n")
			.append("\t<META http-equiv=Content-Type content=\"text/html; charset=").append(charset).append("\">\n")
			.append("\t<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; minimum-scale=0.1; maximum-scale=3.0; \"/>\n")
			.append("\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=Edge\">\n")
			.append("\t<title>").append(title).append("</title>\n")
			.append("\t<style>\n\t\tli {line-height:200%;}\n\t\tli a:link { text-decoration: none; }\n\t\t.content { padding: 1em; }\n\t\t.content p { text-indent: 2em; }\n\t\tol {list-style-type: decimal-leading-zero;}\nol li {\n\tborder-bottom-width: 1px;\n\tborder-bottom-style: dashed;\n\tborder-bottom-color: #CCCCCC;\n}\n.yy {\n\tpadding:3px;\n\tbackground-color:#99cc66;\n\tcolor: #fff;\n\ttext-align: center;\n\tborder-radius: 4px;\n}\nbody, div, ul {\n\tmargin: 0;\n\tpadding: 0;\n\tlist-style: none;\n}\n.book_switch a, .book_switch a:active {\n\tcolor: #09b396;\n\tborder: 1px solid #09b396;\n\tborder-radius: 5px;\n\tdisplay: block;\n\tfont-size: .875rem;\n\tmargin-right: 10px;\n}\n.book_switch {\n\theight: 35px;\n\tline-height: 35px;\n\tpadding: 8px 10px 10px 10px;\n\ttext-align: center;\n}\n.book_switch ul li {\n\tfloat: left;\n\twidth: 25%;\n}\n\t</style>\n");
		if ( addJS ) {
			html.append("<script language=javascript>\n")
			.append("function BS(colorString) {document.bgColor=colorString;}\n")
			.append("function mouseClick(ev){\n")
			.append("\tev = ev || window.event;\n")
			.append("\ty = ev.clientY;\n")
			.append("\th = window.innerHeight || document.documentElement.clientHeight;\n")
			.append("\tif ( y > ( 0.3 * h ) ) {\n")
			.append("\t\twindow.scrollBy(0, h - 20);\n")
			.append("\t} else {\n")
			.append("\t\twindow.scrollBy(0, -h + 20);\n")
			.append("\t}\n")
			.append("}\n")
			.append("document.onmousedown = mouseClick;\n")
			.append("document.onkeydown=function(event){\n")
			.append("\tvar e = event || window.event || arguments.callee.caller.arguments[0];\n")
			.append("\tif(e && (e.keyCode==32 || e.keyCode==81 )){\n")
			.append("\t\th = window.innerHeight || document.documentElement.clientHeight;\n")
			.append("\t\twindow.scrollBy(0, h - 20);\n")
			.append("\t}\n")
			.append("};\n")
			.append("</script>\n");
		}
		html.append("</head>\n")
			.append("<body bgcolor=\"#eefaee\">\n\n");
		return html.toString();
	}
	
}

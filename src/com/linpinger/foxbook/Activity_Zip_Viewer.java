package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class Activity_Zip_Viewer extends ListActivity_Eink {
	
	public final int ZIP = 26 ;        // ��ͨzip�ļ�
	public final int ZIP1024 = 1024 ;  // 1024 html �����zip
	public final int EPUB = 500 ;      // ��ͨ epub�ļ� ����ʽ����
	public final int EPUBQIDIAN = 517; // ��� epub
	
	private int ZIPTYPE = ZIP ;  // �����zip/epub����
	
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	private String zipPath ;
	public FoxMemDB oDB  ; // Ĭ��ʹ��MemDB
	private int tmpBookID = 0;
	
	SharedPreferences settings;

	private void renderListView() { // ˢ��LV
		adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_pagelist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_pagelist.setAdapter(adapter);
	}
	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(settings.getBoolean("isClickHomeExit", false));  // ����������ӷ���ͼ��
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_zip_viewer);
		showHomeUp();
		
		lv_pagelist = getListView();
		
		// ��ȡ������ļ�·��
		Intent itt = getIntent();
		zipPath = itt.getData().getPath(); // ��intent��ȡzip/epub·��
		if ( zipPath.toLowerCase().endsWith(".epub"))
			ZIPTYPE = EPUB ;
		if ( zipPath.toLowerCase().endsWith(".zip"))
			ZIPTYPE = ZIP ;
		File nowDB3 = new File(zipPath.replace(".zip", "").replace(".epub", "") + ".db3");
		if ( nowDB3.exists() ) { // ���ڣ���������һ��
			File bakFile = new File(zipPath.replace(".zip", "").replace(".epub", "") + "_" + System.currentTimeMillis() + ".db3");
			nowDB3.renameTo(bakFile);
			foxtip("���ݿ���ڣ�������Ϊ:\n" + bakFile.getName());
		}
		oDB = new FoxMemDB(nowDB3, this.getApplicationContext()) ; // �����ڴ����ݿ�
		
		setTitle(zipPath);
		reimport();
	
	}
	
	private void reimport() {
		switch (ZIPTYPE) {
		case ZIP:
		case ZIP1024:
			// TODO: ��ͨzip����
			
			ZIPTYPE = ZIP1024 ;
			tmpBookID = FoxMemDBHelper.insertbook("zip", "http://127.0.0.1/", "5", oDB);
			FoxZipReader z = new FoxZipReader(new File(zipPath));
			data = z.getList();
			z.close();
			renderListView();  // �����data����ˢ���б�
			break;
		case EPUB:
		case EPUBQIDIAN:
			FoxEpubReader epub = new FoxEpubReader(new File(zipPath));
			
			if ( epub.getFileContent("catalog.html").length() == 0 ) { // ����� epub
				ZIPTYPE = EPUB ;
				System.out.println("Todo: �����epub��ȡ");
				return ;
			}
			FoxMemDBHelper.importQidianEpub(epub, oDB);
			
			epub.close();
			data = FoxMemDBHelper.getPageList("", oDB); // ��ȡҳ���б�

			renderListView();  // �����data����ˢ���б�
			break;

		default:
			break;
		}

	}
	
    public static HashMap<String, Object> page1024(String html) {
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
//    			.replace("\n ����", "\n")
//    			.replace("\n  ", "\n")
//    			.replace("\n����", "\n")
//    			.replace("\n  ", "\n")
    	Matcher mat = Pattern.compile("(?smi)\"tpc_content\">(.*?)</td>").matcher(html);
    	while (mat.find()) {
    		text = text + mat.group(1) + "\n-----#####-----\n" ;
    	}
		oM.put("content", text);
		
		return oM ;
	}
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> chapinfo = (HashMap<String, Object>) data.get(position);
		String tmpname = (String) chapinfo.get("name");
		String tmpurl = (String) chapinfo.get("url");
		Integer tmpid = (Integer) chapinfo.get("id");
		
		String pagename = "";
		String pageurl = "";
		int pageid = 0;

		switch (ZIPTYPE) {
		case ZIP:
		case ZIP1024:
			long sTime = System.currentTimeMillis();
			String lowname = tmpname.toLowerCase() ;
			// ����html ��ȡ���⣬����
			String html = "";
			HashMap<String, Object> cc = new HashMap<String, Object>();
			if ( lowname.endsWith(".html") | lowname.endsWith(".htm") | lowname.endsWith(".txt") ) {
				FoxZipReader z = new FoxZipReader(new File(zipPath));
				html = z.getHtmlFile(tmpname, "UTF-8");
				z.close();
				if ( html.contains("\"tpc_content\"") )
					cc = page1024(html);
			} else {
				foxtip("�ݲ�֧�����ָ�ʽ�Ĵ���");
				return ;
			}

			pagename = cc.get("title").toString();
			String content = cc.get("content").toString();

			setTitle(tmpname + " " + (System.currentTimeMillis() - sTime) + "ms " + html.length() + "B " + pagename);
			
			// д��RamDB
			oDB.execSQL("delete from page");
			
	        ContentValues xxx = new ContentValues();
	        xxx.put("BookID", tmpBookID);
	        xxx.put("Name", pagename);
	        xxx.put("URL", tmpname);
	        xxx.put("CharCount", content.length());
	        xxx.put("Content", content);
	        xxx.put("DownTime", 11111);
	        xxx.put("Mark", "text");
	        
	        pageid = (int)oDB.getDB().insert("page", null, xxx);
	        pageurl = tmpname ;
			break;

		case EPUB:
		case EPUBQIDIAN:
			pageid = tmpid;
			pagename = tmpname;
			pageurl = tmpurl;
			break;

		default:
			break;
		}

		
		// �����Ķ�ҳ
		Intent intent ;
		if ( settings.getBoolean("isUseNewPageView", true) ) {
			intent = new Intent(Activity_Zip_Viewer.this, Activity_ShowPage4Eink.class);
			Activity_ShowPage4Eink.oDB = oDB;
		} else {
			intent = new Intent(Activity_Zip_Viewer.this, Activity_ShowPage.class);
			Activity_ShowPage.oDB = oDB;
		}
		intent.putExtra("iam", SITES.FROM_DB); // from DB
		intent.putExtra("chapter_id", pageid);
		intent.putExtra("chapter_name", pagename);
		intent.putExtra("chapter_url", pageurl);
		intent.putExtra("searchengine", SITES.SE_BING); // SE
		startActivity(intent);
	}

	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.zip_viewer, menu);
//		int itemcount = menu.size();
//		for ( int i=0; i< itemcount; i++){
//			switch (menu.getItem(i).getItemId()) {
//			case R.id.is_newimportqdtxt:
//				break;
//			}
//		}

		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
			System.exit(0);
			break;
		case R.id.action_save_exit:
			oDB.closeMemDB();
			this.finish();
			System.exit(0);
			break;
		case R.id.jumplist_tobottom:
			lv_pagelist.setSelection(adapter.getCount() - 1);
			setItemPos4Eink(adapter.getCount() - 1);
			break;
		case R.id.jumplist_totop:
			lv_pagelist.setSelection(0);
			setItemPos4Eink(); // ����λ�÷ŵ�ͷ��
			break;
		case R.id.jumplist_tomiddle:
			int midPos = adapter.getCount() / 2 - 1 ;
			lv_pagelist.setSelection(midPos);
			setItemPos4Eink(midPos);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}

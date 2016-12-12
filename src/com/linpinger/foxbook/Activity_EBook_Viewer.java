package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class Activity_EBook_Viewer extends ListActivity_Eink {
	
	public final int ZIP = 26 ;        // ��ͨzip�ļ�
	public final int ZIP1024 = 1024 ;  // 1024 html �����zip
	public final int EPUB = 500 ;      // ��ͨ epub�ļ� ����ʽ����
	public final int EPUBQIDIAN = 517; // ��� epub
	public final int TXT = 200 ;       // ��ͨtxt
	public final int TXTQIDIAN = 217 ; // ���txt
	
	private int EBOOKTYPE = ZIP ;  // �����zip/epub����
	
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	public FoxMemDB oDB  ; // Ĭ��ʹ��MemDB

	private String eBookPath ;
	private int tmpBookID = 0; // ��ȡzip���õ�
	private boolean isNewImportQDtxt = true ; // ʹ���°����txt���뷽��

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
		setContentView(R.layout.activity_ebook_viewer);
		showHomeUp();
		
		lv_pagelist = getListView();
		
		isNewImportQDtxt = settings.getBoolean("isNewImportQDtxt", isNewImportQDtxt);
		
		// ��ȡ������ļ�·��
		Intent itt = getIntent();
		eBookPath = itt.getData().getPath(); // ��intent��ȡtxt/zip/epub·��
		if ( eBookPath.toLowerCase().endsWith(".epub"))
			EBOOKTYPE = EPUB ;
		if ( eBookPath.toLowerCase().endsWith(".zip"))
			EBOOKTYPE = ZIP ;
		if ( eBookPath.toLowerCase().endsWith(".txt"))
			EBOOKTYPE = TXT ;
		
		String eBookPathWithoutExt = eBookPath.replace(".zip", "").replace(".epub", "").replace(".txt", "") ; // Bug: ���Ϊ.Txt, .ZIP �Ϳ���
		File DB3File = new File(eBookPathWithoutExt + ".db3");
		if ( DB3File.exists() ) { // ���ڣ���������һ��
			File bakFile = new File(eBookPathWithoutExt + "_" + System.currentTimeMillis() + ".db3");
			DB3File.renameTo(bakFile);
			foxtip("���ݿ���ڣ�������Ϊ:\n" + bakFile.getName());
		}
		oDB = new FoxMemDB(DB3File, this.getApplicationContext()) ; // �����ڴ����ݿ�
		
		setTitle(eBookPath);
		reimport();
	
	}
	
	private void reimport() {
		switch (EBOOKTYPE) {
		case ZIP:
		case ZIP1024:
			// TODO: ��ͨzip����
			EBOOKTYPE = ZIP1024 ;
			tmpBookID = FoxMemDBHelper.insertbook("zip", "http://127.0.0.1/", "5", oDB);
			FoxZipReader z = new FoxZipReader(new File(eBookPath));
			data = z.getList();
			z.close();
			renderListView();  // �����data����ˢ���б�
			break;
		case EPUB:
		case EPUBQIDIAN:
			FoxEpubReader epub = new FoxEpubReader(new File(eBookPath));
			
			if ( epub.getFileContent("catalog.html").length() == 0 ) { // ����� epub
				EBOOKTYPE = EPUB ;
				System.out.println("Todo: �����epub��ȡ");
				return ;
			}
			FoxMemDBHelper.importQidianEpub(epub, oDB);
			
			epub.close();
			data = FoxMemDBHelper.getPageList("", oDB); // ��ȡҳ���б�

			renderListView();  // �����data����ˢ���б�
			break;
		case TXT:
		case TXTQIDIAN:
//			oDB.execSQL("delete from book"); oDB.execSQL("delete from page");
			setTitle(FoxMemDBHelper.importQidianTxt(eBookPath, oDB, isNewImportQDtxt));
			data = FoxMemDBHelper.getPageList("", oDB); // ��ȡҳ���б�
			renderListView();  // �����data����ˢ���б�
			break;
		default:
			break;
		}

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

		switch (EBOOKTYPE) {
		case ZIP:
		case ZIP1024:
			long sTime = System.currentTimeMillis();
			String lowname = tmpname.toLowerCase() ;
			// ����html ��ȡ���⣬����
			String html = "";
			HashMap<String, Object> cc = new HashMap<String, Object>();
			if ( lowname.endsWith(".html") | lowname.endsWith(".htm") | lowname.endsWith(".txt") ) {
				FoxZipReader z = new FoxZipReader(new File(eBookPath));
				html = z.getHtmlFile(tmpname, "UTF-8");
				z.close();
				if ( html.contains("\"tpc_content\"") )
					cc = FoxBookLib.getPage1024(html);
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
		case TXT:
		case TXTQIDIAN:
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
			intent = new Intent(Activity_EBook_Viewer.this, Activity_ShowPage4Eink.class);
			Activity_ShowPage4Eink.oDB = oDB;
		} else {
			intent = new Intent(Activity_EBook_Viewer.this, Activity_ShowPage.class);
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
		getMenuInflater().inflate(R.menu.ebook_viewer, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
			case R.id.is_newimportqdtxt:
				if ( EBOOKTYPE == TXT || EBOOKTYPE == TXTQIDIAN) {
					menu.getItem(i).setVisible(true) ; // ��ʾ
					if ( isNewImportQDtxt )
						menu.getItem(i).setTitle("�ɷ����������txt");
					else
						menu.getItem(i).setTitle("�·����������txt");
				} else {
					menu.getItem(i).setVisible(false) ; // ��txt����
				}
				break;
			case R.id.action_gbk2utf8:
				if ( EBOOKTYPE == TXT || EBOOKTYPE == TXTQIDIAN)
					menu.getItem(i).setVisible(true) ; // ��ʾ
				else
					menu.getItem(i).setVisible(false) ; // ��txt����
				break;
			}
		}

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
		case R.id.action_gbk2utf8:  // Txt GBK->UTF-8
			FoxMemDBHelper.all2txt("all", oDB, eBookPath.replace(".txt", "") + "_UTF8.txt");
			oDB.getDB().close();
			this.finish();
			System.exit(0);
			break;
		case R.id.is_newimportqdtxt: // �¾ɷ�ʽ���µ������txt
			isNewImportQDtxt = ! isNewImportQDtxt ;
			if (isNewImportQDtxt)
				item.setTitle("�ɷ����������txt");
			else
				item.setTitle("�·����������txt");
			reimport();
			Editor editor = settings.edit();
			editor.putBoolean("isNewImportQDtxt", isNewImportQDtxt);
			editor.commit();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}

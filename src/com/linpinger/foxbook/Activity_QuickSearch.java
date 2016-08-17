package com.linpinger.foxbook;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class Activity_QuickSearch extends ListActivity {
	public static FoxMemDB oDB;
	private ListView lv_sitelist ;
	SimpleAdapter adapter;
	private List<Map<String, Object>> data;
	private Handler handler;
	private static int IS_REFRESH = 5 ;
	
	SharedPreferences settings;
	public static final String FOXSETTING = "FOXSETTING";
	private boolean isEink = false; // �Ƿ�E-ink�豸

	private String book_name = "" ;
	private String book_url = "" ;
	
	private int SE_TYPE = 1; // ��������
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
//		getActionBar().setDisplayShowHomeEnabled(false); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home   this.finish();
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		settings = getSharedPreferences(FOXSETTING, 0);
		isEink = settings.getBoolean("isEink", isEink);
		if ( isEink ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_quicksearch);
		
		lv_sitelist = getListView();
		
		Intent itt = getIntent();
		book_name = itt.getStringExtra("bookname"); // ����
		SE_TYPE = itt.getIntExtra("searchengine", 1) ;
		
		setTitle("����: " + book_name);
		
		data = new ArrayList<Map<String, Object>>(10);
		renderListView();
		
		init_handler() ; // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		
		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
		
		String seURL = "" ;
		try {
			switch (SE_TYPE) { // 1:sogou 2:yahoo 3:bing
			case SITES.SE_SOGOU:
				seURL = "http://www.sogou.com/web?query=" + URLEncoder.encode(book_name, "GB2312") + "&num=50" ;
				break;
			case SITES.SE_YAHOO:
				seURL = "http://search.yahoo.com/search?n=40&p=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			case SITES.SE_BING:
				seURL = "http://cn.bing.com/search?q=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		new Thread(new DownTOC(seURL)).start();
	}
	
	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				book_url = (String) chapinfo.get("url");
				Intent intent = new Intent(Activity_QuickSearch.this, Activity_PageList.class);
				intent.putExtra("iam", SITES.FROM_NET);
				intent.putExtra("bookurl", book_url);
				intent.putExtra("bookname", book_name);
				intent.putExtra("searchengine", SE_TYPE);
				Activity_PageList.oDB = oDB;
				startActivity(intent);
			}
		};
		lv_sitelist.setOnItemClickListener(listener);
	}

	
	public class DownTOC implements Runnable { // ��̨�߳�������ҳ
		private String bookurl = "";
		
		public DownTOC(String inbookurl){
			this.bookurl = inbookurl;
		}
		@Override
		public void run() {
			Message msg = Message.obtain();
			switch(SE_TYPE) {
			default :
				msg.what = IS_REFRESH;
				msg.obj = FoxBookLib.downhtml(this.bookurl);
			}
			handler.sendMessage(msg);
		}
	}

	private void renderListView() { // ˢ��LV
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_2, new String[] { "name", "url" },
				new int[] { android.R.id.text1, android.R.id.text2 });
		lv_sitelist.setAdapter(adapter);
	}
	
	private void init_handler() { // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_REFRESH ) { // �������
					String sHTTP = (String)msg.obj;				
					switch(SE_TYPE) {
					default :
						data = FoxBookLib.getSearchEngineHref(sHTTP, book_name); // ����������ҳ������������
					}
					renderListView();
				}
			}
		};
	}

/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.search, menu);
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case R.id.sm_QuickSearchSouGou: // ����:�ѹ�
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
*/


}

package com.linpinger.foxbook;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class Activity_QuickSearch extends ListActivity {
	private ListView lv_sitelist ;
	SimpleAdapter adapter;
	private List<Map<String, Object>> data;
	private Handler handler;
	private static int IS_REFRESH = 5 ;
	
	private String book_name = "" ;
	private String book_url = "" ;
	private String html = "";
	
	private static int FROM_NET = 2 ; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_quicksearch);
		
		lv_sitelist = getListView();
		
		Intent itt = getIntent();
		book_name = itt.getStringExtra("bookname"); // ����
		
		setTitle("����: " + book_name);
		
		data = new ArrayList<Map<String, Object>>(64);
		renderListView();
		
		init_handler() ; // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		
		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
		
		String gbURL = "" ;
		try {
			gbURL = "http://www.sogou.com/web?query=" + URLEncoder.encode(book_name, "GB2312") + "&num=50" ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		new Thread(new DownTOC(gbURL)).start();
	}
	
	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				book_url = (String) chapinfo.get("url");
				Intent intent = new Intent(Activity_QuickSearch.this,
						Activity_PageList.class);
				intent.putExtra("iam", FROM_NET);
				intent.putExtra("bookurl", book_url);
				intent.putExtra("bookname", book_name);
				intent.putExtra("bShowAll", false);
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
			String html = FoxBookLib.downhtml(this.bookurl);
	        Message msg = Message.obtain();
	        msg.what = IS_REFRESH;
	        msg.obj = html;
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
					html = (String)msg.obj;
//					Log.e("XX1", html);
					data = FoxBookLib.getSearchEngineHref(html, book_name); // ����������ҳ������������
					
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

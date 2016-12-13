package com.linpinger.foxbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.AlertDialog;
// import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class Activity_PageList extends Ext_ListActivity_4Eink {
	public static FoxMemDB oDB;
	
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // ��ɫ������
	private boolean isUseNewPageView = true; // ʹ���µ��Զ���View

	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	
	SimpleAdapter adapter;
	private Handler handler;

	private static int IS_UPDATEPAGE = 88;
	private static int IS_DOWNTOC = 5;
	private final int IS_QIDIAN_MOBILE = 16;
	
	private int foxfrom = 0; // 1=DB, 2=search
	private String bookurl = "";
	private String bookname = "";
	private int bookid = 0 ;
	private String lcURL, lcName;
	private Integer lcID;
	private int longclickpos = 0;
	
	private int SE_TYPE = 1; // ��������
	

	public class DownTOC implements Runnable { // ��̨�߳�������ҳ
		@Override
		public void run() {
			Message msg = Message.obtain();
			switch(SE_TYPE) {
			case SITES.SE_QIDIAN_MOBILE : // ����ֻ���Ŀ¼
				msg.what = IS_QIDIAN_MOBILE;
				msg.obj = ToolBookJava.downhtml(bookurl, "utf-8") ;
				break;
			default:
				msg.what = IS_DOWNTOC;
				msg.obj = ToolBookJava.downhtml(bookurl);
				break;
			}
			handler.sendMessage(msg);
		}
	}

	private void renderListView() { // ˢ��LV
		if (SITES.FROM_DB == foxfrom || SITES.FROM_ZIP == foxfrom) {
			adapter = new SimpleAdapter(this, data, R.layout.lv_item_pagelist,
					new String[] { "name", "count" }, new int[] { R.id.tvName, R.id.tvCount });
			lv_pagelist.setAdapter(adapter);
		} else {
			adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_1,
					new String[] { "name" }, new int[] { android.R.id.text1 });
			lv_pagelist.setAdapter(adapter);
			lv_pagelist.setSelection(adapter.getCount() - 1); // �����б�����β��
		}
	}

	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");
				
				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent ;
				isUseNewPageView = settings.getBoolean("isUseNewPageView", isUseNewPageView);
				if ( isUseNewPageView ) {
					intent = new Intent(Activity_PageList.this, Activity_ShowPage4Eink.class);
					Activity_ShowPage4Eink.oDB = oDB;
				} else {
					intent = new Intent(Activity_PageList.this, Activity_ShowPage.class);
					Activity_ShowPage.oDB = oDB;
				}
				intent.putExtra("iam", foxfrom);
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", ToolBookJava.getFullURL(bookurl, tmpurl));
				intent.putExtra("searchengine", SE_TYPE);

				if ( foxfrom == SITES.FROM_ZIP )
					intent.putExtra("chapter_url", bookurl + "@" + tmpurl);

				startActivity(intent);
			}
		};
		lv_pagelist.setOnItemClickListener(listener);
	}
	
	private void init_LV_item_Long_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ; // base 0

				if ( foxfrom == SITES.FROM_NET ) { // ������������ʱ����Ŀû��ID
					foxtip("������������ʱ����Ŀû��ID");
					return true;
				}

				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcID = (Integer) chapinfol.get("id");

				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("����:" + lcName);
				builder.setItems(new String[] { "ɾ������", "ɾ�����²���д��Dellist", "ɾ�����¼�����", "ɾ�����¼����ϲ���д��Dellist", "ɾ�����¼�����", "ɾ�����¼����²���д��Dellist", "���±���" },
						new DialogInterface.OnClickListener() {
							@TargetApi(Build.VERSION_CODES.HONEYCOMB)
							public void onClick(DialogInterface dialog,	 int which) {
								switch (which) {
								case 0:
									FoxMemDBHelper.delete_Pages(lcID, true, oDB);
									data.remove(longclickpos); // λ�ÿ��ܲ�̫����
									adapter.notifyDataSetChanged();
									foxtip("��ɾ������¼: " + lcName);
									break;
								case 1:
									FoxMemDBHelper.delete_Pages(lcID, false, oDB);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
									foxtip("��ɾ��: " + lcName);
									break;
								case 2:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, true, true, oDB);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("��ɾ������¼: <= " + lcName);
									setItemPos4Eink(); // ����λ�÷ŵ�ͷ��
									break;
								case 3:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, true, false, oDB);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("��ɾ��: <= " + lcName);
									setItemPos4Eink(); // ����λ�÷ŵ�ͷ��
									break;
								case 4:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, false, true, oDB);
									int datasiza = data.size();
									for ( int i = longclickpos; i<datasiza; ++i) {
										data.remove(longclickpos);
									}
									adapter.notifyDataSetChanged();
									foxtip("��ɾ������¼: >= " + lcName);
									break;
								case 5:
									FoxMemDBHelper.delete_nowupdown_Pages(lcID, false, false, oDB);
									int datasizb = data.size();
									for ( int i = longclickpos; i<datasizb; ++i) {
										data.remove(longclickpos);
									}
									foxtip("��ɾ��: >= " + lcName);
									adapter.notifyDataSetChanged();
									break;
								case 6:  // �����½�
									setTitle("���ڸ���: " + lcName);
									(new Thread(){
										public void run(){
											FoxMemDBHelper.updatepage(lcID, oDB);
									        handler.sendEmptyMessage(IS_UPDATEPAGE);
										}
									}).start();
									break;
								} // switch end
								if ( data.size() == 0 )  // ����¼ɾ����󣬽�����Activity
									onBackPressed();
							} // onClick end
						});
				builder.create().show();
				return true;
			}

		};
		lv_pagelist.setOnItemLongClickListener(longlistener);
	}
	
	private void init_handler() { // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		handler = new Handler() {
			public void handleMessage(Message msg) {
				String sHTTP = (String)msg.obj;
				if ( msg.what == IS_UPDATEPAGE ) { // �����½����
					setTitle("������� : " + lcName);
				}
				if ( msg.what == IS_QIDIAN_MOBILE ) {
					data = site_qidian.json2PageList(sHTTP);
					renderListView();
				}
				
				if ( msg.what == IS_DOWNTOC ) { // ����Ŀ¼���
					data = ToolBookJava.tocHref(sHTTP, 0);
					renderListView();
				}
			}
		};
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
		getActionBar().setDisplayShowHomeEnabled(false); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home   this.finish();
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pagelist);

		showHomeUp();
		lv_pagelist = getListView();

		// ��ȡ���������
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0); // ���� �������ݴ�������
		bookurl = itt.getStringExtra("bookurl"); // ����
		bookname = itt.getStringExtra("bookname"); // ����
		SE_TYPE = itt.getIntExtra("searchengine", 1) ; // ����������������

		setTitle(bookname + " : " + bookurl);

		if ( bookurl.startsWith("zip://"))
			foxfrom = SITES.FROM_ZIP ;

		init_handler() ; // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
 
		switch (foxfrom) {
		case SITES.FROM_NET:
			String html = itt.getStringExtra("html");
			if (null == html) { // û�����Լ�����
				new Thread(new DownTOC()).start();
				html = "";
			}
			if ( bookurl.contains("3g.if.qidian.com") ) { // ����ҳ����������ַ
				data = site_qidian.json2PageList(html);
			} else {
				data = ToolBookJava.tocHref(html, 0);
			}
			break;
		case SITES.FROM_DB:
			bookid = itt.getIntExtra("bookid", 0);
			data = FoxMemDBHelper.getPageList("where bookid=" + bookid, oDB); // ��ȡҳ���б�
			break;
		case SITES.FROM_ZIP:
			bookid = itt.getIntExtra("bookid", 0);
			data = FoxMemDBHelper.getPageList("where bookid=" + bookid, 26, oDB); // ��ȡҳ���б�
			break;
		default:
			break;
		}

		renderListView();

		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
		init_LV_item_Long_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
	}


	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.pagelist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.pm_Add:
					if ( SITES.FROM_DB == foxfrom)  // ���Ǳ������ݿ�ʱ������Ӱ�ť
						menu.getItem(i).setVisible(false);
					break;
				case R.id.pm_cleanBook:
				case R.id.pm_cleanBookND:
					if ( SITES.FROM_NET == foxfrom ) // ��������ʱ����ɾ����ť
						menu.getItem(i).setVisible(false);
					break;
			}
		}

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧ�˵�
		switch (item.getItemId()) {
		case android.R.id.home: // ����ͼ��
			onBackPressed();
			break;
		case R.id.pl_finish:
			onBackPressed();
			break;
		case R.id.pm_cleanBook:
			FoxMemDBHelper.delete_Book_All_Pages(bookid, true, oDB);
			foxtip("��ɾ�������¼�¼");
			onBackPressed();
			break;
		case R.id.pm_cleanBookND:
			FoxMemDBHelper.delete_Book_All_Pages(bookid, false, oDB);
			foxtip("��ɾ��");
			onBackPressed();
			break;
		case R.id.pm_Add:
			if ( SITES.FROM_NET == foxfrom ) {
				if ( null != bookurl && "" != bookname ) {
					int nBookID = 0 ;
					// ���������ݿ⣬����ȡ����bookid
					nBookID = FoxMemDBHelper.insertbook(bookname, bookurl, null, oDB);
					if ( nBookID < 1 )
						break ;
					Intent itti = new Intent(Activity_PageList.this, Activity_BookInfo.class);
					itti.putExtra("bookid", nBookID);
					Activity_BookInfo.oDB = oDB;
					startActivity(itti);
					onBackPressed();
				} else {
					setTitle("��Ϣ������@���� : " + bookname + " <" + bookurl + ">");
				}
			}
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
	
	@Override
	public void onBackPressed() { // ���ؼ�����
		setResult(RESULT_OK);
		finish();
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}

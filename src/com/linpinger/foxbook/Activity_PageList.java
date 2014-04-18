package com.linpinger.foxbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class Activity_PageList extends ListActivity {
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	
	SimpleAdapter adapter;
	private Handler handler;

	private static int IS_UPDATEPAGE = 88;
	private static int IS_DOWNTOC = 5;
	private static int FROM_DB = 1 ;
	private static int FROM_NET = 2 ; 
	private int foxfrom = 0; // 1=DB, 2=search
	private String bookurl = "";
	private String bookname = "";
	private String html = "";
 	private boolean bShowAll = false;
	private int bookid = 0 ;
	private String lcURL, lcName;
	private Integer lcID;
	private int longclickpos = 0;

	public class DownTOC implements Runnable { // ��̨�߳�������ҳ
		@Override
		public void run() {
			String html = FoxBookLib.downhtml(bookurl);
	        Message msg = Message.obtain();
	        msg.what = IS_DOWNTOC;
	        msg.obj = html;
	        handler.sendMessage(msg);
		}
	}

	private void renderListView() { // ˢ��LV
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_1, new String[] { "name" },
				new int[] { android.R.id.text1 });
		lv_pagelist.setAdapter(adapter);
	}

	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");

				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent = new Intent(Activity_PageList.this,
						Activity_ShowPage.class);
				intent.putExtra("iam", foxfrom);
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", FoxBookLib.getFullURL(bookurl, tmpurl));
				startActivity(intent);
			}
		};
		lv_pagelist.setOnItemClickListener(listener);
	}
	
	private void init_LV_item_Long_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ; // base 0

				if ( foxfrom == FROM_NET ) { // ������������ʱ����Ŀû��ID
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
									FoxDB.delete_Pages(lcID, true);
									data.remove(longclickpos); // λ�ÿ��ܲ�̫����
									adapter.notifyDataSetChanged();
									foxtip("��ɾ������¼: " + lcName);
									break;
								case 1:
									FoxDB.delete_Pages(lcID, false);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
									foxtip("��ɾ��: " + lcName);
									break;
								case 2:
									FoxDB.delete_nowupdown_Pages(lcID, true, true);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("��ɾ������¼: <= " + lcName);
									break;
								case 3:
									FoxDB.delete_nowupdown_Pages(lcID, true, false);
									for ( int i = 0; i<=longclickpos; ++i) {
										data.remove(0);
									}
									adapter.notifyDataSetChanged();
									foxtip("��ɾ��: <= " + lcName);
									break;
								case 4:
									FoxDB.delete_nowupdown_Pages(lcID, false, true);
									int datasiza = data.size();
									for ( int i = longclickpos; i<datasiza; ++i) {
										data.remove(longclickpos);
									}
									adapter.notifyDataSetChanged();
									foxtip("��ɾ������¼: >= " + lcName);
									break;
								case 5:
									FoxDB.delete_nowupdown_Pages(lcID, false, false);
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
											FoxBookLib.updatepage(lcID);
									        handler.sendEmptyMessage(IS_UPDATEPAGE);
										}
									}).start();
									break;
								}
							}
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
				if ( msg.what == IS_UPDATEPAGE ) { // �����½����
					setTitle("�ڸ������ : " + lcName);
				}
				if ( msg.what == IS_DOWNTOC ) { // ����Ŀ¼���
					html = (String)msg.obj;
					if ( bShowAll ) {
						data = FoxBookLib.tocHref(html, 0);
					} else {
						data = FoxBookLib.tocHref(html, 16);
					}
					renderListView();
				}
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pagelist);

		lv_pagelist = getListView();

		// ��ȡ���������
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0); // ���� �������ݴ�������
		bookurl = itt.getStringExtra("bookurl"); // ����
		bookname = itt.getStringExtra("bookname"); // ����
		bShowAll = itt.getBooleanExtra("bShowAll", false);

		setTitle(bookname + " : " + bookurl);

		init_handler() ; // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
 
		if ( FROM_NET == foxfrom ) {
			html = itt.getStringExtra("html");
			if (null == html) { // û�����Լ�����
				html = "";
				new Thread(new DownTOC()).start();
			}
			data = FoxBookLib.tocHref(html, 16);
		}
		if ( FROM_DB == foxfrom) { // DB
			bookid = itt.getIntExtra("bookid", 0);
			data = FoxDB.getPageList("where bookid=" + bookid); // ��ȡҳ���б�
		}

		renderListView();

		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
		init_LV_item_Long_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
	}

	public boolean onCreateOptionsMenu(Menu menu) { // �˵���ʼ��
		menu.add(0, 1, 1, "ɾ�������½�");
		menu.add(0, 2, 2, "ɾ�������½��Ҳ��޸�DelList");
		menu.add(0, 3, 3, "��ӱ���");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧ�˵�
		switch (item.getItemId()) {
		case 1:
			FoxDB.delete_Book_All_Pages(bookid, true);
			foxtip("��ɾ�������¼�¼");
			setResult(RESULT_OK, (new Intent()).setAction("�����ĳ�鲢д����ɾ���б�"));
			finish();
			break;
		case 2:
			FoxDB.delete_Book_All_Pages(bookid, false);
			foxtip("��ɾ��");
			setResult(RESULT_OK, (new Intent()).setAction("�����ĳ�鲢û��д����ɾ���б�"));
			finish();
			break;
		case 3:
			if ( FROM_NET == foxfrom ) {
				if ( null != bookurl && "" != bookname ) {
					int nBookID = 0 ;
					// ���������ݿ⣬����ȡ����bookid
					nBookID = FoxDB.insertbook(bookname, bookurl);
					if ( nBookID < 1 )
						break ;
					Intent itti = new Intent(Activity_PageList.this, Activity_BookInfo.class);
					itti.putExtra("bookid", nBookID);
					startActivity(itti);
					setResult(RESULT_OK, (new Intent()).setAction("�����б�"));
					finish();
				} else {
					setTitle("��Ϣ������@���� : " + bookname + " <" + bookurl + ">");
				}
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public boolean onKeyDown(int keyCoder, KeyEvent event) { // ������Ӧ
		if (keyCoder == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_OK, (new Intent()).setAction("�����б�"));
			finish();
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}

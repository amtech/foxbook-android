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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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

// 2016-2-17: �� Activity ֻ�� BookList ����
public class Activity_AllPageList extends ListActivity {
	public static FoxMemDB oDB; // ���������޸�
	
	public static final int SHOW_ALL = 1 ;  // ������Activity�ĵ����ߵ���
	public static final int SHOW_LESS1K = 2 ;
	private int showtypelist = SHOW_ALL ;
	
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // �Ƿ�E-ink�豸
	private boolean isUseNewPageView = true; // ʹ���µ��Զ���View

	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	private Handler handler;
	private static int IS_UPDATEPAGE = 88;

	// ������й���ʹ��
	private String lcURL, lcName;
	private Integer lcID ;
	private int longclickpos = 0;

	private void renderListView() { // ˢ��LV
		switch (showtypelist) {
		case SHOW_ALL:
			data = FoxMemDBHelper.getPageList("order by bookid,id", 1, oDB);
			break;
		case SHOW_LESS1K:
			data = FoxMemDBHelper.getPageList("where length(content) < 999 order by bookid,id", oDB);
			break;
		}
		adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_pagelist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_pagelist.setAdapter(adapter);
	}

	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");
				Integer tmpbid = (Integer) chapinfo.get("bookid");
				String bookurl = oDB.getOneCell("select url from book where id=" + tmpbid);

				setTitle(tmpname + " : " + tmpid);
				
				Intent intent;
				isUseNewPageView = settings.getBoolean("isUseNewPageView", isUseNewPageView);
				if ( isUseNewPageView ) {
					intent = new Intent(Activity_AllPageList.this, Activity_ShowPage4Eink.class);
					Activity_ShowPage4Eink.oDB = oDB;
				} else {
					intent = new Intent(Activity_AllPageList.this, Activity_ShowPage.class);
					Activity_ShowPage.oDB = oDB;
				}
				intent.putExtra("iam", SITES.FROM_DB);
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
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ;

				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcID = (Integer) chapinfol.get("id");

				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("����:" + lcName);
				builder.setItems(new String[] { "ɾ������", "ɾ�����²���д��Dellist", "ɾ�����¼�����", "ɾ�����¼�����", "���±���" },
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,  int which) {
								switch (which) {
								case 0:
									FoxMemDBHelper.delete_Pages(lcID, true, oDB);
									foxtip("��ɾ������¼: " + lcName);
									data.remove(longclickpos); // λ�ÿ��ܲ�̫����
									adapter.notifyDataSetChanged();
									showItemCountOnTitle();
									break;
								case 1:
									FoxMemDBHelper.delete_Pages(lcID, false, oDB);
									foxtip("��ɾ��: " + lcName);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
									showItemCountOnTitle();
									break;
								case 2:
									HashMap<String, Object> nHMa ;
									Integer nIDa;
									for ( int i = 0; i<=longclickpos; ++i) { // ɾ�����ݿ��¼
										nHMa = (HashMap<String, Object>) data.get(i);
										nIDa = (Integer) nHMa.get("id");
										FoxMemDBHelper.delete_Pages(nIDa, true, oDB);
									}
									for ( int i = 0; i<=longclickpos; ++i) { // ɾ�����ݽṹ
										data.remove(0);
									}
									adapter.notifyDataSetChanged(); // ֪ͨ���
									showItemCountOnTitle();
									foxtip("��ɾ������¼: <= " + lcName);
									break;
								case 3:
									HashMap<String, Object> nHMb ;
									Integer nIDb;
									int datasiza = data.size();
									for ( int i = longclickpos; i<datasiza; ++i) { // ɾ�����ݿ��¼
										nHMb = (HashMap<String, Object>) data.get(i);
										nIDb = (Integer) nHMb.get("id");
										FoxMemDBHelper.delete_Pages(nIDb, true, oDB);
									}
									for ( int i = longclickpos; i<datasiza; ++i) {
										data.remove(longclickpos);
									}
									adapter.notifyDataSetChanged();
									showItemCountOnTitle();
									foxtip("��ɾ������¼: >= " + lcName);
									break;
								case 4:
									setTitle("���ڸ���: " + lcName);
									(new Thread(){
										public void run(){
											FoxMemDBHelper.updatepage(lcID, oDB);
									        handler.sendEmptyMessage(IS_UPDATEPAGE);
										}
									}).start();
									break;
								}
								if ( data.size() == 0 ) { // ����¼ɾ����󣬽�����Activity
									exitMe(); // ������Activity
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
					setTitle("������� : " + lcName);
				}
			}
		};
	}
	
	private void showItemCountOnTitle() {
		setTitle("����: " + String.valueOf(data.size()));
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
//		getActionBar().setDisplayShowHomeEnabled(false); // ���س���ͼ��
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
		
		showtypelist = getIntent().getIntExtra("apl_showtype", SHOW_ALL); // ͨ��intent��ȡ����
		
		lv_pagelist = getListView();

		init_handler() ; // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		renderListView();
		showItemCountOnTitle();
		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
		init_LV_item_Long_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case android.R.id.home: // ����ͼ��
			exitMe();
			break;
		case R.id.allpagelist_delete: // ɾ�������½�
			HashMap<String, Object> nHMb ;
			Integer nIDb;
			int datasiza = data.size();
			for ( int i = 0; i<datasiza; ++i) { // ɾ�����ݿ��¼
				nHMb = (HashMap<String, Object>) data.get(i);
				nIDb = (Integer) nHMb.get("id");
				FoxMemDBHelper.delete_Pages(nIDb, true, oDB);
			}
			FoxMemDBHelper.simplifyAllDelList(oDB); // ��������DelList
			for ( int i = 0; i<datasiza; ++i) {
				data.remove(0);
			}
			adapter.notifyDataSetChanged();
			foxtip("��ɾ�����м�¼");
			exitMe(); // ������Activity
			break;
		case R.id.jumplist_tobottom:
			lv_pagelist.setSelection(adapter.getCount() - 1);
			break;
		case R.id.jumplist_totop:
			lv_pagelist.setSelection(0);
			break;
		case R.id.jumplist_tomiddle:
			lv_pagelist.setSelection((int)( 0.5 * ( adapter.getCount() - 1 ) ));
			break;
		case R.id.apl_finish:
			exitMe();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.allpagelist, menu);
		return true;
	}
	
	public boolean onKeyDown(int keyCoder, KeyEvent event) { // ������Ӧ
		if (keyCoder == KeyEvent.KEYCODE_BACK) {
			exitMe();
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	private void exitMe() { // ������Activity
		setResult(RESULT_OK, (new Intent()).setAction("�����б�"));
		this.finish();
	}
}

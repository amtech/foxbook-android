package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Activity_Qidian_Txt_Viewer extends ListActivity {
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	public FoxMemDB oDB  ; // Ĭ��ʹ��MemDB
	private String txtPath ;

	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // ��ɫ������
	private boolean isUseNewPageView = true; // ʹ���µ��Զ���View

	
	private void renderListView() { // ˢ��LV
		adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_pagelist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_pagelist.setAdapter(adapter);
	}
	
	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");

				Intent intent ;
				isUseNewPageView = settings.getBoolean("isUseNewPageView", isUseNewPageView);
				if ( isUseNewPageView ) {
					intent = new Intent(Activity_Qidian_Txt_Viewer.this, Activity_ShowPage4Eink.class);
					Activity_ShowPage4Eink.oDB = oDB;
				} else {
					intent = new Intent(Activity_Qidian_Txt_Viewer.this, Activity_ShowPage.class);
					Activity_ShowPage.oDB = oDB;
				}
				intent.putExtra("iam", SITES.FROM_DB); // from DB
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", tmpurl);
				intent.putExtra("searchengine", SITES.SE_BING); // SE
				startActivity(intent);
			}
		};
		lv_pagelist.setOnItemClickListener(listener);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(settings.getBoolean("isClickHomeExit", false));  // ����������ӷ���ͼ��
//		getActionBar().setDisplayShowHomeEnabled(isShowAppIcon); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home   this.finish();
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qidian_txt_viewver);
		showHomeUp();
		
		lv_pagelist = getListView();
		
		// ��ȡ������ļ�·��
		Intent itt = getIntent();
		txtPath = itt.getData().getPath(); // ��intent��ȡtxt·��
		File nowDB3 = new File(txtPath.replace(".txt", "") + ".db3");
		if ( nowDB3.exists() ) { // ���ڣ���������һ��
			File bakFile = new File(txtPath.replace(".txt", "") + "_" + System.currentTimeMillis() + ".db3");
			nowDB3.renameTo(bakFile);
			foxtip("���ݿ���ڣ�������Ϊ:\n" + bakFile.getName());
		}
		oDB = new FoxMemDB(nowDB3, this.getApplicationContext()) ; // �����ڴ����ݿ�
		foxtip("����: " + txtPath);
		String BookName = FoxMemDBHelper.importQidianTxt(txtPath, oDB); //����txt�����ݿ�
		setTitle(BookName);
		
		data = FoxMemDBHelper.getPageList("", oDB); // ��ȡҳ���б�
				
		renderListView();  // �����data����ˢ���б�
		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
	}
	
	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.qidian_txt_viewer, menu);
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
		case R.id.action_gbk2utf8:
			FoxMemDBHelper.all2txt("all", oDB, txtPath.replace(".txt", "") + "_UTF8.txt");
			oDB.getDB().close();
			this.finish();
			System.exit(0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}

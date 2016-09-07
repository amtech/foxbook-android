package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.ListActivity;
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

public class Activity_Qidian_Txt_Viewer extends ListActivity {
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	public FoxMemDB oDB  ; // Ĭ��ʹ��MemDB
	private String txtPath ;

	SharedPreferences settings;
	private boolean isNewImportQDtxt = true ; // ʹ���°����txt���뷽��

	private void renderListView() { // ˢ��LV
		adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_pagelist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_pagelist.setAdapter(adapter);
	}
	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(settings.getBoolean("isClickHomeExit", false));  // ����������ӷ���ͼ��
//		getActionBar().setDisplayShowHomeEnabled(isShowAppIcon); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home   this.finish();
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		isNewImportQDtxt = settings.getBoolean("isNewImportQDtxt", isNewImportQDtxt);
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
//		foxtip("����: " + txtPath);
		
		reimport(isNewImportQDtxt);
	}
	
	private void reimport(boolean isNew) {
		oDB.execSQL("delete from book");
		oDB.execSQL("delete from page");
		setTitle(FoxMemDBHelper.importQidianTxt(txtPath, oDB, isNew));
		data = FoxMemDBHelper.getPageList("", oDB); // ��ȡҳ���б�
		renderListView();  // �����data����ˢ���б�
	}
	
//	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
//	OnItemClickListener listener = new OnItemClickListener() {
//		@SuppressWarnings("unchecked")
//		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//			Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
//XXXXXXXXXXXXXXXXXXXXXXXXXXXX
//		}
//	};
//	lv_pagelist.setOnItemClickListener(listener);
//}
//
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> chapinfo = (HashMap<String, Object>) data.get(position);
		String tmpurl = (String) chapinfo.get("url");
		String tmpname = (String) chapinfo.get("name");
		Integer tmpid = (Integer) chapinfo.get("id");

		Intent intent ;
		if ( settings.getBoolean("isUseNewPageView", true) ) {
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

		// super.onListItemClick(l, v, position, id);
	}

	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.qidian_txt_viewer, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
			case R.id.is_newimportqdtxt:
				if ( isNewImportQDtxt )
					menu.getItem(i).setTitle("�ɷ����������txt");
				else
					menu.getItem(i).setTitle("�·����������txt");
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
		case R.id.action_gbk2utf8:
			FoxMemDBHelper.all2txt("all", oDB, txtPath.replace(".txt", "") + "_UTF8.txt");
			oDB.getDB().close();
			this.finish();
			System.exit(0);
			break;
		case R.id.is_newimportqdtxt: // �¾ɷ�ʽ���µ������txt
			isNewImportQDtxt = ! isNewImportQDtxt ;
			if (isNewImportQDtxt) {
				item.setTitle("�ɷ����������txt");
			} else {
				item.setTitle("�·����������txt");
			}
			reimport(isNewImportQDtxt);
			Editor editor = settings.edit();
			editor.putBoolean("isNewImportQDtxt", isNewImportQDtxt);
			editor.commit();
			break;
		case R.id.jumplist_tobottom:
			lv_pagelist.setSelection(adapter.getCount() - 1);
			break;
		case R.id.jumplist_totop:
			lv_pagelist.setSelection(0);
			break;
		case R.id.jumplist_tomiddle:
			lv_pagelist.setSelection((int)( 0.5 * ( adapter.getCount() - 1 ) ));
		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}

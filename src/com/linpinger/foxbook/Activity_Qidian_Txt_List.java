package com.linpinger.foxbook;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Activity_Qidian_Txt_List extends ListActivity {
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	SimpleAdapter adapter;
	public FoxMemDB oDB  ; // Ĭ��ʹ��MemDB
	
	private void renderListView() { // ˢ��LV
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_1, new String[] { "name" },
				new int[] { android.R.id.text1 });
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

				Intent intent = new Intent(Activity_Qidian_Txt_List.this, Activity_ShowPage.class);
				intent.putExtra("iam", FoxBookLib.FROM_DB); // from DB
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", tmpurl);
				intent.putExtra("searchengine", FoxBookLib.SE_BING); // SE
				Activity_ShowPage.oDB = oDB;
				startActivity(intent);
			}
		};
		lv_pagelist.setOnItemClickListener(listener);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qidian_txt_list);
		lv_pagelist = getListView();
		
		// ��ȡ������ļ�·��
		Intent itt = getIntent();
		String txtPath = itt.getData().getPath(); // ��intent��ȡtxt·��
		oDB = new FoxMemDB(new File(txtPath.replace(".txt", "") + ".db3"), this.getApplicationContext()) ; // �����ڴ����ݿ�
		String BookName = FoxMemDBHelper.importQidianTxt(txtPath, oDB); //����txt�����ݿ�
			
		foxtip("����:" + txtPath);
		setTitle(BookName);
		
		data = FoxMemDBHelper.getPageList("", oDB); // ��ȡҳ���б�
				
		renderListView();  // �����data����ˢ���б�
		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
	}
	
	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.db3_txt_viewer, menu);
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case R.id.action_save_exit:
			oDB.closeMemDB();
			this.finish();
			System.exit(0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}

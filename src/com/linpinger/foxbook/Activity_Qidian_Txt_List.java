package com.linpinger.foxbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
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
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Map<String, Object> chapinfo = (HashMap<String, Object>) parent.getItemAtPosition(position);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpid = (Integer) chapinfo.get("id");

				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent = new Intent(Activity_Qidian_Txt_List.this,
						Activity_ShowPage.class);
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
		
		// ��ȡ���������
		Intent itt = getIntent();
//		if ( itt.getScheme().equalsIgnoreCase("file") ) { // ����txt�ļ��Ķ�ȡ
		String txtPath = itt.getData().getPath(); // ��intent��ȡtxt·��
		
		oDB = new FoxMemDB(this.getApplicationContext()) ; // �����ڴ����ݿ�
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
}

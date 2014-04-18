package com.linpinger.foxbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class Activity_AllPageList extends ListActivity {
	private List<Map<String, Object>> data;
	private ListView lv_pagelist ;
	
	private int howmany=0;
	private int foxfrom = 1; // 1=DB, 2=search
	
	SimpleAdapter adapter;
	
	private String lcURL, lcName;
	private Integer lcID ;
	private int longclickpos = 0;

	private void renderListView() { // ˢ��LV
		if ( 0 == howmany ) {
			data = FoxDB.getPageList("order by bookid,id");
		} else {
			data = FoxDB.getPageList("order by bookid,id limit "+ howmany);
		}
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
				Integer tmpbid = (Integer) chapinfo.get("bookid");
				String bookurl = FoxDB.getOneCell("select url from book where id=" + tmpbid);

				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent = new Intent(Activity_AllPageList.this,
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
				@SuppressWarnings("unchecked")
				Map<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				longclickpos = position ;

				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcID = (Integer) chapinfol.get("id");

				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("����:" + lcName);
				builder.setItems(new String[] { "ɾ������", "ɾ�����²���д��Dellist" },
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,  int which) {
								switch (which) {
								case 0:
									FoxDB.delete_Pages(lcID, true);
									Toast.makeText(getApplicationContext(), "��ɾ������¼: " + lcName, Toast.LENGTH_SHORT).show();
									data.remove(longclickpos); // λ�ÿ��ܲ�̫����
									adapter.notifyDataSetChanged();
									break;
								case 1:
									FoxDB.delete_Pages(lcID, false);
									Toast.makeText(getApplicationContext(), "��ɾ��: " + lcName, Toast.LENGTH_SHORT).show();
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
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
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_allpagelist);
		
		lv_pagelist = getListView();
		
		// ��ȡ���������
		Intent itt = getIntent();
		howmany = itt.getIntExtra("howmany", 0); // ���� ���� ��ʾ������Ŀ, 0Ϊ����

		renderListView();
		init_LV_item_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
		init_LV_item_Long_click() ; // ��ʼ�� ���� ��Ŀ ����Ϊ
	}

	public boolean onKeyDown(int keyCoder, KeyEvent event) { // ������Ӧ
		if (keyCoder == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_OK, (new Intent()).setAction("�����б�"));
			finish();
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

}

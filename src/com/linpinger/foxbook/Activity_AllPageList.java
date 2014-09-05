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
	public static FoxMemDB oDB;
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
			data = FoxMemDBHelper.getPageList("order by bookid,id", oDB);
		} else {
			data = FoxMemDBHelper.getPageList("order by bookid,id limit "+ howmany, oDB);
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
				String bookurl = oDB.getOneCell("select url from book where id=" + tmpbid);

				// setTitle(parent.getItemAtPosition(position).toString());
				Intent intent = new Intent(Activity_AllPageList.this,
						Activity_ShowPage.class);
				intent.putExtra("iam", foxfrom);
				intent.putExtra("chapter_id", tmpid);
				intent.putExtra("chapter_name", tmpname);
				intent.putExtra("chapter_url", FoxBookLib.getFullURL(bookurl, tmpurl));
				Activity_ShowPage.oDB = oDB;
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
				builder.setItems(new String[] { "ɾ������", "ɾ�����²���д��Dellist", "ɾ�����¼�����", "ɾ�����¼�����" },
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,  int which) {
								switch (which) {
								case 0:
									FoxMemDBHelper.delete_Pages(lcID, true, oDB);
									foxtip("��ɾ������¼: " + lcName);
									data.remove(longclickpos); // λ�ÿ��ܲ�̫����
									adapter.notifyDataSetChanged();
									break;
								case 1:
									FoxMemDBHelper.delete_Pages(lcID, false, oDB);
									foxtip("��ɾ��: " + lcName);
									data.remove(longclickpos);
									adapter.notifyDataSetChanged();
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
									foxtip("��ɾ������¼: >= " + lcName);
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

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	private void exitMe() { // ������Activity
		setResult(RESULT_OK, (new Intent()).setAction("����������б�"));
		this.finish();
	}
}

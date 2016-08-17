package com.linpinger.foxbook;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Activity_BookInfo extends Activity {
	public static FoxMemDB oDB;
	private Button btn_save;
	private TextView tv_bid;
	private EditText edt_bname, edt_isend, edt_qdid, edt_burl, edt_delurl;
	
	SharedPreferences settings;
	public static final String FOXSETTING = "FOXSETTING";
	private boolean isEink = false; // �Ƿ�E-ink�豸

	private int bookid ;
	
	private void init_controls() { // ��ʼ�����ؼ�
		btn_save = (Button) findViewById(R.id.btn_save);
		tv_bid = (TextView) findViewById(R.id.tv_bid);
		edt_bname = (EditText) findViewById(R.id.edt_bname);
		edt_isend = (EditText) findViewById(R.id.edt_isend);
		edt_qdid = (EditText) findViewById(R.id.edt_qdid);
		edt_burl = (EditText) findViewById(R.id.edt_burl);
		edt_delurl = (EditText) findViewById(R.id.edt_delurl);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
//		getActionBar().setDisplayShowHomeEnabled(false); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home   this.finish();
	
	public void onCreate(Bundle savedInstanceState) { // �����ʼ��
		settings = getSharedPreferences(FOXSETTING, 0);
		isEink = settings.getBoolean("isEink", isEink);
		if ( isEink ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookinfo);
		
		showHomeUp();
		
		// ͨ��intent��ȡ����
		Intent itt = getIntent();
		bookid = itt.getIntExtra("bookid", 0); // ����
		
		init_controls() ; // ��ʼ�����ؼ�
		// ��ʾ����
		Map<String,String> info = oDB.getOneRow("select name as bn, url as bu, isEnd as bend, qidianid as qid, delurl as list from book where id=" + bookid);
		tv_bid.setText(String.valueOf(bookid)) ;
		edt_bname.setText(info.get("bn"));
		edt_isend.setText(info.get("bend"));
		edt_qdid.setText(info.get("qid"));
		edt_burl.setText(info.get("bu"));
		edt_delurl.setText(info.get("list"));
		
		// ������水ť��������
		btn_save.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ContentValues cc = new ContentValues();
				cc.put("Name", edt_bname.getText().toString());
				cc.put("URL", edt_burl.getText().toString());
				cc.put("DelURL", edt_delurl.getText().toString());
				cc.put("QiDianID", edt_qdid.getText().toString());
				cc.put("isEnd", edt_isend.getText().toString());
				FoxMemDBHelper.update_cell("Book", cc, "id=" + bookid, oDB) ; // �޸ĵ����ֶ�
				setResult(RESULT_OK, (new Intent()).setAction("���޸��鼮��Ϣ"));
				finish();
			}
		});

	}
	
	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case android.R.id.home: // ����ͼ��
			this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}

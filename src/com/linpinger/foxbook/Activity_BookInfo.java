package com.linpinger.foxbook;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_BookInfo extends Activity {
	public static FoxMemDB oDB;
	private Button btn_save;
	private TextView tv_bid;
	private EditText edt_bname, edt_isend, edt_qdid, edt_burl, edt_delurl;
	
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // ��ɫ������

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
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.bookinfo, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case R.id.bi_getQDidFromURL:
			String url_now = edt_burl.getText().toString();
			if ( url_now.contains(".qidian.com/") ) {
				edt_qdid.setText(String.valueOf(site_qidian.qidian_getBookID_FromURL(url_now)));
			} else {
				foxtip("URL������ .qidian.com/");
			}
			break;
		case R.id.bi_copyBookName:
			String bn = edt_bname.getText().toString();
			TOOLS.setcliptext(bn, this);
			foxtip("������: " + bn);
			break;
		case R.id.bi_copyQidianID:
			String bq = edt_qdid.getText().toString();
			TOOLS.setcliptext(bq, this);
			foxtip("������: " + bq);
			break;
		case R.id.bi_copyURL: // ����
			String bu = edt_burl.getText().toString();
			TOOLS.setcliptext(bu, this);
			foxtip("������: " + bu);
			break;
		case R.id.bi_pasteBookName: // ճ��
			edt_bname.setText(TOOLS.getcliptext(this));
			break;
		case R.id.bi_pasteQidianID:
			edt_qdid.setText(TOOLS.getcliptext(this));
			break;
		case R.id.bi_pasteURL:
			edt_burl.setText(TOOLS.getcliptext(this));
			break;
		case android.R.id.home: // ����ͼ��
			this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	


	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}

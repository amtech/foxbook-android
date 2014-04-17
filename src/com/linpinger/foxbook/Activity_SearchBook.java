package com.linpinger.foxbook;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;

import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.net.URLEncoder;

public class Activity_SearchBook extends Activity {
	private long mExitTime ;
	private WebView wv;
	private EditText et;
	private Button btn_search, btn_pre, btn_add;
	private boolean bShowAll = false ;
	
	private String book_name = "";
	private String book_url = "";
	private static int FROM_DB = 1 ;
	private static int FROM_NET = 2 ; 


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		mExitTime = System.currentTimeMillis();
		
		et = (EditText) findViewById(R.id.editText1);
		wv = (WebView) findViewById(R.id.webView1);
		btn_search = (Button) findViewById(R.id.button1);
		btn_pre = (Button) findViewById(R.id.button2);
		btn_add = (Button) findViewById(R.id.button3);
		
//		wv.loadUrl("about:blank");
//		wv.loadDataWithBaseURL("http://www.autohotkey.net/~linpinger/index.html?s=FoxBook_Android", "�÷�˵��:", "text/html", "utf-8", "");

		wv.setWebViewClient(new WebViewClient() { // �ڵ�ǰwebview������ת
			public boolean shouldOverrideUrlLoading(WebView wb, String url) {
				wb.loadUrl(url);
				return true;
			}
		});

		btn_search.setOnClickListener(new OnClickListener() { // �����ť���� // ��Ҫת������
			public void onClick(View v) {
				book_name = et.getText().toString();
				String gbURL;
				try {
					gbURL = URLEncoder.encode(book_name, "GB2312");
					wv.loadUrl("http://www.sogou.com/web?query=" + gbURL
							+ "&num=50");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		});

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle data = msg.getData();
				String html = data.getString("html");

				Intent intent = new Intent(Activity_SearchBook.this,
						Activity_PageList.class);
				intent.putExtra("iam", FROM_NET);
				intent.putExtra("bookurl", book_url);
				intent.putExtra("bookname", book_name);
				intent.putExtra("html", html);
				intent.putExtra("bShowAll", bShowAll);

				startActivity(intent);
			}
		};

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				String html = FoxBookLib.downhtml(book_url);

				Message msg = new Message();
				Bundle data = new Bundle();
				data.putString("html", html); // ��ʾ��ҳ����
				msg.setData(data);
				handler.sendMessage(msg);
			}
		};

		btn_pre.setOnClickListener(new OnClickListener() { // Ԥ����ť
			public void onClick(View v) {
				book_url = wv.getUrl();
				if ( null != book_url ) {
					setTitle("����Ŀ¼ : " + book_name + " <" + book_url + ">");
					new Thread(runnable).start();
				} else {
					book_url = "";
					Intent itt = getIntent();
					book_url = itt.getStringExtra("bookurl");
					if ( null == book_url ) {
						book_url = "";
						setTitle("����: ��ǰҳ���ַΪ��");
					} else {
						book_name = itt.getStringExtra("bookname");
						if ( null == book_name ) {
							book_name = "";
						}
						setTitle("����Ŀ¼ : " + book_name + " <" + book_url + ">");
						new Thread(runnable).start();
					}
				}
			}
		});
	
		btn_add.setOnClickListener(new OnClickListener() { // ��Ӱ�ť
			public void onClick(View v) {
				book_url = wv.getUrl();
				if ( null != book_url && "" != book_name ) {
					int nBookID = 0 ;
					// ���������ݿ⣬����ȡ����bookid
					nBookID = FoxDB.insertbook(book_name, book_url);
					if ( nBookID < 1 )
						return ;
					Intent itti = new Intent(
							Activity_SearchBook.this,
							Activity_BookInfo.class);
					itti.putExtra("bookid", nBookID);
					startActivity(itti);
					setResult(RESULT_OK, (new Intent()).setAction("�����б�"));
					finish();
				} else {
					setTitle("��Ϣ������@���� : " + book_name + " <" + book_url + ">");
				}
			}
		});
		
	}

	public boolean onKeyDown(int keyCoder, KeyEvent event) { // ���˳���
		if ( keyCoder == KeyEvent.KEYCODE_BACK ) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) {
				if ( wv.canGoBack() ) {
					Toast.makeText(this, "������...", Toast.LENGTH_SHORT).show();
					wv.goBack(); // goBack()��ʾ����webView����һҳ��
				} else {
					Toast.makeText(this, "�ٰ�һ���˳�����", Toast.LENGTH_SHORT).show();
                    mExitTime = System.currentTimeMillis();
				}
			} else {
				setResult(RESULT_OK, (new Intent()).setAction("�����б�"));
				finish();
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

	/*
	 * protected void onPause() { super.onPause(); }
	 * 
	 * protected void onResume() { super.onResume(); }
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.search, menu);
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case R.id.sm_bShowAll: // Ԥ����ʾ������Ŀ
			bShowAll = ! item.isChecked() ;
			item.setChecked(bShowAll);
			break;
		case R.id.sm_QuickSearchSouGou: // ����:�ѹ�
			Intent intent = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			book_name = et.getText().toString();
			intent.putExtra("bookname", book_name);
			startActivity(intent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}

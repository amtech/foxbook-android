package com.linpinger.foxbook;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
	private ImageButton btn_search;
	private Button btn_pre ;
	private boolean bShowAll = false ;
	
	private String book_name = "";
	private String book_url = "";
//	private final int FROM_DB = 1 ;
	private final int FROM_NET = 2 ; 

	private static Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		mExitTime = System.currentTimeMillis();
		
		et = (EditText) findViewById(R.id.editText1);
		wv = (WebView) findViewById(R.id.webView1);
		btn_search = (ImageButton) findViewById(R.id.button1);
		btn_pre = (Button) findViewById(R.id.button2);
		
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
					e.toString();
				}

			}

		});

		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
//				super.handleMessage(msg);
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
				return false;
			}
		});

		final Runnable downHTML = new Runnable() {
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
					new Thread(downHTML).start();
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
						new Thread(downHTML).start();
					}
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
			book_name = et.getText().toString();
			Intent intent = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			intent.putExtra("bookname", book_name);
			intent.putExtra("searchengine", 1);
			startActivity(intent);
			break;
		case R.id.sm_QuickSearchBing:  // ����:Bing
			book_name = et.getText().toString();
			Intent itb = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			itb.putExtra("bookname", book_name);
			itb.putExtra("searchengine", 3);
			startActivity(itb);
			break;

		case R.id.sm_QuickSearchYahoo:  // ����:�Ż�
			book_name = et.getText().toString();
			Intent ityh = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			ityh.putExtra("bookname", book_name);
			ityh.putExtra("searchengine", 2);
			startActivity(ityh);
			break;

		}
		return super.onOptionsItemSelected(item);
	}

}

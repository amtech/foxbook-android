package com.linpinger.foxbook;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.TargetApi;
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
import java.util.List;
import java.util.Map;

public class Activity_SearchBook extends Activity {
	public static FoxMemDB oDB;
	private long mExitTime ;
	private WebView wv;
	private EditText et;
	private ImageButton btn_search;
	private Button btn_pre ;
	
	public static final String FOXSETTING = "FOXSETTING";
	
	private String book_name = "";
	private String book_url = "";
	
	private final int IS_GETQIDIANURL = 8;
	private final int IS_DOWNHTML = 5;
	
	private static Handler handler;

	public class GetQidianURL implements Runnable {
		@Override
		public void run() {
			String json = FoxBookLib.downhtml(site_qidian.qidian_getSearchURL_Mobile(book_name), "utf-8");
            List<Map<String, Object>> qds = site_qidian.json2BookList(json);
            if ( qds.get(0).get("name").toString().equalsIgnoreCase(book_name) ) { // ��һ���������Ŀ����
            	book_url = qds.get(0).get("url").toString();
            }

			Message msg = Message.obtain();
			msg.what = IS_GETQIDIANURL;
			msg.obj = book_url;
			handler.sendMessage(msg);
		}			
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
//		getActionBar().setDisplayShowHomeEnabled(false); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home   this.finish();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		mExitTime = System.currentTimeMillis();
		
		showHomeUp();
		
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
		
		// ˵��
//		wv.getSettings().setDefaultTextEncodingName("UTF-8");
		String html = "<!DOCTYPE html>\n<html>\n<head>\t<META http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n<title>������˵��</title>\n</head>\n<body bgcolor=\"#eefaee\">\n<h2>˵��:</h2>\n\n<h3>ʹ��������������:</h3>\n<ul>\n<li>����Ҫ��������������������ť��Ȼ�����������ʾ����������</li>\n<li>�������ֱ��Ŀ¼ҳ��Ȼ�󰴰�ť��Ԥ��</li>\n</ul>\n\n<h3>ʹ�ÿ�������:</h3>\n<ul>\n<li>����Ҫ����������</li>\n<li>���˵������ڳ����Ĳ˵���ѡ��һ����������</li>\n</ul>\n\n<p>����������б������Ļ������Ӻ�����飬֮�󰴱��水ť</p>\n<p>��Ȼ��ص������漴�ɿ�������ӵ���</p>\n\n</body>\n</html>" ;
		wv.loadData(html, "text/html; charset=UTF-8", null);

		btn_search.setOnClickListener(new OnClickListener() { // �����ť���� // ��Ҫת������
			public void onClick(View v) {
				book_name = et.getText().toString();
				try {
					wv.loadUrl("http://cn.bing.com/search?q=" + URLEncoder.encode(book_name, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.toString();
				}

			}

		});

		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
					case IS_GETQIDIANURL:
						String lcQidianURL = (String)msg.obj;
						if ( 0 != lcQidianURL.length() ) {
							Intent intentQD = new Intent(Activity_SearchBook.this, Activity_PageList.class);
							intentQD.putExtra("iam", SITES.FROM_NET);
							intentQD.putExtra("bookurl", lcQidianURL);
							intentQD.putExtra("bookname", book_name);
							intentQD.putExtra("searchengine", SITES.SE_QIDIAN_MOBILE);
							Activity_PageList.oDB = oDB;
							startActivity(intentQD);
						} else {
							foxtip("�������δ������������");
						}
						break;
					case IS_DOWNHTML:
						String html = (String)msg.obj;

						Intent intent = new Intent(Activity_SearchBook.this, Activity_PageList.class);
						intent.putExtra("iam", SITES.FROM_NET);
						intent.putExtra("bookurl", book_url);
						intent.putExtra("bookname", book_name);
						intent.putExtra("html", html);
						Activity_PageList.oDB = oDB;
						startActivity(intent);
						break;
				}
				return false;
			}
		});

		final Runnable downHTML = new Runnable() {
			@Override
			public void run() {
				String html = FoxBookLib.downhtml(book_url);
				
				Message msg = Message.obtain();
				msg.what = IS_DOWNHTML;
				msg.obj = html;
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
		case android.R.id.home: // ����ͼ��
			this.finish();
			break;
		case R.id.sm_QuickSearchQidian: // ����:���
			book_name = et.getText().toString();
			(new Thread(new GetQidianURL())).start() ;
			break;
		case R.id.sm_QuickSearchQreader: // ����:���
			book_name = et.getText().toString();
			Intent intent13 = new Intent(
					Activity_SearchBook.this,
					Activity_PageList.class);
			intent13.putExtra("iam", SITES.FROM_NET);
			intent13.putExtra("bookurl", "http://linpinger.github.io/");
			intent13.putExtra("bookname", book_name);
			intent13.putExtra("searchengine", SITES.SE_QREADER);
			Activity_PageList.oDB = oDB;
			startActivity(intent13);
			break;
		case R.id.sm_QuickSearchZhuiShuShenQi: // ����:׷������
			book_name = et.getText().toString();
			Intent itzssq = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			itzssq.putExtra("bookname", book_name);
			itzssq.putExtra("searchengine", SITES.SE_ZSSQ);
			Activity_QuickSearch.oDB = oDB;
			startActivity(itzssq);
			break;
		case R.id.sm_QuickSearchEaSou: // ����:Easou
			book_name = et.getText().toString();
			Intent iteasou = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			iteasou.putExtra("bookname", book_name);
			iteasou.putExtra("searchengine", SITES.SE_EASOU);
			Activity_QuickSearch.oDB = oDB;
			startActivity(iteasou);
			break;
		case R.id.sm_QuickSearchSouGou: // ����:�ѹ�
			book_name = et.getText().toString();
			Intent intent = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			intent.putExtra("bookname", book_name);
			intent.putExtra("searchengine", SITES.SE_SOGOU);
			Activity_QuickSearch.oDB = oDB;
			startActivity(intent);
			break;
		case R.id.sm_QuickSearchBing:  // ����:Bing
			book_name = et.getText().toString();
			Intent itb = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			itb.putExtra("bookname", book_name);
			itb.putExtra("searchengine", SITES.SE_BING);
			Activity_QuickSearch.oDB = oDB;
			startActivity(itb);
			break;

		case R.id.sm_QuickSearchYahoo:  // ����:�Ż�
			book_name = et.getText().toString();
			Intent ityh = new Intent(Activity_SearchBook.this, Activity_QuickSearch.class);
			ityh.putExtra("bookname", book_name);
			ityh.putExtra("searchengine", SITES.SE_YAHOO);
			Activity_QuickSearch.oDB = oDB;
			startActivity(ityh);
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}

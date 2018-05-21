package com.linpinger.foxbook;

import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.ToolAndroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_BookInfo extends Activity {
	private NovelManager nm;
	private int bookIDX = -1;
	private TextView tv_bid;
	private EditText edt_bname, edt_bauthor, edt_isend, edt_qdid, edt_burl, edt_delurl;

	SharedPreferences settings;

	private void init_controls() { // 初始化各控件
		tv_bid = (TextView) findViewById(R.id.tv_bid);
		edt_bname = (EditText) findViewById(R.id.edt_bname);
		edt_bauthor = (EditText) findViewById(R.id.edt_bauthor);
		edt_isend = (EditText) findViewById(R.id.edt_isend);
		edt_qdid = (EditText) findViewById(R.id.edt_qdid);
		edt_burl = (EditText) findViewById(R.id.edt_burl);
		edt_delurl = (EditText) findViewById(R.id.edt_delurl);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true); // 标题栏中添加返回图标
	}

	public void onCreate(Bundle savedInstanceState) { // 界面初始化
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookinfo);

		showHomeUp();
		init_controls() ; // 初始化各控件

		this.nm = ((FoxApp)this.getApplication()).nm ;

		// 通过intent获取数据
		bookIDX = getIntent().getIntExtra(NV.BookIDX, -1);
		Map<String, Object> info = nm.getBookInfo(bookIDX);

		// 显示数据
		tv_bid.setText(String.valueOf(bookIDX)) ;
		edt_bname.setText(info.get(NV.BookName).toString()) ;
		edt_bauthor.setText(info.get(NV.BookAuthor).toString()) ;
		edt_isend.setText(String.valueOf(info.get(NV.BookStatu))) ;
		edt_qdid.setText(info.get(NV.QDID).toString()) ;
		edt_burl.setText(info.get(NV.BookURL).toString()) ;
		edt_delurl.setText(info.get(NV.DelURL).toString()) ;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.bookinfo, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.bi_getQDidFromURL:
			String url_now = edt_burl.getText().toString();
			if ( url_now.contains(".qidian.com/") ) {
				edt_qdid.setText(new SiteQiDian().getBookID_FromURL(url_now));
			} else {
				foxtip("URL不包含 .qidian.com/");
			}
			break;
		case R.id.bi_clearDelURL:
			edt_delurl.setText("");
			break;
		case R.id.bi_copyBookName:
			String bn = edt_bname.getText().toString();
			ToolAndroid.setClipText(bn, this);
			foxtip("剪贴板: " + bn);
			break;
		case R.id.bi_copyBookAuthor:
			String ba = edt_bauthor.getText().toString();
			ToolAndroid.setClipText(ba, this);
			foxtip("剪贴板: " + ba);
			break;
		case R.id.bi_copyQidianID:
			String bq = edt_qdid.getText().toString();
			ToolAndroid.setClipText(bq, this);
			foxtip("剪贴板: " + bq);
			break;
		case R.id.bi_copyURL: // 复制
			String bu = edt_burl.getText().toString();
			ToolAndroid.setClipText(bu, this);
			foxtip("剪贴板: " + bu);
			break;
		case R.id.bi_copyALL: // 复制全部
			String fbs = "FoxBook>" + edt_bname.getText().toString() + ">"
					+ edt_bauthor.getText().toString() + ">"
					+ edt_qdid.getText().toString() + ">"
					+ edt_burl.getText().toString() + ">"
					+ edt_delurl.getText().toString();
			ToolAndroid.setClipText(fbs, this);
			foxtip("剪贴板: " + fbs);
			break;
		case R.id.bi_pasteALL: // 粘贴全部
			String nowfbs = ToolAndroid.getClipText(this);
			if ( ! nowfbs.contains("FoxBook>") ) {
				foxtip("剪贴板中的内容格式不对哟");
				break;
			}
			String xx[] = nowfbs.split(">");
			edt_bname.setText(xx[1]);
			edt_bauthor.setText(xx[2]);
			edt_qdid.setText(xx[3]);
			if ( edt_burl.getText().toString().contains("http") ) {
				ToolAndroid.setClipText(xx[4], this);
				foxtip("剪贴板: " + xx[4]);
			} else {
				edt_burl.setText(xx[4]);
				edt_delurl.setText(xx[5]);
			}
			break;
		case R.id.bi_pasteBookName: // 粘贴
			edt_bname.setText(ToolAndroid.getClipText(this));
			break;
		case R.id.bi_pasteBookAuthor:
			edt_bauthor.setText(ToolAndroid.getClipText(this));
			foxtip("若作者留空，表示是新书，更新时不会只下载最后55章");
			break;
		case R.id.bi_pasteQidianID:
			edt_qdid.setText(ToolAndroid.getClipText(this));
			break;
		case R.id.bi_pasteURL:
			edt_burl.setText(ToolAndroid.getClipText(this));
			break;
		case android.R.id.home: // 返回图标
			onBackPressed();
			break;
		case R.id.bi_save_exit:
			Map<String, Object> info = nm.getBlankBookInfo();
			info.put(NV.BookName, edt_bname.getText().toString());
			info.put(NV.BookAuthor, edt_bauthor.getText().toString());
			info.put(NV.BookURL, edt_burl.getText().toString());
			info.put(NV.DelURL, edt_delurl.getText().toString());
			info.put(NV.QDID, edt_qdid.getText().toString());
			info.put(NV.BookStatu, Integer.valueOf(edt_isend.getText().toString()));
			nm.setBookInfo(info, bookIDX);

			onBackPressed();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() { // 返回键被按
		setResult(RESULT_OK);
		finish();
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}

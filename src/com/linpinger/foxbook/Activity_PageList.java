package com.linpinger.foxbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.Ext_ListActivity_4Eink;
import com.linpinger.tool.ToolBookJava;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

// Activity_ShowPage4Eink : �����б���ʾ���� aShowPageInMem|aShowPageOnNet, bookIDX, pageIDX, [pageName, pageFullUrl]
// Activity_BookInfo : ����鼮
public class Activity_PageList extends Ext_ListActivity_4Eink {
	private NovelManager nm;

	SharedPreferences settings;

	private List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
	private ListView lv_pagelist ;
	private int posLongClick = -1 ;
	private boolean isOnLine = true; // �Ƿ�����

	SimpleAdapter adapter;
	private Handler handler;

	private static int IS_UPDATEPAGE = 88;
	private static int IS_RenderListView = 5;

	private int ittAction = 0 ; // ���������
	private int bookIDX = -1 ;
	private String searchBookName = "";
	private String searchBookURL = "";

	public class DownTOC implements Runnable { // ��̨�߳�������ҳ
		private String tocURL ;
		public DownTOC(String inURL){
			this.tocURL = inURL;
		}
		@Override
		public void run() {
			if ( tocURL.contains(".if.qidian.com") ) // ���߲鿴��վ��������ֻ�ʱ
				ittAction = AC.aListQDPages;
			if ( ittAction == AC.aListQDPages | ittAction == AC.aSearchBookOnQiDian )
				data = new SiteQiDian().getTOC_Android7( ToolBookJava.downhtml(tocURL, "utf-8") );
			if ( ittAction == AC.aListSitePages | ittAction == AC.aSearchBookOnSite )
				data = new NovelSite().getTOC( ToolBookJava.downhtml(tocURL) ); // PageName PageURL
			handler.sendEmptyMessage(IS_RenderListView);
		}
	}

	private void renderListView() { // ˢ��LV
		if ( ! isOnLine && data.size() == 0 ) { // ����¼ɾ����󣬽�����Activity
			onBackPressed();
		}
		switch (ittAction) {
		case AC.aListBookPages:
		case AC.aListAllPages:
		case AC.aListLess1KPages:
			adapter = new SimpleAdapter(this, data, R.layout.lv_item_pagelist,
					new String[] { NV.PageName, NV.Size }, new int[] { R.id.tvName, R.id.tvCount });
			lv_pagelist.setAdapter(adapter);
			break;
		case AC.aListSitePages:
		case AC.aListQDPages:
		case AC.aSearchBookOnQiDian:
		case AC.aSearchBookOnSite:
			adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_1,
					new String[] { NV.PageName }, new int[] { android.R.id.text1 });
			lv_pagelist.setAdapter(adapter);
			lv_pagelist.setSelection(adapter.getCount() - 1); // �����б�����β��
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) { // ��ʼ�� ���� ��Ŀ ����Ϊ
		Map<String, Object> page = (HashMap<String, Object>) l.getItemAtPosition(position);

		Intent itt = new Intent(Activity_PageList.this, Activity_ShowPage4Eink.class);

		switch (ittAction) {
		case AC.aListBookPages:
		case AC.aListAllPages:
		case AC.aListLess1KPages:
			itt.putExtra(AC.action, AC.aShowPageInMem);
			itt.putExtra(NV.BookIDX, (Integer)page.get(NV.BookIDX));
			itt.putExtra(NV.PageIDX, (Integer)page.get(NV.PageIDX));
			if ( ittAction == AC.aListBookPages) {
				String bookURL = nm.getBookInfo(bookIDX).get(NV.BookURL).toString() ;
				if ( bookURL.contains("zip://") )
					itt.putExtra(NV.PageFullURL, bookURL + "@" + page.get(NV.PageURL).toString() ); // 1024DB3
			}
			break;
		case AC.aListSitePages:
			itt.putExtra(AC.action, AC.aShowPageOnNet);
			itt.putExtra(NV.BookIDX, bookIDX);
			itt.putExtra(NV.PageIDX, -1);
			itt.putExtra(NV.PageName, page.get(NV.PageName).toString() );
			itt.putExtra(NV.PageFullURL, ToolBookJava.getFullURL( nm.getBookInfo(bookIDX).get(NV.BookURL).toString(), page.get(NV.PageURL).toString()) );
			break;
		case AC.aListQDPages:
		case AC.aSearchBookOnQiDian:
			itt.putExtra(AC.action, AC.aShowPageOnNet);
			itt.putExtra(NV.BookIDX, bookIDX);
			itt.putExtra(NV.PageIDX, -1);
			itt.putExtra(NV.PageName, page.get(NV.PageName).toString() );
			itt.putExtra(NV.PageFullURL, new SiteQiDian().getContentFullURL_Android7(page.get(NV.PageURL).toString()) );
			break;
		case AC.aSearchBookOnSite:
			itt.putExtra(AC.action, AC.aShowPageOnNet);
			itt.putExtra(NV.BookIDX, bookIDX);
			itt.putExtra(NV.PageIDX, -1);
			itt.putExtra(NV.PageName, page.get(NV.PageName).toString() );
			itt.putExtra(NV.PageFullURL, ToolBookJava.getFullURL( this.searchBookURL, page.get(NV.PageURL).toString()) );
			break;
		default:
			break;
		}
		System.out.println("APL: Action=" + itt.getIntExtra(AC.action, 0)
				+ " bookIDX=" + itt.getIntExtra(NV.BookIDX, -1)
				+ " pageIDX=" + itt.getIntExtra(NV.PageIDX, -1));
		startActivity(itt);
		super.onListItemClick(l, v, position, id);
	}


	private void init_handler() { // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_UPDATEPAGE ) // �����½����
					setTitle("������� : " + (String)msg.obj );
				if ( msg.what == IS_RenderListView ) // ����Ŀ¼���
					renderListView();
			}
		};
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
		getActionBar().setDisplayShowHomeEnabled(false); // ���س���ͼ��
	}

	@Override
	public void onCreate(Bundle savedInstanceState) { // ���
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pagelist);

		showHomeUp();
		lv_pagelist = getListView();
		this.registerForContextMenu(lv_pagelist); // ListViewע�������Ĳ˵�

		this.nm = ((FoxApp)this.getApplication()).nm;
		init_handler() ; // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ

		// ��ȡ���������
		Intent itt = getIntent();
		ittAction = itt.getIntExtra(AC.action, AC.aListBookPages);
		bookIDX = itt.getIntExtra(NV.BookIDX, -1);
switch (ittAction) {
		case AC.aListBookPages:
			isOnLine = false ;
			data = nm.getBookPageList( bookIDX ); // ��ȡҳ���б�
			Map<String, Object> info = nm.getBookInfo(bookIDX);

			setTitle(info.get(NV.BookName) + " : " + info.get(NV.BookURL));
			break;
		case AC.aListAllPages:
			isOnLine = false ;
			data = nm.getPageList(1);
			setTitle("�� " + String.valueOf(data.size()) + " ��");
			break;
		case AC.aListLess1KPages:
			isOnLine = false ;
			data = nm.getPageList(999);
			setTitle("�� " + String.valueOf(data.size()) + " ��");
			break;
		case AC.aListSitePages:
			new Thread(new DownTOC( nm.getBookInfo(bookIDX).get(NV.BookURL).toString() )).start(); // ���߲鿴Ŀ¼
			setTitle("���߿�: " + nm.getBookInfo(bookIDX).get(NV.BookName).toString() );
			break;
		case AC.aListQDPages:
			this.searchBookName = nm.getBookInfo(bookIDX).get(NV.BookName).toString();
			this.searchBookURL = itt.getStringExtra(NV.TmpString) ;
			new Thread(new DownTOC(this.searchBookURL)).start(); // �鿴���
			setTitle("���: " + this.searchBookName );
			break;
		case AC.aSearchBookOnQiDian:
		case AC.aSearchBookOnSite:
			this.searchBookName = itt.getStringExtra(NV.BookName);
			this.searchBookURL = itt.getStringExtra(NV.BookURL);
			new Thread(new DownTOC( this.searchBookURL )).start(); // ���� һ��TOC
			setTitle("����: " + this.searchBookName );
			break;
		default:
			break;
}

		renderListView();
	}


	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.pagelist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.pm_Add:
					if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite )
						menu.getItem(i).setVisible(true); // ��������ʱ������Ӱ�ť
					else
						menu.getItem(i).setVisible(false);
					break;
				case R.id.pm_cleanBook:
					if ( ittAction == AC.aListQDPages |ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite )
						menu.getItem(i).setVisible(false); // ��������ʱ����ɾ����ť
					else
						menu.getItem(i).setVisible(true);
					break;
				case R.id.pm_cleanBookND:
					if ( ittAction == AC.aSearchBookOnQiDian | ittAction == AC.aSearchBookOnSite
						| ittAction == AC.aListAllPages | ittAction == AC.aListLess1KPages )
						menu.getItem(i).setVisible(false); // ��������ʱ����ɾ����ť
					else
						menu.getItem(i).setVisible(true);
					break;
			}
		}

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧ�˵�
		switch (item.getItemId()) {
		case android.R.id.home: // ����ͼ��
			onBackPressed();
			break;
		case R.id.pl_finish:
			onBackPressed();
			break;
		case R.id.pm_cleanBook:
			if ( ittAction == AC.aListAllPages) {
				nm.clearShelf(true);
				nm.simplifyAllDelList(); // ��������DelList
			}
			if ( ittAction == AC.aListBookPages )
				nm.clearBook(bookIDX, true);
			if ( ittAction == AC.aListLess1KPages ) { // ����һ����ɾ
				for ( int i=data.size()-1; i>=0; i-- )
					nm.clearPage((Integer)data.get(i).get(NV.BookIDX), (Integer)data.get(i).get(NV.PageIDX), true);
			}
			data.clear();
			adapter.notifyDataSetChanged();
			foxtip("��ɾ�����в����¼�¼");
			onBackPressed();
			break;
		case R.id.pm_cleanBookND:
			if ( ittAction == AC.aListBookPages )
				nm.clearBook(bookIDX, false);
			foxtip("��ɾ������");
			onBackPressed();
			break;
		case R.id.pm_Add:
			if ( "" != this.searchBookURL && "" != this.searchBookName ) {
				int nBookIDX = -1 ;
				nBookIDX = nm.addBook(this.searchBookName, this.searchBookURL, ""); // ����������ȡ����bookidx
				if ( nBookIDX < 0 )
					break ;

				Intent itti = new Intent(Activity_PageList.this, Activity_BookInfo.class);
				itti.putExtra(NV.BookIDX, nBookIDX);
				startActivity(itti);
				onBackPressed();
			} else {
				setTitle("��Ϣ������@���� : " + this.searchBookName + " <" + this.searchBookURL + ">");
			}
			break;
		case R.id.jumplist_tobottom:
			lv_pagelist.setSelection(adapter.getCount() - 1);
			setItemPos4Eink(adapter.getCount() - 1);
			break;
		case R.id.jumplist_totop:
			lv_pagelist.setSelection(0);
			setItemPos4Eink(); // ����λ�÷ŵ�ͷ��
			break;
		case R.id.jumplist_tomiddle:
			int midPos = adapter.getCount() / 2 - 1 ;
			lv_pagelist.setSelection(midPos);
			setItemPos4Eink(midPos);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
//		if ( bookIDX == -1 ) { // intent������bookIDX, ������������ʱ����Ŀû��ID
//			foxtip("itent ����� bookIDX = -1\n\n������������ʱ����Ŀû��ID");
//			return super.onContextItemSelected(item);
//		}

		Map<String, Object> page = data.get(posLongClick);
		final String lcName = page.get(NV.PageName).toString(); // final �����߳���ʹ��
		final int bookIDX = (Integer) page.get(NV.BookIDX);
		final int pageIDX = (Integer) page.get(NV.PageIDX);

		setTitle(lcName + " : " + page.get(NV.PageURL));

		String itemName = item.getTitle().toString();
		if ( itemName.equalsIgnoreCase("ɾ������") ) {
			nm.clearPage(bookIDX, pageIDX, true);
			foxtip("��ɾ������¼: " + lcName);
		} else if ( itemName.equalsIgnoreCase("ɾ�����²���д��Dellist") ) {
			nm.clearPage(bookIDX, pageIDX, false);
			foxtip("��ɾ��: " + lcName);
		} else if ( itemName.equalsIgnoreCase("ɾ�����¼�����") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, true, true);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, true, true);
			foxtip("��ɾ������¼: <= " + lcName);
		} else if ( itemName.equalsIgnoreCase("ɾ�����¼����ϲ���д��Dellist") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, true, false);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, true, false);
			foxtip("��ɾ��: <= " + lcName);
		} else if ( itemName.equalsIgnoreCase("ɾ�����¼�����") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, false, true);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, false, true);
			foxtip("��ɾ������¼: >= " + lcName);
		} else if ( itemName.equalsIgnoreCase("ɾ�����¼����²���д��Dellist") ) {
			if ( ittAction == AC.aListBookPages )
				nm.clearBookPages(bookIDX, pageIDX, false, false);
			if ( ittAction == AC.aListAllPages )
				nm.clearShelfPages(bookIDX, pageIDX, false, false);
			foxtip("��ɾ��: >= " + lcName);
		} else if ( itemName.equalsIgnoreCase("���±���") ) {
			if ( bookIDX != -1 ) {
				setTitle("���ڸ���: " + lcName);
				(new Thread(){
					public void run(){
						nm.updatePage(bookIDX, pageIDX);
						updateLocalData(bookIDX) ; // ����data����
						Message msg = Message.obtain();
						msg.what = IS_UPDATEPAGE;
						msg.obj = lcName ;
						handler.sendMessage(msg);
					}
				}).start();
			}
		} else if ( itemName.equalsIgnoreCase("�༭����") ) {
			Intent ittPageInfo = new Intent(Activity_PageList.this,Activity_PageInfo.class);
			ittPageInfo.putExtra(NV.BookIDX, bookIDX);
			ittPageInfo.putExtra(NV.PageIDX, pageIDX);
			startActivity(ittPageInfo);
		} else {
			foxtip("һ����Ȧ����ûʵ������˵���:\n" + itemName);
		}

		updateLocalData(bookIDX) ; // ����data����

		setItemPos4Eink(); // ����λ�÷ŵ�ͷ��
		renderListView();

		return super.onContextItemSelected(item);
	}

	void updateLocalData(int inBookIDX) {
		switch (ittAction) { // ����data����
		case AC.aListBookPages:
			data = nm.getBookPageList( inBookIDX ); // ��ȡҳ���б�
			break;
		case AC.aListAllPages:
			data = nm.getPageList(1);
			break;
		case AC.aListLess1KPages:
			data = nm.getPageList(999);
			break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		if ( isOnLine ) {
			return ; // ���ߵľͲ���ʾmenu
		}

		posLongClick = ( (AdapterContextMenuInfo) menuInfo ).position; //��position �������б��еڼ�����ֵ������Ĳ˵���
		menu.setHeaderTitle("����:" + data.get(posLongClick).get(NV.PageName).toString());

		menu.add("ɾ������");
		menu.add("ɾ�����²���д��Dellist");
		if ( ittAction != AC.aListLess1KPages ) {
		menu.add("ɾ�����¼�����");
		menu.add("ɾ�����¼����ϲ���д��Dellist");
		menu.add("ɾ�����¼�����");
		menu.add("ɾ�����¼����²���д��Dellist");
		}
		menu.add("�༭����");
		menu.add("���±���");

	}

	@Override
	public void onBackPressed() { // ���ؼ�����
		setResult(RESULT_OK);
		finish();
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}

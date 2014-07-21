package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class Activity_BookList extends ListActivity {
	
	public int downThread = 9 ;  // ҳ�����������߳���
	public int leftThread = downThread ;

	ListView lv_booklist;
	List<Map<String, Object>> data;
	String lcURL, lcName; // long click �ı���
	Integer lcCount, lcID;
	private static Handler handler;
	private final int IS_MSG = 1;
	private final int IS_NEWPAGE = 2;
	private final int IS_REFRESHLIST = 3;
	private final int IS_REGENID = 4;
	private final int IS_NEWVER = 5;
	private final int FROM_DB = 1;
	private final int FROM_NET = 2;
	private long mExitTime;
	private int anowID, aisEnd; // ȫ����������ʹ�õı���
	private String anowName;
	private int upthreadcount; // �����鼮����
	private int upchacount;    // �����½ڼ���
	
//	private final int SITE_EASOU = 11 ;
	private final int SITE_ZSSQ = 12 ;
	private final int SITE_KUAIDU = 13 ;

	public class FoxTaskDownPage implements Runnable { // ���߳��������ҳ���б�
		List<Map<String, Object>> taskList;
		public FoxTaskDownPage(List<Map<String, Object>> iTaskList) {
			this.taskList = iTaskList ;
		}
		public void run() {
			Message msg;
			String thName = Thread.currentThread().getName();
			Iterator<Map<String, Object>> itr = taskList.iterator();
			HashMap<String, Object> mm ;
			int nowID ;
			String nowURL ;
			int locCount = 0 ;
			int allCount = taskList.size();
			while (itr.hasNext()) {
				++ locCount ;
				mm = (HashMap<String, Object>) itr.next();
				nowID = (Integer) mm.get("id");
				nowURL = (String) mm.get("url");

				FoxBookLib.updatepage(nowID, nowURL);
				
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = leftThread + ":" + thName + ":" + locCount + " / " + allCount ;
				handler.sendMessage(msg);
			}
			--leftThread;
			if ( 0 == leftThread ) { // �����̸߳������
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = "�Ѹ��������пհ��½�>25" ;
				handler.sendMessage(msg);
			}
		}
	}


	public class UpdateBook implements Runnable { // ��̨�̸߳�����
		private int bookid;
		private String bookname;
		private boolean bDownPage = true;

		UpdateBook(int inbookid, String inbookname, boolean bDownPage) {
			this.bookid = inbookid;
			this.bookname = inbookname;
			this.bDownPage = bDownPage;
		}

		@Override
		public void run() {
			List<Map<String, Object>> xx;
			String bookurl = FoxDB.getOneCell("select url from book where id="
					+ String.valueOf(bookid)); // ��ȡ url
			String existList = FoxDB.getPageListStr(bookid); // �õ��� list

			Message msg = Message.obtain();
			msg.what = IS_MSG;
			msg.obj = bookname + ": ��������Ŀ¼ҳ";
			handler.sendMessage(msg);

			int site_type = 0 ;
			if ( bookurl.indexOf("zhuishushenqi.com") > -1 ) {
				site_type = SITE_ZSSQ ;
			}
			if ( bookurl.indexOf(".qreader.") > -1 ) {
				site_type = SITE_KUAIDU ;
			}
			
			String html = "";
			switch(site_type) {
			case SITE_KUAIDU:
				if (existList.length() > 3) {
					xx = site_qreader.qreader_GetIndex(bookurl, 55, 1); // ����ģʽ  ���55��
				} else {
					xx = site_qreader.qreader_GetIndex(bookurl, 0, 1); // ����ģʽ
				}
				break;
			case SITE_ZSSQ:
				html = FoxBookLib.downhtml(bookurl, "utf-8"); // ����json
				if (existList.length() > 3) {
					xx = site_zssq.json2PageList(html, 55, 1); // ����ģʽ  ���55��
				} else {
					xx = site_zssq.json2PageList(html, 0, 1); // ����ģʽ
				}
				break;
			default:
				html = FoxBookLib.downhtml(bookurl); // ����url
				if (existList.length() > 3) {
					xx = FoxBookLib.tocHref(html, 55); // ������ȡ list ���55��
				} else {
					xx = FoxBookLib.tocHref(html, 0); // ������ȡ list �����½�
				}
			}

			ArrayList<HashMap<String, Object>> newPages = (ArrayList<HashMap<String, Object>>)FoxBookLib.compare2GetNewPages(xx, existList) ;
			int newpagecount = newPages.size(); // ���½���������ͳ��

			if (newpagecount == 0) {
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = bookname + ": �����½�";
				handler.sendMessage(msg);
				handler.sendEmptyMessage(IS_REFRESHLIST); // ������ϣ�֪ͨˢ��
				if ( ! bDownPage ) { //��������Ҫ�����пհ��½�ʱ����һ��
					return;
				}
			} else {
				msg = Message.obtain();
				msg.what = IS_NEWPAGE;
				msg.arg1 = newpagecount; // ���½���
				msg.obj = bookname + ": ���½���: " + String.valueOf(newpagecount);
				handler.sendMessage(msg);
			}

			if ( newpagecount > 0 ) {
				FoxDB.inserNewPages(newPages, bookid); // ��ӵ����ݿ�
			}
			

			if (bDownPage) {
			List<Map<String, Object>> nbl = FoxDB.getBookNewPages(bookid);
			int cTask = nbl.size() ; // ��������
			
			if ( cTask > 25 ) { // �����½������� 25�¾Ͳ��ö���������ģʽ
				int nBaseCount = cTask / downThread ; //ÿ�̻߳���������
				int nLeftCount = cTask % downThread ; //ʣ��������
				int aList[] = new int[downThread] ; // ÿ���߳��е�������

				for ( int i = 0; i < downThread; i++ ) {  // ����������
					if ( i < nLeftCount ) {
						aList[i] = nBaseCount + 1 ;
					} else {
						aList[i] = nBaseCount ;
					}
				}

				List<Map<String, Object>> subList ;
				int startPoint = 0 ;
				for ( int i = 0; i < downThread; i++ ) {
					if ( aList[i] == 0 ) { // ���������������������߳��ٵ������
						--leftThread ;
						continue ;
					}
					subList = new ArrayList<Map<String, Object>>(aList[i]);
					for ( int n = startPoint; n < startPoint + aList[i]; n++ ) {
						subList.add((HashMap<String, Object>)nbl.get(n));
					}
					(new Thread(new FoxTaskDownPage(subList), "T" + i)).start() ;

					startPoint += aList[i] ;
				}
			} else {
			// ���߳�ѭ������ҳ��
			Iterator<Map<String, Object>> itrz = nbl.iterator();
//			String nowURL = "";
			Integer nowpageid = 0;
			int nowCount = 0;
			while (itrz.hasNext()) {
				HashMap<String, Object> nn = (HashMap<String, Object>) itrz.next();
//				nowURL = (String) nn.get("url");
				nowpageid = (Integer) nn.get("id");

				++nowCount;
				msg = Message.obtain();
				msg.what = IS_MSG;
				msg.obj = bookname + ": �����½�: " + nowCount + " / " + newpagecount ;
				handler.sendMessage(msg);

				FoxBookLib.updatepage(nowpageid);
			}
			} // ���̸߳���
			} // bDownPage

			msg = Message.obtain();
			msg.what = IS_MSG;
			msg.obj = bookname + ": �������";
			handler.sendMessage(msg);

			handler.sendEmptyMessage(IS_REFRESHLIST); // ������ϣ�֪ͨˢ��
		}
	}

	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				HashMap<String, Object> chapinfo = (HashMap<String, Object>) arg0.getItemAtPosition(arg2);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpcount = Integer.parseInt((String) chapinfo
						.get("count"));
				Integer tmpid = (Integer) chapinfo.get("id");
				setTitle(tmpname + " : " + tmpurl);

				if (tmpcount > 0) {
					Intent intent = new Intent(Activity_BookList.this,
							Activity_PageList.class);
					intent.putExtra("iam", FROM_DB);
					intent.putExtra("bookurl", tmpurl);
					intent.putExtra("bookname", tmpname);
					intent.putExtra("bookid", tmpid);
					startActivityForResult(intent, 0);
				}
			}
		};
		lv_booklist.setOnItemClickListener(listener);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent retIntent) { // �޸���󷵻ص�����
		if (0 == requestCode && RESULT_OK == resultCode) {
			refresh_BookList(); // ˢ��LV�е�����
		}
	}

	private void init_LV_item_Long_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				HashMap<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcCount = Integer.parseInt((String) chapinfol.get("count"));
				lcID = (Integer) chapinfol.get("id");
				setTitle(lcName + " : " + lcURL);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("����:" + lcName);
				builder.setItems(new String[] { "���±���", "���±���Ŀ¼", "���߲鿴", "�༭������Ϣ",
						"ɾ������", "��������", "����:�ѹ�", "����:����", "����:���", "����:׷������" },
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case 0:
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcName, true)).start();
									foxtip("���ڸ���: " + lcName);
									break;
								case 1:
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcName, false)).start();
									foxtip("���ڸ���Ŀ¼: " + lcName);
									break;
								case 2: // ���߲鿴
									Intent intent = new Intent(
											Activity_BookList.this,
											Activity_PageList.class);
									intent.putExtra("iam", FROM_NET);
									intent.putExtra("bookurl", lcURL);
									intent.putExtra("bookname", lcName);
									if ( lcURL.indexOf("zhuishushenqi.com") > -1 ) {
										intent.putExtra("searchengine", SITE_ZSSQ);
									}
									if ( lcURL.indexOf(".qreader.") > -1 ) {
										intent.putExtra("searchengine", SITE_KUAIDU);
									}
									startActivity(intent);
									break;
								case 3:
									Intent itti = new Intent(
											Activity_BookList.this,
											Activity_BookInfo.class);
									itti.putExtra("bookid", lcID);
									startActivityForResult(itti, 0);
									break;
								case 4:
									FoxDB.deleteBook(lcID);
									refresh_BookList();
									foxtip("��ɾ����:" + lcName);
									break;
								case 5:
									ClipboardManager cbm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									cbm.setText(lcName);
									foxtip("�Ѹ��Ƶ�������:" + lcName);
									break;
								case 6: //sougou
									Intent intent7 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent7.putExtra("bookname", lcName);
									intent7.putExtra("searchengine", 1);
									startActivity(intent7);
									break;
								case 7: //easou
									Intent intent8 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent8.putExtra("bookname", lcName);
									intent8.putExtra("searchengine", 11);
									startActivity(intent8);
									break;
								case 8:
									Intent intent13 = new Intent(
											Activity_BookList.this,
											Activity_PageList.class);
									intent13.putExtra("iam", FROM_NET);
									intent13.putExtra("bookurl", lcURL);
									intent13.putExtra("bookname", lcName);
									intent13.putExtra("searchengine", SITE_KUAIDU);
									startActivity(intent13);
									break;
								case 9: //׷������
									Intent intent9 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent9.putExtra("bookname", lcName);
									intent9.putExtra("searchengine", 12);
									startActivity(intent9);
									break;
								}
							}
						});
				builder.create().show();

				return true;
			}

		};
		lv_booklist.setOnItemLongClickListener(longlistener);
	}

	private void refresh_BookList() { // ˢ��LV�е�����
		data = FoxDB.getBookList(); // ��ȡ�鼮�б�
		// ����listview��Adapter
		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.lv_item_booklist, new String[] { "name", "count" },
				new int[] { R.id.tvName, R.id.tvCount });
		lv_booklist.setAdapter(adapter);
		// adapter.notifyDataSetChanged();
	}

	private void init_handler() { // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
				case IS_MSG:
					setTitle((String)msg.obj);
					break;
				case IS_NEWPAGE:
					upchacount += (Integer) msg.arg1;
					setTitle((String) msg.obj);
					break;
				case IS_REGENID :
					refresh_BookList(); // ˢ��LV�е�����
					foxtip((String) msg.obj);
					break;
				case IS_REFRESHLIST:
					--upthreadcount;
					refresh_BookList(); // ˢ��LV�е�����
					if (upthreadcount <1 ){
						setTitle("�� " + upchacount + " ���½ڣ�ȫ���������");
					} else {
						setTitle("ʣ���߳�: " + upthreadcount);
					}
					break;
				case IS_NEWVER:
					setTitle((String)msg.obj);

					try {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setDataAndType(Uri.fromFile(new File("/sdcard/FoxBook.apk")), "application/vnd.android.package-archive"); 
						startActivity(i);
					} catch(Exception e) {
						e.toString();
					}

					break;
				}
				return false;
			}
		});
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_booklist);
		mExitTime = System.currentTimeMillis(); // ��ǰʱ�䣬���������˳�

		init_handler(); // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ

		lv_booklist = getListView(); // ��ȡLV

		FoxDB.createDBIfNotExist(); // ������ݿⲻ���ڣ�������ṹ
		
		refresh_BookList(); // ˢ��LV�е�����

		init_LV_item_click(); // ��ʼ�� ���� ��Ŀ ����Ϊ
		init_LV_item_Long_click(); // ��ʼ�� ���� ��Ŀ ����Ϊ
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.booklist, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case R.id.action_updateall: // ��������
			upthreadcount = 0;
			upchacount = 0 ;
			Iterator<Map<String, Object>> itrl = data.iterator();
			HashMap<String, Object> jj;
			while (itrl.hasNext()) {
				jj = (HashMap<String, Object>) itrl.next();
				anowID = (Integer) jj.get("id");
				anowName = (String) jj.get("name");
				aisEnd = (Integer) jj.get("isend");
				if (1 != aisEnd) {
					++ upthreadcount;
					new Thread(new UpdateBook(anowID, anowName,true)).start();
				}
			}
			break;
		case R.id.action_vacuum:
			FoxDB.vacuumDB();
			foxtip("���ݿ�����С");
			break;
		case R.id.action_switchdb:
			FoxDB.switchDB();
			refresh_BookList(); // ˢ��LV�е�����
			foxtip("���ݿ����л�");
			break;
		case R.id.action_refresh:
			refresh_BookList(); // ˢ��LV�е�����
			foxtip("ListView��ˢ��");
			break;
		case R.id.action_searchbook:
			Intent intent = new Intent(Activity_BookList.this,
					Activity_SearchBook.class);
			startActivityForResult(intent,0);
			break;
		case R.id.action_allpagelist:
			Intent ittall = new Intent(Activity_BookList.this, Activity_AllPageList.class);
			ittall.putExtra("howmany", 0); // ��ʾ�����½�
			startActivityForResult(ittall, 0);
			break;
		case R.id.action_sortbook_asc: // ˳������
			foxtip("��ʼ����ID...");
			(new Thread(){ public void run(){
					FoxDB.regenID(1);
					Message msg = Message.obtain();
					msg.what = IS_REGENID;
					msg.obj = "�Ѱ�ҳ��ҳ��˳�����ź��鼮";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // ��������
			foxtip("��ʼ����ID...");
			(new Thread(){ public void run(){
				FoxDB.regenID(2);
				Message msg = Message.obtain();
				msg.what = IS_REGENID;
				msg.obj = "�Ѱ�ҳ��ҳ���������ź��鼮";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_regen_pageid:  // ����pageid
			foxtip("��ʼ����ID...");
			(new Thread(){ public void run(){
				FoxDB.regenID(9);
				Message msg = Message.obtain();
				msg.what = IS_REGENID;
				msg.obj = "������ҳ��ID";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_all2umd:
			setTitle("��ʼת����UMD...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2umd();
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "ȫ��ת�����: /sdcard/fox.umd";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2txt:
			setTitle("��ʼת����TXT...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2txt();
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "ȫ��ת�����: /sdcard/fox.txt";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_updatepkg:
			setTitle("��ʼ���°汾...");
			(new Thread(){
				public void run(){
					int newver = new FoxUpdatePkg(getApplicationContext()).FoxCheckUpdate() ;
					Message msg = Message.obtain();
					if ( newver > 0 ) {
						msg.what = IS_NEWVER;
						msg.obj = newver + ":�°汾" ;
					} else {
						msg.what = IS_MSG;
						msg.obj = "���°汾" ;
					}
					handler.sendMessage(msg);
				}
			}).start();

			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

	public boolean onKeyDown(int keyCoder, KeyEvent event) { // ������Ӧ
		if (keyCoder == KeyEvent.KEYCODE_BACK) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) { // �����˳������
				Toast.makeText(this, "�ٰ�һ�η��ؼ��˳�����", Toast.LENGTH_SHORT).show();
				mExitTime = System.currentTimeMillis();
			} else {
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

}

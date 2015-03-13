package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
	
	public FoxMemDB oDB  ; // Ĭ��ʹ��MemDB
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

	private int upchacount;    // �����½ڼ���
	
//	private final int SITE_EASOU = 11 ;
	private final int SITE_ZSSQ = 12 ;
	private final int SITE_KUAIDU = 13 ;
	
	// ����: isMemDB
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	public static final String FOXSETTING = "FOXSETTING";
	private boolean isMemDB = true;  // �Ƿ����ڴ����ݿ�
	private boolean isIntDB = false;  // �Ƿ����ڲ��洢�ռ�[����SD��]�б������ݿ�


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

				FoxBookLib.updatepage(nowID, nowURL, oDB);
				
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

	public class UpdateAllBook implements Runnable {
		public void run() {
			Message msg;
			ArrayList<Thread> threadList = new ArrayList<Thread>(30);
            Thread nowT;
            
        	int anowID, aisEnd; // ȫ����������ʹ�õı���
        	String anowName, anowURL;
        	
			upchacount = 0 ;
			Iterator<Map<String, Object>> itrl = data.iterator();
			HashMap<String, Object> jj;
			while (itrl.hasNext()) {
				jj = (HashMap<String, Object>) itrl.next();
				anowID = (Integer) jj.get("id");
				anowURL = (String) jj.get("url");
				anowName = (String) jj.get("name");
				aisEnd = (Integer) jj.get("isend");
				if (1 != aisEnd) {
					nowT = new Thread(new UpdateBook(anowID, anowURL, anowName,true));
					threadList.add(nowT);
					nowT.start();
				}
			}
			
            Iterator<Thread> itrT = threadList.iterator();
            while (itrT.hasNext()) {
                nowT = (Thread) itrT.next();
                try {
                    nowT.join();
                } catch (Exception ex) {
                    System.out.println("�ȴ��̴߳���: " + ex.toString());
                }
            }
            
			msg = Message.obtain();
			msg.what = IS_MSG;
			msg.obj = "�� " + upchacount + " ���½ڣ�ȫ���������" ;
			handler.sendMessage(msg);
		}
	}

	public class UpdateBook implements Runnable { // ��̨�̸߳�����
		private int bookid;
		private String bookname;
		private String bookurl ;
		private boolean bDownPage = true;

		UpdateBook(int inbookid, String inBookURL, String inbookname, boolean bDownPage) {
			this.bookid = inbookid;
			this.bookurl = inBookURL;
			this.bookname = inbookname;
			this.bDownPage = bDownPage;
		}

		@Override
		public void run() {
			List<Map<String, Object>> xx;
//			String bookurl = FoxDB.getOneCell("select url from book where id=" + String.valueOf(bookid)); // ��ȡ url
			String existList = FoxMemDBHelper.getPageListStr(bookid, oDB); // �õ��� list

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
				FoxMemDBHelper.inserNewPages(newPages, bookid, oDB); // ��ӵ����ݿ�
			}
			

			if (bDownPage) {
			List<Map<String, Object>> nbl = FoxMemDBHelper.getBookNewPages(bookid, oDB);
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

				FoxBookLib.updatepage(nowpageid, oDB);
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
					Activity_PageList.oDB = oDB;
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
				builder.setItems(new String[] { "���±���",
						"���±���Ŀ¼",
						"���߲鿴",
						"����:���",
						"����:�ѹ�",
						"����:���",
						"����:׷������",
						"����:����",
						"�༭������Ϣ",
						"��������",
						"ɾ������" },
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case 0:  // ���±���
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcURL, lcName, true)).start();
									foxtip("���ڸ���: " + lcName);
									break;
								case 1: // ���±���Ŀ¼
									upchacount = 0 ;
									new Thread(new UpdateBook(lcID, lcURL, lcName, false)).start();
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
									Activity_PageList.oDB = oDB;
									startActivity(intent);
									break;
								case 3: // ����:���
									String lcQidianID = oDB.getOneCell("select qidianid from book where id=" + lcID);
									String lcQidianURL = "";
									if ( 0 == lcQidianID.length() ) {
						                String json = FoxBookLib.downhtml(site_qidian.qidian_getSearchURL_Mobile(lcName), "utf-8");
						                List<Map<String, Object>> qds = site_qidian.json2BookList(json);
						                if ( qds.get(0).get("name").toString().equalsIgnoreCase(lcName) ) { // ��һ���������Ŀ����
											lcQidianURL = qds.get(0).get("url").toString();
						                }
									} else { // �������ID
										lcQidianURL = site_qidian.qidian_getIndexURL_Desk(Integer.valueOf(lcQidianID)) ;
									}
									if ( 0 != lcQidianURL.length() ) {
										Intent intentQD = new Intent(Activity_BookList.this, Activity_PageList.class);
										intentQD.putExtra("iam", FROM_NET);
										intentQD.putExtra("bookurl", lcQidianURL);
										intentQD.putExtra("bookname", lcName);
										intentQD.putExtra("searchengine", 1);
										Activity_PageList.oDB = oDB;
										startActivity(intentQD);
									} else {
										foxtip("�������δ������������");
									}
									break;
								case 4: // ����:sougou
									Intent intent7 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent7.putExtra("bookname", lcName);
									intent7.putExtra("searchengine", 1);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent7);
									break;
								case 5:  // ����:���
									Intent intent13 = new Intent(
											Activity_BookList.this,
											Activity_PageList.class);
									intent13.putExtra("iam", FROM_NET);
									intent13.putExtra("bookurl", lcURL);
									intent13.putExtra("bookname", lcName);
									intent13.putExtra("searchengine", SITE_KUAIDU);
									Activity_PageList.oDB = oDB;
									startActivity(intent13);
									break;
								case 6: // ����:׷������
									Intent intent9 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent9.putExtra("bookname", lcName);
									intent9.putExtra("searchengine", 12);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent9);
									break;
								case 7: // ����:easou
									Intent intent8 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent8.putExtra("bookname", lcName);
									intent8.putExtra("searchengine", 11);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent8);
									break;
								case 8:  // �༭������Ϣ
									Intent itti = new Intent(
											Activity_BookList.this,
											Activity_BookInfo.class);
									itti.putExtra("bookid", lcID);
									Activity_BookInfo.oDB = oDB;
									startActivityForResult(itti, 0);
									break;
								case 9:  // ��������
									ClipboardManager cbm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									cbm.setText(lcName);
									foxtip("�Ѹ��Ƶ�������:" + lcName);
									break;
								case 10: // ɾ������
									FoxMemDBHelper.deleteBook(lcID, oDB);
									refresh_BookList();
									foxtip("��ɾ����:" + lcName);
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
		data = FoxMemDBHelper.getBookList(oDB); // ��ȡ�鼮�б�
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
					refresh_BookList(); // ˢ��LV�е�����
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

		// ��ȡ���ã��Ƿ�ʹ���ڴ����ݿ�
        settings = getSharedPreferences(FOXSETTING, 0);
        editor = settings.edit();
        this.isMemDB = settings.getBoolean("isMemDB", isMemDB);
        this.isIntDB = settings.getBoolean("isIntDB", isIntDB);

		oDB = new FoxMemDB(this.isMemDB, this.isIntDB, this.getApplicationContext()) ; // Ĭ��ʹ��MemDB
		
		init_handler(); // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ

		lv_booklist = getListView(); // ��ȡLV

		refresh_BookList(); // ˢ��LV�е�����

		init_LV_item_click(); // ��ʼ�� ���� ��Ŀ ����Ϊ
		init_LV_item_Long_click(); // ��ʼ�� ���� ��Ŀ ����Ϊ
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.booklist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.action_isMemDB:
					menu.getItem(i).setChecked(this.isMemDB);
					break;
				case R.id.action_isIntDB:
					menu.getItem(i).setChecked(this.isIntDB);
					break;
				case R.id.action_intDB2SD:
					menu.getItem(i).setVisible(this.isIntDB);
					break;
				case R.id.action_SD2intDB:
					menu.getItem(i).setVisible(this.isIntDB);
					break;					
			}
		}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case R.id.action_updateall: // ��������
			new Thread(new UpdateAllBook()).start();
			break;
		case R.id.action_switchdb:
			this.setTitle("�л����ݿ�");
			(new Thread(){
				public void run(){
					String nowPath = oDB.switchMemDB();
					Message msg = Message.obtain();
					msg.what = IS_REGENID;
					msg.obj = "���л���: " + nowPath;
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_refresh:
			refresh_BookList(); // ˢ��LV�е�����
			foxtip("ListView��ˢ��");
			break;
		case R.id.action_searchbook:  // �������鼮
			Intent intent = new Intent(Activity_BookList.this,
					Activity_SearchBook.class);
			Activity_SearchBook.oDB = oDB;
			startActivityForResult(intent,0);
			break;
		case R.id.action_allpagelist:  // �����½�
			Intent ittall = new Intent(Activity_BookList.this, Activity_AllPageList.class);
			ittall.putExtra("howmany", 0); // ��ʾ�����½�
			Activity_AllPageList.oDB = oDB;
			startActivityForResult(ittall, 0);
			break;
		case R.id.action_sortbook_asc: // ˳������
			this.setTitle("˳������");
			(new Thread(){ public void run(){
					FoxMemDBHelper.regenID(1, oDB); // ˳��bookid
					FoxMemDBHelper.regenID(9, oDB); // ��������ҳ��ID
					Message msg = Message.obtain();
					msg.what = IS_REGENID;
					msg.obj = "�Ѱ�ҳ��ҳ��˳�����ź��鼮";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // ��������
			this.setTitle("��������");
			(new Thread(){ public void run(){
				FoxMemDBHelper.regenID(2, oDB); // ����bookid
				FoxMemDBHelper.regenID(9, oDB); // ��������ҳ��ID
				Message msg = Message.obtain();
				msg.what = IS_REGENID;
				msg.obj = "�Ѱ�ҳ��ҳ���������ź��鼮";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_isMemDB:
			isMemDB = ! item.isChecked() ;
			item.setChecked(isMemDB);
			editor.putBoolean("isMemDB", isMemDB);
			editor.commit();
			if (isMemDB) {
				foxtip("�л����ڴ����ݿ�ģʽ������������Ч");
			} else {
				foxtip("�л�����ͨģʽ������������Ч");
			}
			break;
		case R.id.action_all2epub:
			setTitle("��ʼת����EPUB...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2epub(oDB);
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "ȫ��ת�����: /sdcard/fox.epub";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2umd:
			setTitle("��ʼת����UMD...");
			(new Thread(){
				public void run(){
					FoxBookLib.all2umd(oDB);
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
					FoxBookLib.all2txt(oDB);
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
		case R.id.action_exitwithnosave:  // ���������ݿ��˳�
			this.finish();
			System.exit(0);
			break;
		case R.id.action_isIntDB:   // �Ƿ�ʹ���ڲ��洢
			isIntDB = ! item.isChecked() ;
			item.setChecked(isIntDB);
			editor.putBoolean("isIntDB", isIntDB);
			editor.commit();
			if (isIntDB) {
				foxtip("�л����ڲ��洢���ݿ�ģʽ������������Ч");
			} else {
				foxtip("�л���SD�����ݿ�ģʽ������������Ч");
			}
			break;
		case R.id.action_intDB2SD:  // �ڲ��洢->SD��
			setTitle("����: �ڲ��洢->SD��...");
			(new Thread(){
				public void run(){
					oDB.SD2Int(false);
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "��ϵ���: �ڲ��洢->SD��";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_SD2intDB:  // SD��->�ڲ��洢
			foxtip("�������Զ��˳�������");
			setTitle("����: SD��->�ڲ��洢...");
			(new Thread(){
				public void run(){
					oDB.SD2Int(true);
					Message msg = Message.obtain();
					msg.what = IS_MSG;
					msg.obj = "��ϵ���: SD��->�ڲ��洢";
					handler.sendMessage(msg);
					System.exit(0); // �������˳�
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
				foxtip("�ٰ�һ�η��ؼ��˳�����");
				mExitTime = System.currentTimeMillis();
			} else {
//				foxtip("�˳��У����ڱ������ݿ�..."); // ��ʾ����
				oDB.closeMemDB();
				this.finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}

}

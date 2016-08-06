package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ClipData;
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

@SuppressLint("SdCardPath")
public class Activity_BookList extends ListActivity {
	
	public FoxMemDB oDB  ; // Ĭ��ʹ��MemDB
	public int downThread = 9 ;  // ҳ�����������߳���
	public int leftThread = downThread ;

	private FoxHTTPD foxHTTPD  = null;
	private boolean bDB3FileFromIntent = false;  // �Ƿ���ͨ���ļ����������ģ����޸Ĳ��������ݿ��˳��˵�����
	
	ListView lv_booklist;
	List<Map<String, Object>> data;
	String lcURL, lcName; // long click �ı���
	Integer lcCount, lcID;
	private static Handler handler;
	private final int DO_SETTITLE = 1;
	private final int IS_NEWPAGE = 2;
	private final int DO_REFRESHLIST = 3;
	private final int DO_REFRESH_TIP = 4;
	private final int IS_NEWVER = 5;
	private final int DO_REFRESH_SETTITLE = 6 ;
	private final int DO_UPDATEFINISH = 7;
	private boolean switchdbLock = false;
	private long mExitTime;

	private int upchacount;    // �����½ڼ���
	
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

				FoxMemDBHelper.updatepage(nowID, nowURL, oDB);
				
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = leftThread + ":" + thName + ":" + locCount + " / " + allCount ;
				handler.sendMessage(msg);
			}
			--leftThread;
			if ( 0 == leftThread ) { // �����̸߳������
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = "�Ѹ��������пհ��½�>25" ;
				handler.sendMessage(msg);
			}
		}
	}

	public class UpdateAllBook implements Runnable {
		public void run() {
			Message msg;
			
			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = "�������..." ;
			handler.sendMessage(msg);
			ArrayList<HashMap<String, Object>> nn = FoxMemDBHelper.compareShelfToGetNew(oDB);
			if ( nn != null ) {
				int nnSize = nn.size() ;
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = "���: " + nnSize + " ������" ;
				handler.sendMessage(msg);
				if ( 0 == nnSize ) {
					return ;
				} else { 
					Iterator<HashMap<String, Object>> itrXX = nn.iterator();
					HashMap<String, Object> mm;
					int nowBID = 0;
					String nowName, nowURL;
					Thread nowTTT;
					while (itrXX.hasNext()) {
						mm = (HashMap<String, Object>) itrXX.next();
						nowBID =  (Integer)mm.get("id");
						nowName = (String) mm.get("name");
						nowURL = (String) mm.get("url");
//						nowPageList = (String) mm.get("pagelist");
						nowTTT = new Thread(new UpdateBook(nowBID, nowURL, nowName, true));
						nowTTT.start();
						try {
							nowTTT.join();
							msg = Message.obtain();
							msg.what = DO_SETTITLE;
							msg.obj = "����: " + nowName;
							handler.sendMessage(msg);
						} catch (InterruptedException e) {
							e.toString();
						}
					}
					msg = Message.obtain();
					msg.what = DO_UPDATEFINISH;
					msg.obj = "���: " + nnSize + " �Ѹ���" ;
					handler.sendMessage(msg);
					return ;
				}
			}
			
			
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
			msg.what = DO_UPDATEFINISH;
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
			String existList = FoxMemDBHelper.getPageListStr(bookid, oDB); // �õ��� list

			Message msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": ��������Ŀ¼ҳ";
			handler.sendMessage(msg);

			int site_type = 0 ;
            if ( bookurl.contains("3g.if.qidian.com") ) {
                site_type = SITES.SITE_QIDIAN_MOBILE ;
            }

			
			String html = "";
			switch(site_type) {
			case SITES.SITE_QIDIAN_MOBILE:
				html = FoxBookLib.downhtml(bookurl, "utf-8");
				xx = site_qidian.json2PageList(html);
				break;
			default:
				html = FoxBookLib.downhtml(bookurl); // ����url
				if (existList.length() > 3) {
					xx = FoxBookLib.tocHref(html, 55); // ������ȡ list ���55��
				} else {
					xx = FoxBookLib.tocHref(html, 0); // ������ȡ list �����½�
				}
			}

			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, Object>> newPages = (ArrayList<HashMap<String, Object>>)FoxBookLib.compare2GetNewPages(xx, existList) ;
			int newpagecount = newPages.size(); // ���½���������ͳ��

			if (newpagecount == 0) {
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = bookname + ": �����½�";
				handler.sendMessage(msg);
				handler.sendEmptyMessage(DO_REFRESHLIST); // ������ϣ�֪ͨˢ��
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
				msg.what = DO_SETTITLE;
				msg.obj = bookname + ": �����½�: " + nowCount + " / " + newpagecount ;
				handler.sendMessage(msg);

				FoxMemDBHelper.updatepage(nowpageid, oDB);
			}
			} // ���̸߳���
			} // bDownPage

			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": �������";
			handler.sendMessage(msg);

			handler.sendEmptyMessage(DO_REFRESHLIST); // ������ϣ�֪ͨˢ��
		}
	}

	private void init_LV_item_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		OnItemClickListener listener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> chapinfo = (HashMap<String, Object>) arg0.getItemAtPosition(arg2);
				String tmpurl = (String) chapinfo.get("url");
				String tmpname = (String) chapinfo.get("name");
				Integer tmpcount = Integer.parseInt((String) chapinfo.get("count"));
				Integer tmpid = (Integer) chapinfo.get("id");
				setTitle(tmpname + " : " + tmpurl);

				if (tmpcount > 0) {
					Intent intent = new Intent(Activity_BookList.this,
							Activity_PageList.class);
					intent.putExtra("iam", SITES.FROM_DB);
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
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> chapinfol = (HashMap<String, Object>) parent.getItemAtPosition(position);
				lcURL = (String) chapinfol.get("url");
				lcName = (String) chapinfol.get("name");
				lcCount = Integer.parseInt((String) chapinfol.get("count"));
				lcID = (Integer) chapinfol.get("id");
				setTitle(lcName + " : " + lcCount);

				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("����:" + lcName);
				builder.setItems(new String[] { "���±���",
						"���±���Ŀ¼",
						"���߲鿴",
						"����:���",
						"����:bing",
						"��������",
						"�༭������Ϣ",
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
									Intent intent = new Intent(Activity_BookList.this, Activity_PageList.class);
									intent.putExtra("iam", SITES.FROM_NET);
									intent.putExtra("bookurl", lcURL);
									intent.putExtra("bookname", lcName);
									if ( lcURL.contains("3g.if.qidian.com") ) {
										intent.putExtra("searchengine", SITES.SE_QIDIAN_MOBILE);
									}
									Activity_PageList.oDB = oDB;
									startActivity(intent);
									break;
								case 3: // ����:���
									String lcQidianID = oDB.getOneCell("select qidianid from book where id=" + lcID);
									String lcQidianURL = "";
									if ( null == lcQidianID || 0 == lcQidianID.length() ) {
						                String json = FoxBookLib.downhtml(site_qidian.qidian_getSearchURL_Mobile(lcName), "utf-8");
						                List<Map<String, Object>> qds = site_qidian.json2BookList(json);
						                if ( qds.get(0).get("name").toString().equalsIgnoreCase(lcName) ) { // ��һ���������Ŀ����
											lcQidianURL = qds.get(0).get("url").toString();
						                }
									} else { // �������ID
										lcQidianURL = site_qidian.qidian_getIndexURL_Mobile(Integer.valueOf(lcQidianID)) ;
									}
									if ( 0 != lcQidianURL.length() ) {
										Intent intentQD = new Intent(Activity_BookList.this, Activity_PageList.class);
										intentQD.putExtra("iam", SITES.FROM_NET);
										intentQD.putExtra("bookurl", lcQidianURL);
										intentQD.putExtra("bookname", lcName);
										intentQD.putExtra("searchengine", SITES.SE_QIDIAN_MOBILE);
										Activity_PageList.oDB = oDB;
										startActivity(intentQD);
									} else {
										foxtip("�������δ������������");
									}
									break;
								case 4: // ����:bing
									Intent intent7 = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
									intent7.putExtra("bookname", lcName);
									intent7.putExtra("searchengine", SITES.SE_BING);
									Activity_QuickSearch.oDB = oDB;
									startActivity(intent7);
									break;
								case 5:  // ��������
									copyToClipboard(lcName);
									foxtip("�Ѹ��Ƶ�������: " + lcName);
									break;
								case 6:  // �༭������Ϣ
									Intent itti = new Intent(
											Activity_BookList.this,
											Activity_BookInfo.class);
									itti.putExtra("bookid", lcID);
									Activity_BookInfo.oDB = oDB;
									startActivityForResult(itti, 0);
									break;
								case 7: // ɾ������
									FoxMemDBHelper.deleteBook(lcID, oDB);
									refresh_BookList();
									foxtip("��ɾ��: " + lcName);
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
				case DO_UPDATEFINISH:
					setTitle((String)msg.obj);
					String xCount = oDB.getOneCell("select count(id) from page where length(content) < 999");
					if ( Integer.parseInt(xCount) > 0 ) {
						setTitle(xCount + ":" + (String)msg.obj);
						foxtip("�� " + xCount + " �½ڶ���1K");
					}
					break;
				case DO_SETTITLE:
					setTitle((String)msg.obj);
					break;
				case DO_REFRESHLIST:
					refresh_BookList();
					break;
				case DO_REFRESH_TIP :
					refresh_BookList();
					foxtip((String) msg.obj);
					break;
				case DO_REFRESH_SETTITLE :
					refresh_BookList();
					setTitle((String) msg.obj);
					break;
				case IS_NEWPAGE:
					upchacount += (Integer) msg.arg1;
					setTitle((String) msg.obj);
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

		// ��ȡ�����·��(����db3�ļ�)
		String db3PathIn = "none" ;
		try {
			db3PathIn = getIntent().getData().getPath();
		} catch (Exception e) {
			e.toString();
		}

		// ��ȡ���ã��Ƿ�ʹ���ڴ����ݿ�
        settings = getSharedPreferences(FOXSETTING, 0);
        editor = settings.edit();
        this.isMemDB = settings.getBoolean("isMemDB", isMemDB);
        this.isIntDB = settings.getBoolean("isIntDB", isIntDB);

        if ( db3PathIn.equalsIgnoreCase("none")) {
        	bDB3FileFromIntent = false;
        	oDB = new FoxMemDB(this.isMemDB, this.isIntDB, this.getApplicationContext()) ; // Ĭ��ʹ��MemDB
        } else {
        	bDB3FileFromIntent = true;
        	File inDB3File = new File(db3PathIn);
        	oDB = new FoxMemDB(inDB3File, this.getApplicationContext()) ; // ��DB3
        	setTitle(inDB3File.getName());
        	foxtip("ע��:\n�˳�ʱ���ᱣ�����ݿ���޸�Ŷ\n��Ҫ�����޸ģ����˵�����ѡ��˵�");
        }
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
				case R.id.action_exitwithnosave:
					if ( bDB3FileFromIntent ) {
						menu.getItem(i).setTitle("�������ݿⲢ�˳�");
					}
					break;
				case R.id.action_switchdb:
					if ( bDB3FileFromIntent ) {
						menu.getItem(i).setVisible(false);
					}
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
			if ( switchdbLock ) {
				foxtip("�����л���...");
			} else {
				(new Thread(){
					public void run(){
						switchdbLock = true;
						String nowPath = oDB.switchMemDB().getName().replace(".db3", "");
						switchdbLock = false;
						Message msg = Message.obtain();
						msg.what = DO_REFRESH_SETTITLE;
						msg.obj = nowPath;
						handler.sendMessage(msg);
					}
				}).start();
			}
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
			ittall.putExtra("apl_showtype", Activity_AllPageList.SHOW_ALL);
			Activity_AllPageList.oDB = oDB;
			startActivityForResult(ittall, 0);
			break;
		case R.id.action_showokinapl:  // ��ʾ��������1K���½�
			Intent ittlok = new Intent(Activity_BookList.this, Activity_AllPageList.class);
			ittlok.putExtra("apl_showtype", Activity_AllPageList.SHOW_LESS1K);
			Activity_AllPageList.oDB = oDB;
			startActivityForResult(ittlok, 0);
			break;
			
		case R.id.action_sortbook_asc: // ˳������
			this.setTitle("˳������");
			(new Thread(){ public void run(){
					FoxMemDBHelper.regenID(1, oDB); // ˳��bookid
					FoxMemDBHelper.regenID(9, oDB); // ��������ҳ��ID
					FoxMemDBHelper.simplifyAllDelList(oDB);
					Message msg = Message.obtain();
					msg.what = DO_REFRESH_TIP;
					msg.obj = "�Ѱ�ҳ��ҳ��˳�����ź��鼮";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // ��������
			this.setTitle("��������");
			(new Thread(){ public void run(){
				FoxMemDBHelper.regenID(2, oDB); // ����bookid
				FoxMemDBHelper.regenID(9, oDB); // ��������ҳ��ID
				FoxMemDBHelper.simplifyAllDelList(oDB);
				Message msg = Message.obtain();
				msg.what = DO_REFRESH_TIP;
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
					FoxMemDBHelper.all2epub(oDB);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "ȫ��ת�����: /sdcard/fox.epub";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2umd:
			setTitle("��ʼת����UMD...");
			(new Thread(){
				public void run(){
					FoxMemDBHelper.all2umd(oDB);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "ȫ��ת�����: /sdcard/fox.umd";
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2txt:
			setTitle("��ʼת����TXT...");
			(new Thread(){
				public void run(){
					FoxMemDBHelper.all2txt(oDB);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
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
						msg.what = DO_SETTITLE;
						msg.obj = "���°汾" ;
					}
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_exitwithnosave:  // ���������ݿ��˳�
			if ( bDB3FileFromIntent ) { // �������ݿⲢ�˳�
				beforeExitApp();
			}
			this.finish();
			System.exit(0);
			break;
		case R.id.action_foxhttpd: // ����ֹͣ������
			int nowListenPort = 8888 ;
			String nowIP = FoxUpdatePkg.getLocalIpAddress();
			boolean bStartIt = ! item.isChecked(); // ����ѡ���Ƿ�ѡ��ȷ���Ƿ���
			if (bStartIt) {
				try {
					foxHTTPD = new FoxHTTPD(nowListenPort, new File("/sdcard/"), oDB);
					item.setChecked(bStartIt);
					item.setTitle(nowIP + ":" + String.valueOf(nowListenPort) + " �ѿ�");
					foxtip(nowIP + ":" + String.valueOf(nowListenPort) + " �ѿ�\n��Ҫ�رգ�ѡ��ͬһ�˵�");
				} catch (Exception e) {
					e.toString();
				}
			} else {
				if (foxHTTPD != null) {
					foxHTTPD.stop();
					item.setChecked(bStartIt);
					item.setTitle(nowIP + ":" + String.valueOf(nowListenPort) + " �ѹ�");
				}
			}
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
					msg.what = DO_SETTITLE;
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
					msg.what = DO_SETTITLE;
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
				if ( ! bDB3FileFromIntent ) { // ���������ݿⲢ�˳�
					beforeExitApp();
				}
				this.finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}
	private void beforeExitApp() {
		oDB.closeMemDB();
		if (foxHTTPD != null) {
			foxHTTPD.stop();
		}
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void copyToClipboard(String iText) {
		((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", iText));
	}

}

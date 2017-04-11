package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.Ext_ListActivity_4Eink;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolBookJava;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/*
��������: Action, Datas
Activity_PageList : �����б� 		: aListBookPages, bookIDX
Activity_PageList : ��ʾ������		: aListAllPages
Activity_PageList : ��ʾС��1K��	: aListLess1KPages
Activity_PageList : ���߲鿴		: aListSitePages, bookIDX
Activity_PageList : �������		: aListQDPages, bookIDX, TmpString
Activity_SearchBook : �������		: aListQDPages, bookName
Activity_BookInfo : �༭��Ϣ		: bookIDX

Activity_QuickSearch : ����Bing		: atSearch
*/
public class Activity_BookList extends Ext_ListActivity_4Eink {

	private NovelManager nm ;
	File wDir ;			// ����Ŀ¼
	File cookiesFile ;	// ������cookie���ļ���: FoxBook.cookie

	public int downThread = 9 ; // ҳ�����������߳���
	public int leftThread = downThread ;

	// ����: 
	SharedPreferences settings;
	private String beforeSwitchShelf = "orderby_count_desc" ; // �� arrays.xml�е�beforeSwitchShelf_Values ��Ӧ
	private boolean isUpdateBlankPagesFirst = true; // ����ǰ�ȼ���Ƿ��пհ��½�
	private boolean isCompareShelf = true ;		// ����ǰ�Ƚ����
	private boolean isShowIfRoom = false;		// һֱ��ʾ�˵�ͼ��

	private FoxHTTPD foxHTTPD = null;
	private boolean bShelfFileFromIntent = false; // �Ƿ���ͨ���ļ����������ģ����޸Ĳ������˳��˵�����

	ListView lv_booklist;
	List<Map<String, Object>> data;

	private static Handler handler;
	private final int DO_SETTITLE = 1;
	private final int IS_NEWPAGE = 2;
	private final int DO_REFRESHLIST = 3;
	private final int DO_REFRESH_TIP = 4;
	private final int IS_NEWVER = 5;
	private final int DO_REFRESH_SETTITLE = 6 ;
	private final int DO_UPDATEFINISH = 7;

	private boolean switchShelfLock = false;
	private long mExitTime;

	private int upchacount; // �����½ڼ���

	public class UpdateAllBook implements Runnable {
		public void run() {
			Message msg;

			isUpdateBlankPagesFirst = settings.getBoolean("isUpdateBlankPagesFirst", isUpdateBlankPagesFirst);
			if ( isUpdateBlankPagesFirst ) {
				for (Map<String, Object> blankPage : nm.getPageList(99) ) {
					msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "���: " + (String)blankPage.get(NV.PageName);
					handler.sendMessage(msg);
					nm.updatePage((Integer)blankPage.get(NV.BookIDX), (Integer)blankPage.get(NV.PageIDX));
				}
			}

			isCompareShelf = settings.getBoolean("isCompareShelf", isCompareShelf); // ����ǰ�Ƚ����
if ( isCompareShelf ) {
			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = "�������..." ;
			handler.sendMessage(msg);
			List<Map<String, Object>> nn = new NovelSite().compareShelfToGetNew(nm.getBookListForShelf(), cookiesFile);
			if ( nn != null ) {
				int nnSize = nn.size() ;
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = "���: " + nnSize + " ������" ;
				handler.sendMessage(msg);
				if ( 0 == nnSize ) {
					return ;
				} else { 
					int nowBookIDX = -1;
					String nowName, nowURL;
					Thread nowTTT;
					for ( Map<String, Object> mm : nn ) {
						nowBookIDX = (Integer)mm.get(NV.BookIDX);
						nowName = mm.get(NV.BookName).toString();
						nowURL = mm.get(NV.BookURL).toString();
						nowTTT = new Thread(new UpdateBook(nowBookIDX, nowURL, nowName, true));
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
}

			List<Thread> threadList = new ArrayList<Thread>(30);
			Thread nowT;

			// ȫ����������ʹ�õı���
			int nowBookIDX = -1;
			String anowName, anowURL;
			upchacount = 0 ;
			for ( Map<String, Object> jj : data ) {
				nowBookIDX = (Integer) jj.get(NV.BookIDX);
				anowURL = (String) jj.get(NV.BookURL);
				anowName = (String) jj.get(NV.BookName);
				if ( (Integer)jj.get(NV.BookStatu) != 1 ) {
					nowT = new Thread(new UpdateBook(nowBookIDX, anowURL, anowName,true));
					threadList.add(nowT);
					nowT.start();
				}
			}

			for ( Thread nowThread : threadList ) {
				try {
					nowThread.join();
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
		private int bookIDX = 0 ;
		private String bookname ;
		private String bookurl ;
		private boolean bDownPage = true;

		UpdateBook(int inbookidx, String inBookURL, String inbookname, boolean bDownPage) {
			this.bookIDX = inbookidx;
			this.bookurl = inBookURL;
			this.bookname = inbookname;
			this.bDownPage = bDownPage;
		}

		@Override
		public void run() {
			Message msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": ����Ŀ¼ҳ";
			handler.sendMessage(msg);

			String existList = nm.getPageListStr(bookIDX); // �õ��� list
			List<Map<String, Object>> linkList;
			if ( bookurl.contains(".if.qidian.com") ) {
				linkList = new SiteQiDian().getTOC_Android7(ToolBookJava.downhtml(bookurl, "utf-8"));
			} else {
				linkList = new NovelSite().getTOC(ToolBookJava.downhtml(bookurl)); // ������ȡ list �����½�
				if ( existList.length() > 3 ) {
					if ( nm.getBookInfo(bookIDX).get(NV.BookAuthor).toString().length() > 1 ) // ������������ʾΪ����
						linkList = ToolBookJava.getLastNPage(linkList, 55); // ��ȡ list ���55��
				}
			}

			List<Map<String, Object>> newPages = ToolBookJava.compare2GetNewPages(linkList, existList) ;
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

			List<Map<String, Object>> nbl = nm.addBookBlankPageList(newPages, bookIDX);
		if (bDownPage) {
			int cTask = nbl.size() ; // ��������
			
			if ( cTask > 25 ) { // �����½������� 25�¾Ͳ��ö���������ģʽ
				int nBaseCount = cTask / downThread ; //ÿ�̻߳���������
				int nLeftCount = cTask % downThread ; //ʣ��������
				int aList[] = new int[downThread] ; // ÿ���߳��е�������

				for ( int i = 0; i < downThread; i++ ) { // ����������
					if ( i < nLeftCount )
						aList[i] = nBaseCount + 1 ;
					else
						aList[i] = nBaseCount ;
				}

				List<Map<String, Object>> subList ;
				int startPoint = 0 ;
				for ( int i = 0; i < downThread; i++ ) {
					if ( aList[i] == 0 ) { // ���������������������߳��ٵ������
						--leftThread ;
						continue ;
					}
					subList = new ArrayList<Map<String, Object>>(aList[i]);
					for ( int n = startPoint; n < startPoint + aList[i]; n++ )
						subList.add(nbl.get(n));
					(new Thread(new FoxTaskDownPage(subList), "T" + i)).start() ;

					startPoint += aList[i] ;
				}
			} else { // ���߳�ѭ������ҳ��
				int nowCount = 0;
				for (Map<String, Object> blankPage : nbl){
					++nowCount;
					msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = bookname + ": �����½�: " + nowCount + " / " + newpagecount ;
					handler.sendMessage(msg);

					nm.updatePage(bookIDX, (Integer)blankPage.get(NV.PageIDX));
				}
			} // ���̸߳��� end
		} // bDownPage

			msg = Message.obtain();
			msg.what = DO_SETTITLE;
			msg.obj = bookname + ": �������";
			handler.sendMessage(msg);

			handler.sendEmptyMessage(DO_REFRESHLIST); // ������ϣ�֪ͨˢ��
		}
	}

	public class FoxTaskDownPage implements Runnable { // ���߳��������ҳ���б�
		List<Map<String, Object>> taskList;
	
		public FoxTaskDownPage(List<Map<String, Object>> iTaskList) {
			this.taskList = iTaskList ;
		}

		public void run() {
			Message msg;
			String thName = Thread.currentThread().getName();
			int locCount = 0 ;
			int allCount = taskList.size();
			for (Map<String, Object> tsk : taskList) {
				++ locCount ;
				msg = Message.obtain();
				msg.what = DO_SETTITLE;
				msg.obj = leftThread + ":" + thName + ":" + locCount + " / " + allCount ;
				handler.sendMessage(msg);

				nm.updatePage((Integer)tsk.get(NV.BookIDX), (Integer)tsk.get(NV.PageIDX));
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

	private void refresh_BookList() { // ˢ��LV�е�����
		data = nm.getBookList(); // ��ȡ�鼮�б�
		lv_booklist.setAdapter(new SimpleAdapter(this, data, R.layout.lv_item_booklist
			, new String[] { NV.BookName, NV.PagesCount }
			, new int[] { R.id.tvName, R.id.tvCount } )); // ����listview��Adapter, ��data��ԭdataʱ���� adapter.notifyDataSetChanged();
		setItemPos4Eink();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(settings.getBoolean("isClickHomeExit", false)); // ����������ӷ���ͼ��
		getActionBar().setDisplayShowHomeEnabled(settings.getBoolean("isShowAppIcon", true)); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home this.finish();

	public void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_booklist);

		showHomeUp();
		isShowIfRoom = settings.getBoolean("isShowIfRoom", isShowIfRoom);
		lv_booklist = getListView(); // ��ȡLV
// GUI ������ʾ���
		mExitTime = System.currentTimeMillis(); // ��ǰʱ�䣬���������˳�
		this.wDir = ToolAndroid.getDefaultDir(settings);
		this.cookiesFile = new File(wDir, "FoxBook.cookie");

		File inShelfFile; // �����·��(db3/fml�ļ�)
		if ( getIntent().getData() == null ) {
			bShelfFileFromIntent = false;
			inShelfFile = new File(this.wDir, "FoxBook.fml");
			if ( ! inShelfFile.exists() ) {
				inShelfFile = new File(this.wDir, "FoxBook.db3");
				if ( ! inShelfFile.exists() )
					inShelfFile = new File(this.wDir, "FoxBook.fml");
			}
		 } else {
			bShelfFileFromIntent = true;
			inShelfFile = new File(getIntent().getData().getPath());
			foxtip("ע��:\n�˳�ʱ���ᱣ���޸�Ŷ\n��Ҫ�����޸ģ����˵�����ѡ��˵�");
		}
		setTitle(inShelfFile.getName());
		this.nm = new NovelManager(inShelfFile); // Todo: �޸�db���뷽ʽ
		((FoxApp)this.getApplication()).nm = this.nm ;

		refresh_BookList();
		init_handler(); // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ

		// ��ʼ�� ���� ��Ŀ ����Ϊ
		lv_booklist.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> book = (HashMap<String, Object>) parent.getItemAtPosition(position);
				setTitle(book.get(NV.BookName) + " : " + book.get(NV.PagesCount));
				lvItemLongClickDialog(book);
				return true;
			}
		}); // long click end

		if ( 0 == data.size() ) { // û���飬��ת������ҳ��
			foxtip("ò��û����Ŷ����ż�Լ�����������");
			startActivityForResult(new Intent(Activity_BookList.this, Activity_SearchBook.class),4);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> book = (HashMap<String, Object>) l.getItemAtPosition(position);
		setTitle(book.get(NV.BookName) + " : " + book.get(NV.BookURL));

		if ( (Integer)book.get(NV.PagesCount) > 0) { // �½�������0
			Intent itt = new Intent(Activity_BookList.this, Activity_PageList.class);
			itt.putExtra(AC.action, AC.aListBookPages);
			itt.putExtra(NV.BookIDX, (Integer)book.get(NV.BookIDX));
			startActivityForResult(itt, 1);
		}
		super.onListItemClick(l, v, position, id);
	}

	private void lvItemLongClickDialog(final Map<String, Object> book) { // ����LV��Ŀ�����ĶԻ���
		final String lcURL = book.get(NV.BookURL).toString();
		final String lcName = book.get(NV.BookName).toString();
		final int bookIDX = (Integer)book.get(NV.BookIDX);

		new AlertDialog.Builder(this) //.setIcon(R.drawable.ic_launcher);
		.setTitle("����:" + lcName)
		.setItems(new String[] { "���±���",
				"���±���Ŀ¼",
				"���߲鿴",
				"����:���",
				"����:bing",
				"��������",
				"�༭������Ϣ",
				"ɾ������" },
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0: // ���±���
					upchacount = 0 ;
					new Thread(new UpdateBook(bookIDX, lcURL, lcName, true)).start();
					foxtip("���ڸ���: " + lcName);
					break;
				case 1: // ���±���Ŀ¼
					upchacount = 0 ;
					new Thread(new UpdateBook(bookIDX, lcURL, lcName, false)).start();
					foxtip("���ڸ���Ŀ¼: " + lcName);
					break;
				case 2: // ���߲鿴
					Intent itt = new Intent(Activity_BookList.this, Activity_PageList.class);
					itt.putExtra(AC.action, AC.aListSitePages);
					itt.putExtra(NV.BookIDX, bookIDX);
					startActivityForResult(itt, 1);
					break;
				case 3: // ����:���
					String lcQidianID = nm.getBookInfo(bookIDX).get(NV.QDID).toString();

					if ( null == lcQidianID | 0 == lcQidianID.length() | lcQidianID == "0" ) {
						Intent ittQDS = new Intent(Activity_BookList.this, Activity_SearchBook.class);
						ittQDS.putExtra(AC.action, AC.aListQDPages);
						ittQDS.putExtra(NV.BookName, lcName);
						startActivity(ittQDS);
					} else { // �������ID
						String lcQidianURL = new SiteQiDian().getTOCURL_Android7(lcQidianID) ;

						Intent ittQD = new Intent(Activity_BookList.this, Activity_PageList.class);
						ittQD.putExtra(AC.action, AC.aListQDPages);
						ittQD.putExtra(NV.BookIDX, bookIDX);
						ittQD.putExtra(NV.TmpString, lcQidianURL);
						startActivityForResult(ittQD, 1);
					}
					break;
				case 4: // ����:bing
					Intent ittSEB = new Intent(Activity_BookList.this, Activity_QuickSearch.class);
					ittSEB.putExtra(NV.BookName, lcName);
					ittSEB.putExtra(AC.searchEngine, AC.SE_BING);
					startActivityForResult(ittSEB, 5);
					break;
				case 5: // ��������
					ToolAndroid.setClipText(lcName, getApplicationContext());
					foxtip("�Ѹ��Ƶ�������: " + lcName);
					break;
				case 6: // �༭������Ϣ
					Intent ittBookInfo = new Intent(Activity_BookList.this,Activity_BookInfo.class);
					ittBookInfo.putExtra(NV.BookIDX, bookIDX);
					startActivityForResult(ittBookInfo, 3);
					break;
				case 7: // ɾ������
					nm.deleteBook(bookIDX);
					refresh_BookList();
					foxtip("��ɾ��: " + lcName);
					break;
				}
			}
		})
		.create().show();
	}

	private void init_handler() { // ��ʼ��һ��handler ���ڴ����̨�̵߳���Ϣ
		handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
				case DO_UPDATEFINISH:
					setTitle((String)msg.obj);
					int xCount = nm.getLess1KCount();
					if ( xCount > 0 ) {
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
						i.setDataAndType(Uri.fromFile(new File(wDir, "FoxBook.apk")), "application/vnd.android.package-archive"); 
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.booklist, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.action_exitwithnosave:
					if ( bShelfFileFromIntent )
						menu.getItem(i).setTitle("���沢�˳�");
					break;
				case R.id.action_switchShelf:
					if ( bShelfFileFromIntent )
						menu.getItem(i).setVisible(false);
					if ( isShowIfRoom )
						setTypeOfShowAsAction(menu.getItem(i));
					break;
				case R.id.action_allpagelist:
					if ( isShowIfRoom )
						setTypeOfShowAsAction(menu.getItem(i));
					break;
				case R.id.action_updateall:
					if ( isShowIfRoom )
						setTypeOfShowAsAction(menu.getItem(i));
					break;
			}
		}
		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setTypeOfShowAsAction(MenuItem mi) {
		mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM); // SHOW_AS_ACTION_NEVER // API > 11
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		case R.id.setting:
			startActivity(new Intent(Activity_BookList.this, Activity_Setting.class));
			break;
		case R.id.action_updateall: // ��������
			new Thread(new UpdateAllBook()).start();
			break;
		case R.id.action_switchShelf:
			this.setTitle("�л����ݿ�");
			if ( switchShelfLock ) {
				foxtip("�����л���...");
			} else {
				(new Thread(){
					public void run(){
						switchShelfLock = true;
						beforeSwitchShelf = settings.getString("beforeSwitchShelf", beforeSwitchShelf);
						if ( ! beforeSwitchShelf.equalsIgnoreCase("none") ) { // �л�ǰ������
							if ( beforeSwitchShelf.equalsIgnoreCase("orderby_count_desc") )
								nm.sortBooks(true);
							if ( beforeSwitchShelf.equalsIgnoreCase("orderby_count_asc") )
								nm.sortBooks(false);
							nm.simplifyAllDelList();
						}
						String nowPath = nm.switchShelf().getName();
						switchShelfLock = false;
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
		case R.id.action_searchbook: // �������鼮
			startActivityForResult(new Intent(Activity_BookList.this, Activity_SearchBook.class),4);
			break;

		case R.id.action_allpagelist: // �����½�
			Intent ittall = new Intent(Activity_BookList.this, Activity_PageList.class);
			ittall.putExtra(AC.action, AC.aListAllPages);
			startActivityForResult(ittall, 2);
			break;
		case R.id.action_showokinapl: // ��ʾ��������1K���½�
			Intent ittlok = new Intent(Activity_BookList.this, Activity_PageList.class);
			ittlok.putExtra(AC.action, AC.aListLess1KPages);
			startActivityForResult(ittlok, 2);
			break;

		case R.id.action_sortbook_asc: // ˳������
			this.setTitle("˳������");
			(new Thread(){ public void run(){
					nm.sortBooks(false);
					nm.simplifyAllDelList();
					Message msg = Message.obtain();
					msg.what = DO_REFRESH_TIP;
					msg.obj = "�Ѱ�ҳ��ҳ��˳�����ź��鼮";
					handler.sendMessage(msg);
				} }).start();
			break;
		case R.id.action_sortbook_desc: // ��������
			this.setTitle("��������");
			(new Thread(){ public void run(){
				nm.sortBooks(true);
				nm.simplifyAllDelList();
				Message msg = Message.obtain();
				msg.what = DO_REFRESH_TIP;
				msg.obj = "�Ѱ�ҳ��ҳ���������ź��鼮";
				handler.sendMessage(msg);
			} }).start();
			break;
		case R.id.action_all2epub:
			setTitle("��ʼת����EPUB...");
			(new Thread(){
				public void run(){
					File oFile = new File(wDir, "fox.epub");
					nm.exportAsEpub(oFile);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "ȫ��ת�����: " + oFile.getPath();
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case R.id.action_all2txt:
			setTitle("��ʼת����TXT...");
			(new Thread(){
				public void run(){
					File oFile = new File(wDir, "fox.txt");
					nm.exportAsTxt(oFile);
					Message msg = Message.obtain();
					msg.what = DO_SETTITLE;
					msg.obj = "ȫ��ת�����: " + oFile.getPath();
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
		case R.id.action_exitwithnosave: // ���������ݿ��˳�
			if ( bShelfFileFromIntent ) // �������ݿⲢ�˳�
				beforeExitApp();
			this.finish();
			System.exit(0);
			break;
		case R.id.action_foxhttpd: // ����ֹͣ������
			int nowListenPort = 8888 ;
			String nowIP = "127.0.0.1" ;
			HashMap<String, String> hmwifi = ToolAndroid.getWifiInfo(getApplicationContext());
			if ( null != hmwifi )
				nowIP = hmwifi.get("ip");
			boolean bStartIt = ! item.isChecked(); // ����ѡ���Ƿ�ѡ��ȷ���Ƿ���
			if (bStartIt) {
				try {
					foxHTTPD = new FoxHTTPD(nowListenPort, wDir, nm);
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
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent retIntent) {
		if (RESULT_OK == resultCode)
			refresh_BookList(); // ˢ��LV�е�����
	}

	private void beforeExitApp() {
		if ( ! settings.getBoolean("isSaveAsFML", true) )
			nm.setSaveFormat(NovelManager.SQLITE3);
		nm.close();
		if (foxHTTPD != null)
			foxHTTPD.stop();
	}

	@Override
	public void onBackPressed() { // ���ؼ�����
		if ((System.currentTimeMillis() - mExitTime) > 2000) { // �����˳������
			foxtip("�ٰ�һ���˳�����");
			mExitTime = System.currentTimeMillis();
		} else {
			if ( ! bShelfFileFromIntent ) // ���������ݿⲢ�˳�
				beforeExitApp();
			this.finish();
			System.exit(0);
		}
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}

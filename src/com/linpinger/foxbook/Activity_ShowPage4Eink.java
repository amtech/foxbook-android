package com.linpinger.foxbook;

import java.io.File;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Activity_ShowPage4Eink extends Activity {
	public static FoxMemDB oDB;

	FoxTextView mv;
	private float cX = 0 ; // ���View������
	private float cY = 0 ; // ���View������

	private int foxfrom = 0 ;  // 1=DB, 2=search 
	private int bookid = 0 ; // ��ҳʱʹ��
	private int pageid = 0 ;
	private String pagetext = "��ȱ" ;
	private String pagename = "" ;
	private String pageurl = "" ;

	private String bookname = "";
	private String allpagescount = "0" ;

	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private String myBGcolor = "default" ;  // ����:Ĭ����Ƥֽ
	private boolean isMapUpKey = false; // �Ƿ�ӳ���Ϸ�Ϊ�·���
	private float fontsize = 36.0f; // �����С
	private float paddingMultip  = 0.5f ; // ҳ�߾� = �����С * paddingMultip
	private float lineSpaceingMultip = 1.5f ; // �м�౶��
	private boolean isProcessLongOneLine = true; // ���������� > 4K

	private long tLastPushEinkButton ;

	private final int IS_REFRESH = 5 ;

	private int SE_TYPE = 1; // ��������
	
	private File ZIPFILE ;


	private class FoxTextView extends Ext_View_FoxTextView {

		public FoxTextView(Context context) {
			super(context);
		}

		private int setPrevOrNextText(boolean isNextPage) {
			String strNoMoreTip ;
			String whereStrA;
			String whereStrB;
			String addSQL = " and content is not null ";

			if ( foxfrom == SITES.FROM_ZIP )
				addSQL = "" ;

			if ( isNextPage ) {
				strNoMoreTip = "�ף�û����һҳ��";
				whereStrA = "id > " + pageid + " and bookid = " + bookid + addSQL + " limit 1" ;
				whereStrB = "page.bookid=book.id and bookid > " + bookid + addSQL + " order by bookid, id limit 1";
			} else {
				strNoMoreTip = "�ף�û����һҳ��";
				whereStrA = "id < " + pageid + " and bookid = " + bookid + addSQL + " order by id desc limit 1" ;
				whereStrB = "page.bookid=book.id and bookid < " + bookid + addSQL + " order by bookid desc, id desc limit 1";
			}

			if ( 0 == pageid ) {
				foxtip("�ף�ID Ϊ 0");
				return -1;
			}
			

			Map<String,String> pp ;
			pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where " + whereStrA); // ����
			if ( null == pp.get("id") ) {
				pp = oDB.getOneRow("select page.id as id, page.bookid as bid, page.name as name, page.url as url, page.content as content, book.name as bnn from book, page where " + whereStrB);
				if ( null == pp.get("name") ) {
					foxtip(strNoMoreTip);
					return -2;
				}
				bookname = pp.get("bnn");
			}
			pageid = Integer.valueOf(pp.get("id"));
			bookid = Integer.valueOf(pp.get("bid"));
			pagename = pp.get("name");
			pagetext = pp.get("content");

			if ( foxfrom == SITES.FROM_ZIP ) {
				String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, pp.get("url"));
				if ( html.contains("\"tpc_content\"") ) {
					HashMap<String, Object> cc = ToolBookJava.getPage1024(html);
					pagetext = cc.get("content").toString();
					pagename = cc.get("title").toString();
				}
			}

			pagetext = ProcessLongOneLine(pagetext);
			mv.setText(pagename, "����" + pagetext.replace("\n", "\n����"), bookname + "   " + pageid + " / " + allpagescount);
			return 0;
		}

		@Override
		public int setPrevText() {
			return setPrevOrNextText(false); // ��һ
		}

		@Override
		public int setNextText() {
			return setPrevOrNextText(true); // ��һ
		}
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isFullScreen", false) )
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

		this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar); // ��ActionBar

		super.onCreate(savedInstanceState);
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // ���ó�ʱʱ�� 3����
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // ��Զ����
        myBGcolor = settings.getString("myBGcolor", myBGcolor);
        editor = settings.edit(); // ��ȡ����
		setBGcolor(myBGcolor);

		isProcessLongOneLine = settings.getBoolean("isProcessLongOneLine", isProcessLongOneLine);

		mv = new FoxTextView(this); // �Զ���View
		mv.setBodyBold(settings.getBoolean("isBodyBold", false));

		setContentView(mv);
		this.registerForContextMenu(mv); // �������Ĳ˵�
		mv.setOnTouchListener(new OnTouchListener(){ // �����¼�
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
//				if ( arg1.getAction() == MotionEvent.ACTION_DOWN )
				cX = arg1.getX(); // ��ȡ�������clickʹ��
				cY = arg1.getY(); // getRawX  getRawY
				return false;
			}
		});
		mv.setOnClickListener(new OnClickListener(){  // �����¼�
			@Override
			public void onClick(View arg0) {
//				foxtip("c=" + cX + ":" + cY + "/" + arg0.getWidth() + ":" + arg0.getHeight() + "\n" + arg0.toString());
				int vy = arg0.getHeight();
				if ( cY <= vy / 3 ) { // С��1/3����
					int vx = arg0.getWidth(); // ��Ļ���
					if ( cX >= vx * 0.6666 ) { // ���Ͻ�1/3��
						showMenu();
					} else if (cX <= vx * 0.333) { // ���Ͻ�
						foxExit();
					} else {
						mv.clickPrev();
					}
				} else {
					mv.clickNext();
				}
			}
		});

		tLastPushEinkButton = System.currentTimeMillis();

        isMapUpKey = settings.getBoolean("isMapUpKey", isMapUpKey);

        fontsize = settings.getFloat("fontsize", (float)ToolAndroid.sp2px(this, 18.5f)); // �����С
		mv.setFontSize(fontsize);

		paddingMultip = settings.getFloat("paddingMultip", paddingMultip);
		mv.setPadding(String.valueOf(paddingMultip) + "f");

		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");

		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0);       // ���� �������ݴ�������
		pagename = itt.getStringExtra("chapter_name");
		pageurl = itt.getStringExtra("chapter_url");
		SE_TYPE = itt.getIntExtra("searchengine", 1) ; // ����������������

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				String sText = (String)msg.obj;
				if ( msg.what == IS_REFRESH ) {
					if ( sText.length() < 9 ) {
						mv.setText("����", "�������ޣ����ܴ����ʱ�����������Ŷ\n\nURL: " + pageurl + "\nPageName: " + pagename + "\nContent:" + sText);
					} else {
						pagetext = ProcessLongOneLine(pagetext);
						mv.setText(pagename, "����" + sText.replace("\n", "\n����"));
					}
					mv.postInvalidate();
				}
			}
		};

		final Runnable down_page = new Runnable() {
			@Override
			public void run() {
				String text = "";
				switch(SE_TYPE) {
				case SITES.SE_QIDIAN_MOBILE:
					text = ToolBookJava.downhtml(pageurl, "GBK");
					text = site_qidian.qidian_getTextFromPageJS(text);
					break;
				default:
					text = FoxMemDBHelper.updatepage(-1, pageurl, oDB) ;
				}
				Message msg = Message.obtain();
				msg.what = IS_REFRESH;
				msg.obj = text;
				handler.sendMessage(msg);
			}
		};

		switch (foxfrom) {
		case SITES.FROM_DB: // DB
			pageid =  itt.getIntExtra("chapter_id", 0);
			Map<String,String> infox = oDB.getOneRow("select page.bookid as bid, page.Content as cc, page.Name as naa, book.name as bnn from book,page where page.bookid=book.id and page.id = " + pageid ); // + " and page.Content is not null");
			bookid = Integer.valueOf(infox.get("bid")); // ��ҳʹ��
			pagetext = infox.get("cc") ;
			pagename = infox.get("naa") ;
			bookname = infox.get("bnn") ;

			if ( null == pagetext | pagetext.length() < 5  )
				pagetext = "���½����ݻ�û���أ���ص��б����±�����½�" ;
			allpagescount = oDB.getOneCell("select count(id) from page");
			pagetext = ProcessLongOneLine(pagetext);
			mv.setText(pagename, "����" + pagetext.replace("\n", "\n����"), bookname + "   " + pageid + " / " + allpagescount);
			break;
		case SITES.FROM_NET: // NET
			new Thread(down_page).start();
			break;
		case SITES.FROM_ZIP: // ZIP�ļ�
			pageid =  itt.getIntExtra("chapter_id", 0);
			bookid = Integer.valueOf(oDB.getOneCell("select bookid from page where id = " + pageid )); // ��ҳʹ��
			allpagescount = oDB.getOneCell("select count(id) from page");
			
	    	Matcher mat = Pattern.compile("(?i)^zip://([^@]*?)@([^@]*)$").matcher(pageurl);
	    	String zipRelPath = "";
	    	String zipItemName = "";
	    	while (mat.find()) {
	    		zipRelPath = mat.group(1) ;
	    		zipItemName = mat.group(2) ;
	    	}
	    	ZIPFILE = new File(oDB.getDBFile().getParent() + "/" + zipRelPath);
	    	String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, zipItemName);
			if ( html.contains("\"tpc_content\"") ) {
				HashMap<String, Object> cc = ToolBookJava.getPage1024(html);
				pagetext = cc.get("content").toString();
				pagename = cc.get("title").toString();
			}
			mv.setText(pagename, "����" + pagetext.replace("\n", "\n����"), bookname + "   " + pageid + " / " + allpagescount);
			break;
		default:
			break;
		} // switch ����

	} // oncreate ����


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if ( v == mv ) { // menu.setHeaderTitle("�˵���");
			getMenuInflater().inflate(R.menu.showpage4eink, menu);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.show_prev:
			mv.setPrevText(); // ��һ��
			mv.postInvalidate();
			break;
		case R.id.show_next:
			mv.setNextText() ; // ��һ��
			mv.postInvalidate();
			break;
		case R.id.sp_set_size_up: // ��������
			fontsize += 0.5f ;
			mv.setFontSize(fontsize);
			mv.postInvalidate();
			editor.putFloat("fontsize", fontsize);
			editor.commit();
			foxtip("�����С: " + fontsize);
			break;
		case R.id.sp_set_size_down: // ��С����
			fontsize -= 0.5f ;
			mv.setFontSize(fontsize);
			mv.postInvalidate();
			editor.putFloat("fontsize", fontsize);
			editor.commit();
			foxtip("�����С: " + fontsize);
			break;
		case R.id.paddingup:  // ����ҳ�߾�
			paddingMultip += 0.1f ;
			mv.setPadding(String.valueOf(paddingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("paddingMultip", paddingMultip);
			editor.commit();
			foxtip("ҳ�߾�: " + paddingMultip);
			break;
		case R.id.paddingdown:  // ��Сҳ�߾�
			paddingMultip -= 0.1f ;
			mv.setPadding(String.valueOf(paddingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("paddingMultip", paddingMultip);
			editor.commit();
			foxtip("ҳ�߾�: " + paddingMultip);
			break;
		case R.id.sp_set_linespace_up: // �� �м��
			lineSpaceingMultip += 0.05f ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			foxtip("�м��: " + lineSpaceingMultip);
			break;
		case R.id.sp_set_linespace_down: // �� �м��
			lineSpaceingMultip -= 0.05f ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			mv.postInvalidate();
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			foxtip("�м��: " + lineSpaceingMultip);
			break;
		case R.id.bg_color1:
			setBGcolor("green");
			break;
		case R.id.bg_color2:
			setBGcolor("gray");
			break;
		case R.id.bg_color3:
			setBGcolor("white");
			break;
		case R.id.bg_color9: // ���ñ���
			setBGcolor("default");
			break;
		case R.id.setting:
			startActivity(new Intent(Activity_ShowPage4Eink.this, Activity_Setting.class));
			break;
		case R.id.userfont:
			String nowFontPath = settings.getString("selectfont", "/sdcard/fonts/foxfont.ttf") ;
			if ( ! mv.setUserFontPath(nowFontPath) )
				foxtip("���岻����:\n" + nowFontPath);
			else
				mv.postInvalidate();
			break;
		case R.id.selectFont:
			Intent itt = new Intent(Activity_ShowPage4Eink.this, Activity_FileChooser.class);
			itt.putExtra("dir", "/sdcard/fonts/");
			startActivityForResult(itt, 9);
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 9:  // ��Ӧ�ļ�ѡ������ѡ��
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				// �ж��ļ�����׺
				String newFont = new File(uri.getPath()).getAbsolutePath();
				String nowPATH = newFont.toLowerCase() ;
				if ( nowPATH.endsWith(".ttf") | nowPATH.endsWith(".ttc") | nowPATH.endsWith(".otf") ) {
					editor.putString("selectfont", newFont);
					editor.commit();
					foxtip(newFont);
					mv.setUserFontPath(newFont);
					mv.postInvalidate();
				} else {
					foxtip("Ҫѡ���׺Ϊ.ttf/.ttc/.otf�������ļ�");
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int kc = event.getKeyCode() ;

		// Ī������İ�һ����ť��������keyCode
		if ( ( event.getAction() == KeyEvent.ACTION_UP ) & ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) ) {
			if ( System.currentTimeMillis() - tLastPushEinkButton < 1000 ) { // Ī������Ļ�ఴ��Ҳ������
				tLastPushEinkButton = System.currentTimeMillis();
					return true ;
			}
			// 2016-8-15: BOOX C67ML Carta ���ҷ�ҳ����Ӧ: KEYCODE_PAGE_UP = 92, KEYCODE_PAGE_DOWN = 93
			if ( ! isMapUpKey ) {
				if ( KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc ) {
					mv.clickPrev();
					tLastPushEinkButton = System.currentTimeMillis();
					return true;
				}
			}
			mv.clickNext();
			tLastPushEinkButton = System.currentTimeMillis();
			return true;
		}
		if ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) {
			return true;
		}
		if ( KeyEvent.KEYCODE_MENU == kc & event.getAction() == KeyEvent.ACTION_UP ) {
			this.showMenu();
		}
		return super.dispatchKeyEvent(event);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxExit() {
		this.finish();
	}
	private void showMenu() {
		this.openContextMenu(mv);
	}
	private void setBGcolor(String bgcolor) {
		myBGcolor = bgcolor ;
		editor.putString("myBGcolor", myBGcolor);
		editor.commit();

		if ( myBGcolor.equalsIgnoreCase("white") )
			getWindow().setBackgroundDrawableResource(R.color.qd_mapp_bg_white); // ��ɫ����
		if ( myBGcolor.equalsIgnoreCase("green") )
			getWindow().setBackgroundDrawableResource(R.color.qd_mapp_bg_green); // ��ɫ
		if ( myBGcolor.equalsIgnoreCase("default") )
			getWindow().setBackgroundDrawableResource(R.drawable.parchment_paper); // ��������
		if ( myBGcolor.equalsIgnoreCase("gray") )
			getWindow().setBackgroundDrawableResource(R.color.qd_mapp_bg_grey); // ��ɫ
	}

	private String ProcessLongOneLine(String iText) {
		if ( ! isProcessLongOneLine )
			return iText ;

//		long sTime = System.currentTimeMillis();

		int sLen = iText.length(); // �ַ���
		int lineCount = 0 ; // ����
		try {
			LineNumberReader lnr = new LineNumberReader(new StringReader(iText));
			while ( null != lnr.readLine() )
				++ lineCount;
		} catch ( Exception e ) {
			e.toString();
		}
		if ( ( sLen / lineCount ) > 200 ) { // С��200(Ҳ����Զ���130)��ζ�Ż��з����࣬�����ʱ�򲻻�̫����������ֵ200�͵ô���õ��㹻��Ļ��з�
//			Log.e("XX", "Cal : " + sLen + " / " +  lineCount + " >= 200 ");
			iText = iText.replace("��", "��\n");
			iText = iText.replace("\n\n", "\n");
		}

//		Log.e("TT", "Time=" + ( System.currentTimeMillis() - sTime ) ); 
		return iText;
	}

}

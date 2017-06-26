package com.linpinger.foxbook;

import java.io.File;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.Site1024;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.Activity_FileChooser;
import com.linpinger.tool.FoxZipReader;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolBookJava;

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
	private NovelManager nm;

	private int ittAction = 0 ;  // ��������
	private int bookIDX = -1 ; // ��ҳʱʹ��
	private int pageIDX = -1 ; // ��ҳʱʹ��


	FoxTextView mv;
	private float cX = 0 ; // ���View������
	private float cY = 0 ; // ���View������

	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private String myBGcolor = "default" ;  // ����:Ĭ����Ƥֽ
	private boolean isMapUpKey = false; // �Ƿ�ӳ���Ϸ�Ϊ�·���

	private boolean isShowSettingMenus = true ; // �Ƿ���ʾ ��������ò˵��һ��ֻ���ڵ�һ��ʹ�õ�ʱ����Ҫ����
	private float fontsize = 36.0f; // �����С
	private float paddingMultip  = 0.5f ; // ҳ�߾� = �����С * paddingMultip
	private float lineSpaceingMultip = 1.5f ; // �м�౶��
	private boolean isProcessLongOneLine = true; // ���������� > 4K

	private long tLastPushEinkButton ;

	private final int IS_REFRESH = 5 ;

	private File ZIPFILE ;


	private class FoxTextView extends Ext_View_FoxTextView {
		public FoxTextView(Context context) {
			super(context);
		}

		private int setPrevOrNextText(boolean isNextPage) {
			if ( -1 == pageIDX ) { // ���磬Ӧ�ض���Ϊ-1
				foxtip("�ף�ID Ϊ -1");
				return -1;
			}

			Map<String, Object> mp ;
			String strNoMoreTip ;
			if ( isNextPage ) {
				mp = nm.getNextPage(bookIDX, pageIDX);
				strNoMoreTip = "�ף�û����һҳ��";
			} else {
				mp = nm.getPrevPage(bookIDX, pageIDX);
				strNoMoreTip = "�ף�û����һҳ��";
			}
			if ( null == mp ) {
				foxtip(strNoMoreTip);
				return -2;
			}
			if ( ittAction == AC.aShowPageInZip1024 ) {
				String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, mp.get(NV.PageURL).toString());
				if ( html.contains("\"tpc_content\"") )
					mp.putAll(new Site1024().getContentTitle(html));
			}
			String newBookAddWJX = "��" ;
			if ( (Integer)mp.get(NV.BookIDX) == bookIDX )
				newBookAddWJX = "" ;

			bookIDX = (Integer) mp.get(NV.BookIDX);
			pageIDX = (Integer) mp.get(NV.PageIDX);
			String pagetext = mp.get(NV.Content).toString();
			if ( pagetext.length() == 0 ) {
				foxtip("����δ����: " + mp.get(NV.PageName));
				return -3;
			}
			pagetext = ProcessLongOneLine(pagetext);
			mv.setText(newBookAddWJX + mp.get(NV.PageName).toString()
					, "����" + pagetext.replace("\n", "\n����")
					, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
					+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
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

		this.nm = ((FoxApp)this.getApplication()).nm;

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
		isShowSettingMenus = settings.getBoolean("isShowSettingMenus", isShowSettingMenus);

		fontsize = settings.getFloat("fontsize", (float)ToolAndroid.sp2px(this, 18.5f)); // �����С
		mv.setFontSize(fontsize);

		paddingMultip = settings.getFloat("paddingMultip", paddingMultip);
		mv.setPadding(String.valueOf(paddingMultip) + "f");

		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_REFRESH ) {
					HashMap<String, String> page = (HashMap<String, String>)msg.obj;
					String sText = page.get(NV.Content);
					if ( sText.length() < 9 ) {
						mv.setText("����", "�������ޣ����ܴ����ʱ�����������Ŷ\n\nURL: "
								+ page.get(NV.PageFullURL)
								+ "\nPageName: " + page.get(NV.PageName)
								+ "\nContent:" + sText);
					} else {
						sText = ProcessLongOneLine(sText);
						mv.setText(page.get(NV.PageName).toString(), "����" + sText.replace("\n", "\n����"));
					}
					mv.postInvalidate();
				}
			}
		};

		Intent itt = getIntent();
		ittAction = itt.getIntExtra(AC.action, 0); // ���� ��������
		bookIDX = itt.getIntExtra(NV.BookIDX, -1);
		pageIDX = itt.getIntExtra(NV.PageIDX, -1);
		if ( ittAction == AC.aShowPageInMem ) { // 1024DB3
			if ( nm.getBookInfo(bookIDX).get(NV.BookURL).toString().contains("zip://") )
				ittAction = AC.aShowPageInZip1024 ;
		}
switch (ittAction) {
		case AC.aShowPageInMem:
			Map<String, Object> page = nm.getPage(bookIDX, pageIDX);
			String pagetext = page.get(NV.Content).toString();
			if ( null == pagetext | pagetext.length() < 5  )
				pagetext = "���½����ݻ�û���أ���ص��б����±�����½�" ;
			pagetext = ProcessLongOneLine(pagetext);
			mv.setText(page.get(NV.PageName).toString()
					, "����" + pagetext.replace("\n", "\n����")
					, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
					+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			break;
		case AC.aShowPageOnNet: // NET
			final String pageTitle = itt.getStringExtra(NV.PageName);
			final String fullPageURL = itt.getStringExtra(NV.PageFullURL);
			new Thread( new Runnable() {
				@Override
				public void run() {
					String text = "";
					if ( fullPageURL.contains("druid.if.qidian.com/") )
						text = new SiteQiDian().getContent_Android7(ToolBookJava.downhtml(fullPageURL, "utf-8"));
					else
						text = nm.updatePage(fullPageURL) ;

					HashMap<String, String> page = new HashMap<String, String>(2);
					page.put(NV.PageName, pageTitle);
					page.put(NV.Content, text);
					page.put(NV.PageFullURL, fullPageURL);

					Message msg = Message.obtain();
					msg.what = IS_REFRESH;
					msg.obj = page;
					handler.sendMessage(msg);
				}
			}).start();
			break;
		case AC.aShowPageInZip1024: // ZIP�ļ�
			String zipPageFullURL = itt.getStringExtra(NV.PageFullURL);
			Matcher mat = Pattern.compile("(?i)^zip://([^@]*?)@([^@]*)$").matcher(zipPageFullURL);
			String zipRelPath = "";
			String zipItemName = "";
			while (mat.find()) {
				zipRelPath = mat.group(1) ;
				zipItemName = mat.group(2) ;
			}

			ZIPFILE = new File(nm.getShelfFile().getParent() + "/" + zipRelPath);
			String html = FoxZipReader.getUtf8TextFromZip(ZIPFILE, zipItemName);
			String content = "";
			String pageName = "";
			if ( html.contains("\"tpc_content\"") ) {
				HashMap<String, Object> xx = new Site1024().getContentTitle(html);
				pageName = xx.get(NV.PageName).toString();
				content = xx.get(NV.Content).toString();
			}

			mv.setText(pageName , "����" + content.replace("\n", "\n����")
					, nm.getBookInfo(bookIDX).get(NV.BookName).toString()
					+ "   " + nm.getPagePosAtShelfPages(bookIDX, pageIDX));
			break;
		default:
			break;
} // switch ����

	} // oncreate ����


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if ( v == mv ) { // menu.setHeaderTitle("�˵���");
			getMenuInflater().inflate(R.menu.showpage4eink, menu);
			MenuItem mit ;
			int itemcount = menu.size();
			for ( int i=0; i< itemcount; i++){ // ��ʾ/���ز˵�
				mit = menu.getItem(i);
				switch ( mit.getItemId() ) {
					case R.id.sp_set_size_up:
					case R.id.sp_set_size_down:
					case R.id.paddingup:
					case R.id.paddingdown:
					case R.id.sp_set_linespace_up:
					case R.id.sp_set_linespace_down:
					case R.id.userfont:
					case R.id.selectFont:
					case R.id.setting:
					case R.id.group1:
						mit.setVisible(isShowSettingMenus);
						break;
					case R.id.show_next:
					case R.id.show_prev:
						mit.setVisible( ! isShowSettingMenus );
						break;
					case R.id.ck_isShowSettingMenus:
						mit.setChecked(isShowSettingMenus);
						if ( isShowSettingMenus )
							mit.setTitle("����ʾ���ò˵�");
						else
							mit.setTitle("���������ò˵�");
						break;
					default:
						break;
				}
			}
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ck_isShowSettingMenus:
			isShowSettingMenus = ! item.isChecked(); // ����ѡ���Ƿ�ѡ��ȷ���Ƿ���
			item.setChecked(isShowSettingMenus);
			if ( isShowSettingMenus )
				item.setTitle("����ʾ���ò˵�");
			else
				item.setTitle("���������ò˵�");
			editor.putBoolean("isShowSettingMenus", isShowSettingMenus);
			editor.commit();
			foxtip("�㿪�˵��Բ鿴Ч��");
			break;
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
		if ( sLen == 0 )
			return "";
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

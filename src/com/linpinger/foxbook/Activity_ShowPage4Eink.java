package com.linpinger.foxbook;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
	
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private String myBGcolor = "default" ;  // ����:Ĭ����Ƥֽ
	private boolean isMapUpKey = false; // �Ƿ�ӳ���Ϸ�Ϊ�·���
	private boolean isFullScreen = false; // �´��Ƿ�ȫ��
	private float sp_fontsize = 18; // �����С
	private float lineSpaceingMultip = 1.5f ; // �м�౶��

	private long tLastPushEinkButton ;
	private String lastTitle="";

	private final int IS_REFRESH = 5 ;
	
	private int SE_TYPE = 1; // ��������
	
	
	private class FoxTextView extends View_FoxTextView {

		public FoxTextView(Context context) {
			super(context);
		}

		@Override
		public int setPrevText() {
			if ( 0 == pageid ) {
				foxtip("�ף�ID Ϊ 0");
				return -1;
			}
			Map<String,String> pp ;
			pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id < " + pageid + " and bookid = " + bookid + " and content is not null order by id desc limit 1"); // �����ڵ���һ��
			if ( null == pp.get("id") ) {
				pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid < " + bookid + " and content is not null order by bookid desc, id desc limit 1"); // ��һ��������һ��
				if ( null == pp.get("name") ) {
					foxtip("�ף�û����һҳ��");
					return -2;
				}
			}
			pageid = Integer.valueOf(pp.get("id"));
			bookid = Integer.valueOf(pp.get("bid"));
			lastTitle = pageid + " : " + pp.get("name") + " : " + pp.get("url");
			setTitle(lastTitle);
			pagetext = pp.get("content");
			mv.setText(pp.get("name"), "����" + pagetext.replace("\n", "\n����"), pageid);
			return 0;
		}

		@Override
		public int setNextText() {
			if ( 0 == pageid ) {
				foxtip("�ף�ID Ϊ 0");
				return -1 ;
			}
			Map<String,String> nn;
			nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id > " + pageid + " and bookid = " + bookid + " and content is not null limit 1"); // �����ڵ���һ��
			if ( null == nn.get("id") ) {
				nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid > " + bookid + " and content is not null order by bookid, id limit 1");
				if ( null == nn.get("name") ) {
					foxtip("�ף�û����һҳ��");
					return -2 ;
				}
			}
			
			pageid = Integer.valueOf(nn.get("id"));
			bookid = Integer.valueOf(nn.get("bid"));
			lastTitle = pageid + " : " + nn.get("name") + " : " + nn.get("url");
			setTitle(lastTitle);
			pagetext = nn.get("content");
			mv.setText(nn.get("name"), "����" + pagetext.replace("\n", "\n����"), pageid);
			return 0;
		}
	}
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isFullScreen = settings.getBoolean("isFullScreen", isFullScreen);
		if ( isFullScreen )
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

		this.setTheme(android.R.style.Theme_Holo_Light_NoActionBar); // ��ActionBar

		super.onCreate(savedInstanceState);
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // ���ó�ʱʱ�� 3����
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // ��Զ����
        myBGcolor = settings.getString("myBGcolor", myBGcolor);
        editor = settings.edit(); // ��ȡ����
		setBGcolor(myBGcolor);
		
		mv = new FoxTextView(this); // �Զ���View
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
		sp_fontsize = settings.getFloat("ShowPage_FontSize", sp_fontsize);
		mv.setFontSizeSP(String.valueOf(sp_fontsize) + "f");
		
		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
				
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0);       // ���� �������ݴ�������
		pagename = itt.getStringExtra("chapter_name");
		pageurl = itt.getStringExtra("chapter_url");
		SE_TYPE = itt.getIntExtra("searchengine", 1) ; // ����������������

		setTitle(pagename + " : " + pageurl );

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				String sText = (String)msg.obj;
				if ( msg.what == IS_REFRESH ) {
					setTitle(pagename + " : " + pageurl );
					if ( sText.length() < 9 ) {
						mv.setText("����", "�������ޣ����ܴ����ʱ�����������Ŷ\n\nURL: " + pageurl + "\nPageName: " + pagename + "\nContent:" + sText , 0);
					} else {
						mv.setText(pagename, "����" + sText.replace("\n", "\n����"), 0);
					}
				}
			}
		};

		final Runnable down_page = new Runnable() {
			@Override
			public void run() {
				String text = "";
				switch(SE_TYPE) {
				case SITES.SE_QIDIAN_MOBILE:
					text = FoxBookLib.downhtml(pageurl, "GBK");
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
		
		if ( SITES.FROM_DB == foxfrom ){ // DB
			pageid =  itt.getIntExtra("chapter_id", 0);
			Map<String,String> infox = oDB.getOneRow("select bookid as bid, Content as cc, Name as naa from page where id = " + pageid + " and Content is not null");
			pagetext = infox.get("cc") ;
			pagename = infox.get("naa") ;

			if ( null == pagetext  ) {
				pagetext = "���½����ݻ�û���أ���ص��б����±�����½�" ;
			} else {
				bookid = Integer.valueOf(infox.get("bid")); // ��ҳʹ��
			}
			mv.setText(pagename, "����" + pagetext.replace("\n", "\n����"), pageid);
			
		} 
		if ( SITES.FROM_NET == foxfrom ){ // NET
			setTitle("������...");
			new Thread(down_page).start();
		}
		
	} // oncreate ����


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if ( v == mv ) {
			// menu.setHeaderTitle("�˵���");
			getMenuInflater().inflate(R.menu.showpage4eink, menu);
			int itemcount = menu.size();
			for ( int i=0; i< itemcount; i++){
				switch (menu.getItem(i).getItemId()) {
					case R.id.is_nextfullscreen:
						menu.getItem(i).setChecked(isFullScreen);
						break;
					case R.id.is_mapupkey:
						menu.getItem(i).setChecked(isMapUpKey);
						break;
				}
			}

		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.show_prev:
			mv.setPrevText(); // ��һ��
			mv.setInfoR();
			mv.postInvalidate();
			break;
		case R.id.show_next:
			mv.setNextText() ; // ��һ��
			mv.setInfoR();
			mv.postInvalidate();
			break;
		case R.id.sp_set_size_up: // ��������
			++sp_fontsize;
			mv.setFontSizeSP(String.valueOf(sp_fontsize) + "f");
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			mv.postInvalidate();
			break;
		case R.id.sp_set_size_down: // ��С����
			--sp_fontsize;
			mv.setFontSizeSP(String.valueOf(sp_fontsize) + "f");
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			mv.postInvalidate();
			break;
		case R.id.userfont:
			String nowFontPath = settings.getString("selectfont", "/sdcard/fonts/foxfont.ttf") ;
			if ( ! mv.setUserFontPath(nowFontPath) )
				foxtip("���岻����:\n" + nowFontPath);
			else
				mv.postInvalidate();
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
		case R.id.sp_set_linespace_up:
			lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
			lineSpaceingMultip += 0.1 ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			mv.postInvalidate();
			break;
		case R.id.sp_set_linespace_down:
			lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
			lineSpaceingMultip -= 0.1 ;
			mv.setLineSpaceing(String.valueOf(lineSpaceingMultip) + "f");
			editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
			editor.commit();
			mv.postInvalidate();
			break;
		case R.id.is_nextfullscreen:
			isFullScreen = ! item.isChecked();
			item.setChecked(isFullScreen);
			editor.putBoolean("isFullScreen", isFullScreen);
			editor.commit();
			if (isFullScreen) {
				foxtip("�´���ȫ��ģʽ�������˳�");
			} else {
				foxtip("�´β���ȫ��ģʽ�������˳�");
			}
			foxExit();
			break;
		case R.id.is_mapupkey:
			isMapUpKey = ! item.isChecked();
			item.setChecked(isMapUpKey);
			editor.putBoolean("isMapUpKey", isMapUpKey);
			editor.commit();
			if (isMapUpKey) {
				foxtip("�������Ϸ�������Ϊ�·�");
			} else {
				foxtip("���ڻָ��Ϸ�������");
			}
			break;
		}
		return true;
	}
	


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int kc = event.getKeyCode() ;

// foxtip("key: " + kc + " isMapUpKey: " + isMapUpKey + "\nEvent: " + event.toString());

		// Ī������İ�һ����ť��������keyCode
		if ( ( event.getAction() == KeyEvent.ACTION_UP ) & ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) ) {
			if ( System.currentTimeMillis() - tLastPushEinkButton < 1000 ) { // Ī������Ļ�ఴ��Ҳ������
				tLastPushEinkButton = System.currentTimeMillis();
//				foxtip("��\n\n\n\n\n\nXXXXX");
//				if ( isCloseSmoothScroll )
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
	
}

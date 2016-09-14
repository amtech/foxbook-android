package com.linpinger.foxbook;

import java.io.File;
import java.util.Map;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Activity_ShowPage extends Activity {
	public static FoxMemDB oDB;

	private int foxfrom = 0 ;  // 1=DB, 2=search 
	private TextView tv ;
	private ScrollView sv;
	private LinearLayout rootLayout ;
	private int bookid = 0 ; // ��ҳʱʹ��
	private int pageid = 0 ;
	private String pagetext = "��ȱ" ;
	private String pagename = "" ;
	private String pageurl = "" ;
	private float cX = 0 ; // ���textView������
	private float cY = 0 ; // ���textView������
	
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	private boolean isWhiteActionBar = false; // ��ɫ������
	private String myBGcolor = "default" ;  // ����:Ĭ����Ƥֽ
	private boolean isMapUpKey = false; // �Ƿ�ӳ���Ϸ�Ϊ�·���
	private boolean isFullScreen = false; // �´��Ƿ�ȫ��
	private boolean isCloseSmoothScroll = false; // �ر�ƽ������
	private boolean isShowScrollBar = false; // �Ƿ���ʾ������/�Զ�����
	private boolean bHideActionBar = false ;
	private float sp_fontsize = 18.5f; // �����С
	private float lineSpaceingMultip = 1.3f ; // �м�౶��
	

	private long tLastPushEinkButton ;
	private String lastTitle="";

	private final int IS_REFRESH = 5 ;
	
	private int SE_TYPE = 1; // ��������
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
		getActionBar().setDisplayShowHomeEnabled(false); // ���س���ͼ��
	}		// ��Ӧ����¼���onOptionsItemSelected��switch�м��� android.R.id.home   this.finish();
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void changeLineSpaceing(float mu) {
		float spEx = tv.getLineSpacingExtra();
		float spMu = tv.getLineSpacingMultiplier();
		lineSpaceingMultip = spMu + mu ;
		editor.putFloat("lineSpaceingMultip", lineSpaceingMultip);
		editor.commit();
		tv.setLineSpacing(spEx, lineSpaceingMultip);
		foxtip("��ǰ�м��: " + spEx + "  ����: " + lineSpaceingMultip);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setLineSpaceing() {
		tv.setLineSpacing(tv.getLineSpacingExtra(), lineSpaceingMultip);
	}

	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		isFullScreen = settings.getBoolean("isFullScreen", isFullScreen);
		if ( isFullScreen ) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // ���ó�ʱʱ�� 3����
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // ��Զ����
		setContentView(R.layout.activity_showpage);
		
		showHomeUp();
		
		bHideActionBar = settings.getBoolean("bHideActionBar", bHideActionBar);
		if ( bHideActionBar ) {
			getActionBar().hide();
		}
		
		rootLayout = (LinearLayout) findViewById(R.id.activity_showpage_root_layout);
		tv = (TextView) findViewById(R.id.tv_page);
		sv = (ScrollView) findViewById(R.id.scrollView1);

		isShowScrollBar = settings.getBoolean("isShowScrollBar", isShowScrollBar);
		if ( isShowScrollBar ) {
			sv.setScrollbarFadingEnabled(false); // һֱ��ʾ�����������Զ�����
		}

		tLastPushEinkButton = System.currentTimeMillis();

		// ��ȡ���ã������������С
        
        editor = settings.edit();
        
        isMapUpKey = settings.getBoolean("isMapUpKey", isMapUpKey);
        isCloseSmoothScroll = settings.getBoolean("isCloseSmoothScroll", isCloseSmoothScroll);
        myBGcolor = settings.getString("myBGcolor", myBGcolor);
        setBGcolor(myBGcolor);
		sp_fontsize = settings.getFloat("ShowPage_FontSize", sp_fontsize);
		tv.setTextSize(sp_fontsize);
		
		lineSpaceingMultip = settings.getFloat("lineSpaceingMultip", lineSpaceingMultip);
		setLineSpaceing();
				
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
						tv.setText("�������ޣ����ܴ����ʱ�����������Ŷ\n\nURL: " + pageurl + "\nPageName: " + pagename + "\nContent:" + sText );
					} else {
						if ( bHideActionBar ) {
							tv.setText(pagename + "\n\n����" + sText.replace("\n", "\n����") + "\n" + pagename);
						} else {
							tv.setText("����" + sText.replace("\n", "\n����") + "\n" + pagename);
						}
						
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
			if ( bHideActionBar ) {
				tv.setText(pagename + "\n\n����" + pagetext.replace("\n", "\n����") + "\n" + pagename);
			} else {
				tv.setText("����" + pagetext.replace("\n", "\n����") + "\n" + pagename);
			}
			
		} 
		if ( SITES.FROM_NET == foxfrom ){ // NET
			setTitle("������...");
			new Thread(down_page).start();
		}

		tv.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) { // ��������
				int vy = getWindowManager().getDefaultDisplay().getHeight(); // ��Ļ�߶�
				if ( cY <= vy / 3 ) { // С��1/3����
					int vx = getWindowManager().getDefaultDisplay().getWidth(); // ��Ļ���
					if ( cX >= vx * 0.6666 ) { // ���Ͻ�1/3��
						hideShowActionBar();
					} else {
						clickPrev();
					}
				} else {
					clickNext();
				}
			}
		});
		
		tv.setOnTouchListener(new OnTouchListener(){ // �����¼�
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
//				if ( arg1.getAction() == MotionEvent.ACTION_DOWN )
				cX = arg1.getRawX(); // ��ȡ�������clickʹ��
				cY = arg1.getRawY(); // ��ȡ�������clickʹ��
				return false;
			}
		});
		
	}

	private void hideShowActionBar() {
		bHideActionBar = ! bHideActionBar ;
		if ( bHideActionBar ) {
			getActionBar().hide();
		} else {
			if ( "" == lastTitle ) {
				lastTitle = this.getTitle().toString();
			}
			String aNow = (new java.text.SimpleDateFormat("HH:mm")).format(new java.util.Date()) ;
			this.setTitle(aNow + " " + lastTitle);
			getActionBar().show();
		}
		editor.putBoolean("bHideActionBar", bHideActionBar);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.showpage, menu);
		int itemcount = menu.size();
		for ( int i=0; i< itemcount; i++){
			switch (menu.getItem(i).getItemId()) {
				case R.id.is_nextfullscreen:
					menu.getItem(i).setChecked(isFullScreen);
					break;
				case R.id.is_closesmoothscroll:
					menu.getItem(i).setChecked(isCloseSmoothScroll);
					break;
				case R.id.is_nextshowscrollbar:
					menu.getItem(i).setChecked(isShowScrollBar);
					break;
				case R.id.is_mapupkey:
					menu.getItem(i).setChecked(isMapUpKey);
					break;
			}
		}
		return true;
	}

	private void showPrevChapter() { // ��һ��
		if ( 0 == pageid ) {
			foxtip("�ף�ID Ϊ 0");
			return ;
		}
		Map<String,String> pp ;
		pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id < " + pageid + " and bookid = " + bookid + " and content is not null order by id desc limit 1"); // �����ڵ���һ��
		if ( null == pp.get("id") ) {
			pp = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid < " + bookid + " and content is not null order by bookid desc, id limit 1");
			if ( null == pp.get("name") ) {
				foxtip("�ף�û����һҳ��");
				return ;
			}
		}
		pageid = Integer.valueOf(pp.get("id"));
		bookid = Integer.valueOf(pp.get("bid"));
		lastTitle = pageid + " : " + pp.get("name") + " : " + pp.get("url");
		setTitle(lastTitle);
		pagetext = pp.get("content");
		if ( bHideActionBar ) {
			tv.setText(pp.get("name") + "\n\n����" + pagetext.replace("\n", "\n����") + "\n" + pp.get("name"));
		} else {
			tv.setText("����" + pagetext.replace("\n", "\n����") + "\n" + pp.get("name"));
		}
//		sv.smoothScrollTo(0, 0);
		sv.scrollTo(0, 0);
	}
	
	private void showNextChapter() { // ��һ��
		if ( 0 == pageid ) {
			foxtip("�ף�ID Ϊ 0");
			return ;
		}
		Map<String,String> nn;
		nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where id > " + pageid + " and bookid = " + bookid + " and content is not null limit 1"); // �����ڵ���һ��
		if ( null == nn.get("id") ) {
			nn = oDB.getOneRow("select id as id, bookid as bid, name as name, url as url, content as content from page where bookid > " + bookid + " and content is not null order by bookid, id limit 1");
			if ( null == nn.get("name") ) {
				foxtip("�ף�û����һҳ��");
				return ;
			}
		}
		
		pageid = Integer.valueOf(nn.get("id"));
		bookid = Integer.valueOf(nn.get("bid"));
		lastTitle = pageid + " : " + nn.get("name") + " : " + nn.get("url");
		setTitle(lastTitle);
		pagetext = nn.get("content");
		if ( bHideActionBar ) {
			tv.setText(nn.get("name") + "\n\n����" + pagetext.replace("\n", "\n����") + "\n" + nn.get("name"));
		} else {
			tv.setText("����" + pagetext.replace("\n", "\n����") + "\n" + nn.get("name"));
		}
		
//		sv.smoothScrollTo(0, 0);
		sv.scrollTo(0, 0);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case android.R.id.home: // ����ͼ��
			this.finish();
			break;
		case R.id.show_prev:
			showPrevChapter(); // ��һ��
			break;
		case R.id.show_next:
			showNextChapter() ; // ��һ��
			break;
		case R.id.sp_set_size_up: // ��������
			++sp_fontsize;
			tv.setTextSize(sp_fontsize);
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
			break;
		case R.id.userfont:
			File font1 = new File(settings.getString("selectfont", "/sdcard/fonts/foxfont.ttf"));
			if ( font1.exists() ) {
				tv.setTypeface(Typeface.createFromFile(font1));
			} else {
				foxtip("���岻����:\n" + font1.getAbsolutePath());
				font1.getParentFile().mkdirs();
			}
			break;
		case R.id.hideactionbar:
			hideShowActionBar();
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
			changeLineSpaceing(0.05f);
			break;
		case R.id.sp_set_linespace_down:
			changeLineSpaceing(-0.05f);
			break;
			
		case R.id.sp_set_size_down: // ��С����
			--sp_fontsize;
			tv.setTextSize(sp_fontsize);
			editor.putFloat("ShowPage_FontSize", sp_fontsize);
			editor.commit();
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
			this.finish();
			break;
		case R.id.is_closesmoothscroll:
			isCloseSmoothScroll = ! item.isChecked();
			item.setChecked(isCloseSmoothScroll);
			editor.putBoolean("isCloseSmoothScroll", isCloseSmoothScroll);
			editor.commit();
			if (isCloseSmoothScroll) {
				foxtip("ȡ��ƽ������");
			} else {
				foxtip("Ĭ��ƽ������");
			}			
			break;
		case R.id.is_nextshowscrollbar:
			isShowScrollBar = ! item.isChecked();
			item.setChecked(isShowScrollBar);
			editor.putBoolean("isShowScrollBar", isShowScrollBar);
			editor.commit();
			if (isShowScrollBar) {
				sv.setScrollbarFadingEnabled(false);
				foxtip("������һֱ��ʾ");
			} else {
				sv.setScrollbarFadingEnabled(true);
				foxtip("�������Զ�����");
			}
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
		return super.onOptionsItemSelected(item);
	}
	
	private void setBGcolor(String bgcolor) {
		if ( bgcolor.equalsIgnoreCase("default") ) {
			rootLayout.setBackgroundResource(R.drawable.parchment_paper);
		}
		if ( bgcolor.equalsIgnoreCase("green") ) {
			rootLayout.setBackgroundResource(R.color.qd_mapp_bg_green);
		}
		if ( bgcolor.equalsIgnoreCase("gray") ) {
			rootLayout.setBackgroundResource(R.color.qd_mapp_bg_grey);
		}
		if ( bgcolor.equalsIgnoreCase("white") ) {
			rootLayout.setBackgroundResource(R.color.qd_mapp_bg_white);
		}
		editor.putString("myBGcolor", bgcolor);
		editor.commit();
	}

	private void clickPrev() {
		if (sv.getScrollY() == 0) { // �ڶ���ǰ����
			showPrevChapter(); // ��һ��
		} else {
			if ( isCloseSmoothScroll ) {
				sv.scrollBy(0, 30 - sv.getMeasuredHeight());
			} else {
				sv.smoothScrollBy(0, 30 - sv.getMeasuredHeight());
			}
		}
	}
	private void clickNext() {
		if (sv.getScrollY() == (tv.getHeight() - sv.getHeight())) { // ���ײ���ҳ
			showNextChapter() ; // ��һ��
		} else {
			if ( isCloseSmoothScroll ) {
				sv.scrollBy(0, sv.getMeasuredHeight() - 30);
			} else {
				sv.smoothScrollBy(0, sv.getMeasuredHeight() - 30);
			}
		}
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
				if ( isCloseSmoothScroll )
					return true ;
			}
			// 2016-8-15: BOOX C67ML Carta ���ҷ�ҳ����Ӧ: KEYCODE_PAGE_UP = 92, KEYCODE_PAGE_DOWN = 93
			if ( ! isMapUpKey ) {
				if ( KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc ) {
					clickPrev();
					tLastPushEinkButton = System.currentTimeMillis();
					return true;
				}
			}
			clickNext();
			tLastPushEinkButton = System.currentTimeMillis();
			return true;
		}
		if ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) {
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

/*

	public boolean onKeyDown(int keyCoder, KeyEvent event) {
//		foxtip("����D: " + keyCoder + "\n�¼�: " + event.toString() );
		if ( KeyEvent.KEYCODE_VOLUME_UP == keyCoder ) {
			clickPrev();
			return true;
		}
		if ( KeyEvent.KEYCODE_VOLUME_DOWN == keyCoder ) {
			clickNext();
			return true;
		}
		if ( KeyEvent.KEYCODE_PAGE_DOWN == keyCoder | KeyEvent.KEYCODE_PAGE_UP == keyCoder ) {
			return true;
		}
		return super.onKeyDown(keyCoder, event);
	}
	
	public boolean onKeyUp(int keyCoder, KeyEvent event) {
//		foxtip("����U: " + keyCoder + "\n�¼�: " + event.toString() );
		if ( KeyEvent.KEYCODE_PAGE_DOWN == keyCoder | KeyEvent.KEYCODE_PAGE_UP == keyCoder ) {
			return true;
		}
		if ( KeyEvent.KEYCODE_VOLUME_UP == keyCoder | KeyEvent.KEYCODE_VOLUME_DOWN == keyCoder ) {
			return true;
		}
		return super.onKeyUp(keyCoder, event);
	}
*/

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}

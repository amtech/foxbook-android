package com.linpinger.foxbook;

import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_ShowPage extends Activity {
	private static int FROM_DB = 1 ;
	private static int FROM_NET = 2 ; 

	private int foxfrom = 0 ;  // 1=DB, 2=search 
	private TextView tv ;
	private ScrollView sv;
	private int pageid = 0 ;
	private String pagetext = "��ȱ" ;
	private String pagename = "" ;
	private String pageurl = "" ;
	private float cX, cY ; // ���textView������
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 180000); // ���ó�ʱʱ�� 3����
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // ��Զ����
		setContentView(R.layout.activity_showpage);

		tv = (TextView) findViewById(R.id.tv_page);
		sv = (ScrollView) findViewById(R.id.scrollView1);
		
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0);       // ���� �������ݴ�������
		pagename = itt.getStringExtra("chapter_name");
		pageurl = itt.getStringExtra("chapter_url");

		setTitle(pagename + " : " + pageurl );

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle data = msg.getData();
				pagetext = data.getString("text");
				
				tv.setText("����" + pagetext.replace("\n", "\n����"));
			}
		};

		final Runnable down_page = new Runnable() {
			@Override
			public void run() {
				String text = FoxBookLib.updatepage(-1, pageurl) ;
		
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putString("text", text); 
				msg.setData(data);
				handler.sendMessage(msg);
			}
		};
		
		if ( FROM_DB == foxfrom ){ // DB
			pageid =  itt.getIntExtra("chapter_id", 0);
			pagetext = FoxDB.getOneCell("select Content from page where id = " + pageid + " and Content is not null" );
	 
			tv.setText("����" + pagetext.replace("\n", "\n����"));
		} 
		if ( FROM_NET == foxfrom ){ // NET
			setTitle("������...");
			new Thread(down_page).start();
		}

		tv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) { // ��������
				// TODO Auto-generated method stub
				int vy = getWindowManager().getDefaultDisplay().getHeight(); // ��Ļ�߶�
				if ( cY <= vy / 3 ) { // С��1/3�� ��һҳ
					sv.smoothScrollBy(0, 30 - sv.getMeasuredHeight());
				} else {
					sv.smoothScrollBy(0, sv.getMeasuredHeight() - 30);
				}
			}
		});
		
		tv.setOnTouchListener(new OnTouchListener(){ // �����¼�
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
//				if ( arg1.getAction() == MotionEvent.ACTION_DOWN )
				cY = arg1.getRawY(); // ��ȡ�������clickʹ��
//				cX = arg1.getRawX();
				return false;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �����˵�
		getMenuInflater().inflate(R.menu.showpage, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case R.id.show_prev: // ��һҳ
			if ( 0 == pageid ) {
				foxtip("�ף�ID Ϊ 0");
				break ;
			}
			Map<String,String> pp ;
			pp = FoxDB.getOneRow("select id as id, name as name, url as url, content as content from page where id < " + pageid + " and bookid in (select bookid from page where id = " + pageid + ") and content is not null order by id desc limit 1");
			if ( null == pp.get("id") ) {
				pp = FoxDB.getOneRow("select id as id, name as name, url as url, content as content from page where id < " + pageid + " and content is not null order by bookid, id limit 1");
				if ( null == pp.get("name") ) {
					foxtip("�ף�û����һҳ��");
					break;
				}
			}
			pageid = Integer.valueOf(pp.get("id"));
			setTitle(pageid + " : " + pp.get("name") + " : " + pp.get("url") );
			pagetext = pp.get("content");
			tv.setText("����" + pagetext.replace("\n", "\n����"));
//			sv.smoothScrollTo(0, 0);
			sv.scrollTo(0, 0);
			break;
		case R.id.show_next: // ��һҳ
			if ( 0 == pageid ) {
				foxtip("�ף�ID Ϊ 0");
				break ;
			}
			Map<String,String> nn;
			nn = FoxDB.getOneRow("select id as id, name as name, url as url, content as content from page where id > " + pageid + " and bookid in (select bookid from page where id = " + pageid + ") and content is not null limit 1");
			if ( null == nn.get("id") ) {
				nn = FoxDB.getOneRow("select id as id, name as name, url as url, content as content from page where id > " + pageid + " and content is not null order by bookid, id limit 1");
				if ( null == nn.get("name") ) {
					foxtip("�ף�û����һҳ��");
					break;
				}
			}
			
			pageid = Integer.valueOf(nn.get("id"));
			setTitle(pageid + " : " + nn.get("name") + " : " + nn.get("url") );
			pagetext = nn.get("content");
			tv.setText("����" + pagetext.replace("\n", "\n����"));
			
//			sv.smoothScrollTo(0, 0);
			sv.scrollTo(0, 0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}

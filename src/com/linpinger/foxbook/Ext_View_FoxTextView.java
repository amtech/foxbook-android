package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;

import com.linpinger.tool.ToolAndroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

//��Activity��ʹ�� View.setOnClickListener �󶨵���¼�������Լ����ж�
public class Ext_View_FoxTextView extends View  {

	private FoxBroadcastReceiver bc_rcv;
	private Context ctx ;
	private Paint p;
	
	// ����
	String txt = "ľ�����ݣ����Ǹ����˵���Ϣ";
	String firstPageInfoL = "" ;   // ��һҳʱ���������Ϣ����ʾ����Ϣ
	String infoL = "���������ձ���";
	String infoR = "15:55  0%";
	int batteryLevel = 0;
	float lineSpaceing = 1.5f ;
	float paddingMulti = 0.5f ;
	
	
//	private boolean bDrawSplitLine = false ;  // �����õ�
	private float fontSize = 36.0f ; // E-ink:26 Mobile:34 Mi:26
	private boolean bUseUserFont = false ; // ʹ���û�����
	private String userFontPath = "/sdcard/fonts/foxfont.ttf"; // �û�����·��
	
//	private float clickX = 0 ;
//	private float clickY = 0 ;
	private int nowPageNum = 0 ; // ��һ��Ϊ0
	private boolean isLastPage = false ; //�Ƿ������һҳ
	private boolean isBodyBold = false ; // �����Ƿ�Ӵ�
	
	private ArrayList<String> lines ;
	private int lastTxtHashCode = 0 ;
	private float lastMaxWidth = 0 ;

	public Ext_View_FoxTextView(Context context) {
		super(context);
		ctx = context ;
		
		p = new Paint();
		p.setAntiAlias(true);
		p.setSubpixelText(true); 
		p.setColor(Color.BLACK);  // ��ɫ

		fontSize = (float)ToolAndroid.sp2px(ctx, 18.5f);
		
		bc_rcv = new FoxBroadcastReceiver();
		ctx.registerReceiver(bc_rcv, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); // �����䶯
		ctx.registerReceiver(bc_rcv, new IntentFilter(Intent.ACTION_TIME_CHANGED)); // ʱ��䶯 ACTION_TIME_TICK
	}

	public void setBodyBold(boolean bBold) {
		isBodyBold = bBold ;
	}

	@Override
	protected void onDetachedFromWindow() {
		ctx.unregisterReceiver(bc_rcv);  // �رչ㲥����
		super.onDetachedFromWindow();
	}

	public int setPrevText() { // �豻����
		setText("���±���", "������������\n����ôô��\n");
		return 0;
	}
	public int setNextText() { // �豻����
		setText("���±���", "������������\n����ôô��\n");
		return 0;
	}
	
	public void clickPrev() {
		if ( nowPageNum == 0) { // ��һҳ������һ��
			if ( 0 == setPrevText() ) // ��һ��
				nowPageNum = -6 ; // �ص�����β��
		} else {
			-- this.nowPageNum ;
		}
		setInfoR();
		postInvalidate();
	}
	public void clickNext() {
		if ( isLastPage ) { // ��һ��
			setNextText();
		} else {
			++ this.nowPageNum ;
		}
		setInfoR();
		postInvalidate();
	}
	
	public void setInfoR() {
		infoR = (new java.text.SimpleDateFormat("HH:mm")).format(new java.util.Date()) + "  " + batteryLevel + "%";
	}

	public void setText(String iTitle, String iTxt, String iFirstPageLinfo) {
		firstPageInfoL = iFirstPageLinfo;
		setText(iTitle, iTxt);
	}
	public void setText(String iTitle, String iTxt) {
		infoL = iTitle;
		txt = iTxt ;
		nowPageNum = 0;
		this.setInfoR();
	}

	public void setFontSize(float inFontSizePX) {
		fontSize = inFontSizePX;
	}
	public void setLineSpaceing(String inLS) {// "1.5f"
		lineSpaceing = Float.valueOf(inLS);
	}
	public void setPadding(String floatFontSizeMultiple) { // 0.5f
		paddingMulti = Float.valueOf(floatFontSizeMultiple) ;
	}

	public boolean setUserFontPath(String fontPath) {
		File font1 = new File(fontPath);
		if ( font1.exists() ) {
			bUseUserFont = true ;
			userFontPath = fontPath;
		} else {
			bUseUserFont = false ;
			font1.getParentFile().mkdirs();
		}
		return bUseUserFont;
	}

//�����ı�API: http://www.cnblogs.com/tianzhijiexian/p/4297664.html
//canvas.drawColor(Color.WHITE); // ��ɫ����
//canvas.drawColor(Color.parseColor("#EFF8D6")); // ��ɫ
//Bitmap bg = BitmapFactory.decodeResource(getResources(), R.drawable.parchment_paper);
//canvas.drawBitmap(bg, new Rect(0,0,bg.getWidth(),bg.getHeight()), new Rect(0,0, cw, ch), p);
	@Override
	protected void onDraw(Canvas canvas) {
 		// super.onDraw(canvas);
		// �������
		float lineHeight = fontSize * lineSpaceing ;
		float padding = fontSize * paddingMulti ;
		
		// ��ȡ�����Ĵ�С
		int cw = canvas.getWidth();
		int ch = canvas.getHeight();
//Log.e("OD", "����: W=" + cw + " H=" + ch);
		
		// ��һ����
/*
		if ( bDrawSplitLine ) {
			p.setStyle(Paint.Style.STROKE) ; // ����  FILL
			p.setStrokeWidth(1);
			Rect textRect = new Rect(new Float(padding).intValue(), new Float(padding).intValue(), new Float(cw - padding).intValue(), new Float(ch - padding).intValue());
			canvas.drawRect(textRect, p);
			canvas.drawLine(0, ch/3, cw, 3 + ch / 3, p); // ����
			canvas.drawLine(cw/3*2, 0, 3+cw/3*2, ch, p); // ����
			
			if ( clickX > 0 ) { // ���Ƶ��������
				canvas.drawLine(0, clickY, cw, clickY+6, p); // ����
				canvas.drawLine(clickX, 0, clickX+6, ch, p); // ����
			}
		}
*/			
		// ��������
		if ( bUseUserFont )
			p.setTypeface(Typeface.createFromFile(new File(userFontPath)));  // ��������

		p.setStyle(Paint.Style.FILL) ;
		p.setTextSize(fontSize);

		// ����txt���ݼ�maxwidth�Ƿ���ϴ���ͬ�����в�ͬ����������lines��Ҳ����˵ֻ�ڵ�һ�����ɣ�����ÿ�λ��ƶ�����һ�Σ�����Ǹ���ʱ��
		int nowTxtHashCode = txt.hashCode();
		float nowMaxWidth = cw - 2 * padding ;
		if ( ( lastTxtHashCode != nowTxtHashCode ) || ( lastMaxWidth != nowMaxWidth ) ) {
			lines = split2lines(txt, p, cw - 2 * padding); // �����ݲ����
			lastTxtHashCode = nowTxtHashCode ;
			lastMaxWidth = nowMaxWidth ;
		}

		int lineCount = lines.size();

		// ����ÿ���������
		int linePerScreen = (int) Math.floor( (ch - padding) / lineHeight);
		int nowPageCount = (int) Math.ceil( lineCount / Double.valueOf((String.valueOf(linePerScreen) + ".0")) );  // ����
		if (nowPageNum == -6) // ���Ϸ����ص�β��
			nowPageNum = nowPageCount - 1 ;
		int startLineNum = nowPageNum * linePerScreen ; // 0base:����  
		int endLineNum   =  ( nowPageNum + 1 ) * linePerScreen  - 1 ; //  0base:����
		if ( endLineNum >= lineCount - 1 ) {
			endLineNum = lineCount - 1;
			isLastPage = true  ; // ��ʾҪ���·�ҳ��
		} else {
			isLastPage = false ;
		}

//Log.e("XO", "LPS=" + linePerScreen + " STN=" + startLineNum + " ETN=" + endLineNum + " C=" + lineCount);
		// ���������
		int drawCount = 0;
		for ( int lineidx = startLineNum; lineidx < lineCount; lineidx++) {
//Log.e("XX", "LC=" + lineidx + " DC=" + drawCount + " text=" + line);
			if ( lineidx > endLineNum )
				break;
			++ drawCount;
			if ( lineidx == 0 ) { // ��һ�е�������
				p.setFakeBoldText(true);
				p.setTextSize(lineHeight - padding * 0.5f );
				float titleX = ( cw - p.measureText(infoL)) / 2;
				if ( titleX < 0 )
					titleX = padding ;
				canvas.drawText(infoL, titleX, lineHeight, p);
				p.setTextSize(fontSize);
				if ( ! isBodyBold )
					p.setFakeBoldText(false);
			} else {
				canvas.drawText(lines.get(lineidx), padding, lineHeight * drawCount, p);
			}
		}

		// ���Ƶײ���Ϣ
//		p.setColor(Color.DKGRAY);  // ��ɫ
//		p.setTextSize(fontSize * 0.4f + padding * 0.5f);
//		float infoY = ch - padding / 2 ;
		p.setTextSize( (ch - linePerScreen * lineHeight) / 2 );
		float infoY = ch - (ch - linePerScreen * lineHeight) / 4 ;
		float infoRLen = p.measureText(infoR);
		canvas.drawText(infoR, cw - padding - infoRLen , infoY, p); // �Ҳ�ʱ�� ����
		// �����Ϣ
		if ( 0 == nowPageNum ) { // ��ҳ
			canvas.drawText("1 / " + nowPageCount + "  " + firstPageInfoL, padding, infoY, p);
			// canvas.drawText("���¹� " + nowPageCount + " ҳ    " + firstPageInfoL, padding, ch - padding / 2, p);
		} else { // ����ҳ���ضϹ�������
			String newInfoL = ( nowPageNum + 1 ) + " / " + nowPageCount + "  " + infoL ;
			int infoLen = p.breakText(newInfoL, true, cw - 2 * padding - infoRLen - fontSize, null);
			String addStr = "";
			if ( infoLen < newInfoL.length() )
				addStr = "��" ;
			canvas.drawText(newInfoL.substring(0, infoLen) + addStr, padding, infoY, p);
		}
//		p.setColor(Color.BLACK);  // ��ɫ
	} // onDraw����
	
	private ArrayList<String> split2lines(String inText, Paint p, float maxWidth) {
		ArrayList<String> oLine = new ArrayList<String>(50);
		oLine.add(""); // ��һ�е�������
		String inLine[] = inText.replace("\r", "").split("\n");
		int lineLen = 0;
		int count = 0;
		for ( String line : inLine ) {
			 while(true) {
				lineLen = line.length(); // �г�
				count = p.breakText(line, true, maxWidth, null);
				if ( count >= lineLen ) {
					oLine.add(line);
					break ;
				} else {
					oLine.add(line.substring(0, count));
					line = line.substring(count);
				}
			}
		}

		// ɾ��β������
		int oS = oLine.size();
		String aa ;
		for ( int i=1; i<5; i++) {
			aa = (String)oLine.get(oS-i) ;
			if ( 0 == aa.length() | aa.equalsIgnoreCase("����") ) {
				oLine.remove(oS-i);
			} else {
				break;
			}
		}

		return oLine;
	}


	class FoxBroadcastReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context ctx, Intent itt) {
			if( itt.getAction().equals(Intent.ACTION_BATTERY_CHANGED) ) {
				batteryLevel = itt.getIntExtra("level", 0);
				setInfoR();
				postInvalidate();
			}
			if( itt.getAction().equals(Intent.ACTION_TIME_CHANGED) ) {
				setInfoR();
				postInvalidate();
			}
		}
	}

} // �Զ���View����


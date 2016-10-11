package com.linpinger.foxbook;

import android.app.ListActivity;
import android.view.KeyEvent;
import android.widget.ListView;

public class ListActivity_Eink extends ListActivity {
	
	ListView lv ;
	private int showItemPos = 0 ;
	private long tLastPushEinkButton = System.currentTimeMillis(); ;

	public void setItemPos4Eink() { // ������ķ���������λ�÷ŵ�ͷ��
		showItemPos = 0 ;
	}
	public void setItemPos4Eink(int itemPos) {
		showItemPos = itemPos ;
	}
	
	private void lvPageUP() {
		lv = getListView();
		
		if ( showItemPos > 0 ) {
			if ( lv.getAdapter().getCount() < 1 )
				return;
			int itemsPerScreen = lv.getHeight() / lv.getChildAt(0).getHeight() ;
			showItemPos = showItemPos - itemsPerScreen + 1;
		}
		if ( showItemPos <= 0 )
			showItemPos = 0;
		lv.setSelection(showItemPos);
	}

	private void lvPageDown() {
		lv = getListView();
		int dataSize = lv.getAdapter().getCount();
		if ( showItemPos < dataSize ) {
			if ( dataSize < 1 )
				return;
			int itemsPerScreen = lv.getHeight() / lv.getChildAt(0).getHeight() ;
			showItemPos = showItemPos + itemsPerScreen - 1;
		}
		if ( showItemPos >= dataSize )
			showItemPos = dataSize - 1 ;
		lv.setSelection(showItemPos);
//		setTitle("Pos=" + showItemPos);
//		lv.smoothScrollBy(1000, 600); // ��Ч
	}
	

	public boolean dispatchKeyEvent(KeyEvent event) {
		int kc = event.getKeyCode() ;
		if ( ( event.getAction() == KeyEvent.ACTION_UP ) & ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) ) {
			if ( System.currentTimeMillis() - tLastPushEinkButton < 1000 ) { // Ī������Ļ�ఴ��Ҳ������
				tLastPushEinkButton = System.currentTimeMillis();
					return true ;
			}
			if ( KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc ) {
				lvPageUP();
			} else {
				lvPageDown();
			}
			tLastPushEinkButton = System.currentTimeMillis();
			return true;
		}
		if ( KeyEvent.KEYCODE_PAGE_DOWN == kc | KeyEvent.KEYCODE_PAGE_UP == kc | KeyEvent.KEYCODE_VOLUME_UP == kc | KeyEvent.KEYCODE_VOLUME_DOWN == kc ) {
			return true;
		}

		return super.dispatchKeyEvent(event);
	}
	
}

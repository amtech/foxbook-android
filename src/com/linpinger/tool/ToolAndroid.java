package com.linpinger.tool;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

public class ToolAndroid {

	public static boolean myConfigImportExPort(Context ctx, boolean isExport) { // ���뵼���Ķ�ҳ���ã�����������������е��鷳��
		File cfgFile = new File( Environment.getExternalStorageDirectory().getPath() + "/FoxBook.cfg" );
		SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);

		if ( isExport ) { // ����: DefaultSharedPreferences -> Properties
			StringBuffer oStr = new StringBuffer();
			oStr.append("fontsize=").append(ps.getFloat("fontsize", 36.0f)).append("\n")
				.append("paddingMultip=").append(ps.getFloat("paddingMultip", 0.5f)).append("\n")
				.append("lineSpaceingMultip=").append(ps.getFloat("lineSpaceingMultip", 1.5f)).append("\n")
				.append("myBGcolor=").append(ps.getString("myBGcolor", "green")).append("\n")
				.append("\n");
			ToolJava.renameIfExist(cfgFile);
			ToolJava.writeText(oStr.toString(), cfgFile.getPath() );
			return true ;
		} else { // ����: DefaultSharedPreferences <- Properties
			if ( ! cfgFile.exists() ) {
				System.out.println("����: FoxBook�����ļ������ڣ��޷�����");
				return false;
			}
			Properties pro = new Properties();
			try {
				FileInputStream inputFile = new FileInputStream(cfgFile);
				pro.load(inputFile);
				inputFile.close();
			} catch ( Exception e ) {
				System.err.println(e.toString());
			}
			Editor ed = ps.edit();
			ed.putFloat("fontsize", Float.valueOf(pro.getProperty("fontsize")));
			ed.putFloat("paddingMultip", Float.valueOf(pro.getProperty("paddingMultip")));
			ed.putFloat("lineSpaceingMultip", Float.valueOf(pro.getProperty("lineSpaceingMultip")));
			ed.putString("myBGcolor", pro.getProperty("myBGcolor"));
			return ed.commit();
		}
	}

	// ��ȡwifi��ip,name,������Ϣ
	public static HashMap<String, String> getWifiInfo(Context context) { // ip,name,info | null=��WIFI
		HashMap<String, String> hm = new HashMap<String, String>();
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);  
		NetworkInfo info = cm.getActiveNetworkInfo();
		if ( info != null && info.isAvailable() ) {
			WifiManager wifimanage = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);// ��ȡWifiManager
			if (!wifimanage.isWifiEnabled())
				wifimanage.setWifiEnabled(true);
			WifiInfo wifiinfo = wifimanage.getConnectionInfo();
			hm.put("ip", intToIp(wifiinfo.getIpAddress()) );
			String wifiId = wifiinfo != null ? wifiinfo.getSSID() : null;
			hm.put("name", wifiId );
			hm.put("info", wifiinfo.toString());
			return hm;
		} else {
			return null ; //"��WIFI" ;
		}
	}

//	public static String getLocalIpAddress() {
//		try { // ��������ӿ�
//			Enumeration<NetworkInterface> infos = NetworkInterface.getNetworkInterfaces();
//			while (infos.hasMoreElements()) { // ��ȡ����ӿ�
//				NetworkInterface niFace = infos.nextElement();
//				Enumeration<InetAddress> enumIpAddr = niFace.getInetAddresses();
//				while (enumIpAddr.hasMoreElements()) {
//					InetAddress mInetAddress = enumIpAddr.nextElement();
//					// ����ȡ�������ַ����127.0.0.1ʱ���صõõ���IP
//					if (!mInetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress()))
//						return mInetAddress.getHostAddress().toString();
//				}
//			}
//		} catch (Exception e) {
//			System.err.println(e.toString());
//		}
//		return "127.0.0.1";
//	}

	// ����ȡ��intתΪ������ip��ַ,�ο������ϵģ��޸�����
	private static String intToIp(int i){  
		return ( i & 0xFF) + "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) + "." + ((i >> 24 ) & 0xFF ) ; 
	}

    /** 
     * ��spֵת��Ϊpxֵ����֤���ִ�С���� 
     *  
     * @param spValue 
     * @param fontScale 
     *            ��DisplayMetrics��������scaledDensity�� 
     * @return 
     */  
    public static int sp2px(Context context, float spValue) {  
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (spValue * fontScale + 0.5f);  
    }

	/** 
     * ��pxֵת��Ϊspֵ����֤���ִ�С���� 
     *  
     * @param pxValue 
     * @param fontScale 
     *            ��DisplayMetrics��������scaledDensity�� 
     * @return 
     */  
    public static int px2sp(Context context, float pxValue) {  
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (pxValue / fontScale + 0.5f);  
    }

	public static void download(String iURL, String saveName, Context ctx) {
		download(iURL, saveName, "99_sync", ctx);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void download(String iURL, String saveName, String saveDir, Context ctx) {
		DownloadManager downloadManager = (DownloadManager)ctx.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(iURL));
		request.setDestinationInExternalPublicDir(saveDir, saveName);
		request.setTitle("����: " + saveName);
		request.setDescription(saveName);
		// request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		// request.setMimeType("application/cn.trinea.download.file");
		downloadManager.enqueue(request);
	}

//	public static void setcliptext(String content, Context ctx){ // �����ı���������
//		ClipboardManager cmb = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
//		cmb.setText(content.trim()); 
//	} 
//
//	public static String getcliptext(Context ctx) { // �Ӽ��������ı�
//		ClipboardManager cmb = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
//		return cmb.getText().toString().trim(); 
//	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void setClipText(String iText, Context ctx) {
		((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", iText));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static String getClipText(Context ctx) {
		return ((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString();
	}

}

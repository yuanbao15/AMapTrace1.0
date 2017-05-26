package cn.yuanbao.test0503lbs_amap1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;

/**
 * �����ҵ����ӣ�ע��һ������ʱ���Ͷ�λ��Ϣ�ϴ�
 * ��ʱ�����á�δʹ�ã�
 * @author yuanbao15
 *
 */
public class LocationService extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

    /*private static final String TAG = "LocationService";

    //����AMapLocationClient�����
    AMapLocationClient mLocationClient = null;
    //����AMapLocationClientOption����
    public AMapLocationClientOption mLocationOption = null;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "start LocationService!");
        netThread.start();
        //��ʼ����λ
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //���ö�λ�ص�����
        mLocationClient.setLocationListener(mLocationListener);
        //��ʼ��AMapLocationClientOption����
        mLocationOption = new AMapLocationClientOption();
        //���ö�λģʽΪAMapLocationMode.Hight_Accuracy���߾���ģʽ��
        mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
        //��ȡһ�ζ�λ�����
        //�÷���Ĭ��Ϊfalse��
        mLocationOption.setOnceLocation(true);
        mLocationOption.setOnceLocationLatest(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "StartCommand LocationService!");
        getPosition();
        return super.onStartCommand(intent, flags, startId);

    }

    Handler netHandler = null;

    *//**
     * �շ��������ݵ��߳�
     *//*
    Thread netThread = new Thread(){
        @Override
        public void run() {
            Looper.prepare();
            netHandler = new Handler(){
                public void dispatchMessage(Message msg) {
                    Bundle data = msg.getData();
                    switch(msg.what){
                    case 0x1: //����λ��
                        String macstr = getMac();
                        String longitude = data.getString("longitude");
                        String latitude = data.getString("latitude");
                        String timestr = data.getString("timestr");
                        upDatePosition(macstr,longitude+","+latitude,timestr,timestr);
                        break;

                    }
                };
            };
            Looper.loop();
        }
    };

    public void getPosition(){
        //����λ�ͻ��˶������ö�λ����
        mLocationClient.setLocationOption(mLocationOption);
        //������λ
        mLocationClient.startLocation();
    }
    //������λ�ص�������
    public AMapLocationListener mLocationListener = new AMapLocationListener(){

        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
                    if(amapLocation==null){
                        Log.i(TAG, "amapLocation is null!");
                        return;
                    }
                    if(amapLocation.getErrorCode()!=0){
                        Log.i(TAG, "amapLocation has exception errorCode:"+amapLocation.getErrorCode());
                        return;
                    }

                    Double longitude = amapLocation.getLongitude();//��ȡ����
                    Double latitude = amapLocation.getLatitude();//��ȡγ��
                    String longitudestr = String.valueOf(longitude);
                    String latitudestr = String.valueOf(latitude);
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(amapLocation.getTime());
                    String timestr = df.format(date);
                    Log.i(TAG, "longitude,latitude:"+longitude+","+latitude);
                    Log.i(TAG, "time:"+timestr);
                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("longitude", longitudestr);
                    data.putString("latitude", latitudestr);
                    data.putString("timestr", timestr);
                    msg.setData(data);
                    msg.what = 0x1;
                    netHandler.sendMessage(msg);
        }

    };



    private String getMac() {
        String macSerial = null;
        String str = "";

        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str;) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// ȥ�ո�
                    break;
                }
            }
        } catch (IOException ex) {
            // ����Ĭ��ֵ
            ex.printStackTrace();
        }
        return macSerial;
    }



    public void upDatePosition(String mac, String position, String recordTime, String reportTime) {
        Log.i(TAG, mac+";"+position+";"+recordTime);
        // �����ռ�
        String nameSpace = "http://nfswit.cc/";

        // ���õķ�������
        String methodName = "UpdateDevicePosition";

        // EndPoint
        String endPoint = "http://182.247.238.98:82/WebService1.asmx";

        // SOAP Action
        String soapAction = "http://nfswit.cc/UpdateDevicePosition";

        // ָ��WebService�������ռ�͵��õķ�����
        SoapObject rpc = new SoapObject(nameSpace, methodName);

        // ָ������
        rpc.addProperty("strDeviceMAC", mac);
        rpc.addProperty("strDevicePosition", position);
        rpc.addProperty("strDeviceRecordTime", recordTime);
        rpc.addProperty("strDeviceReportTime", reportTime);

        // ���ɵ���WebService������SOAP������Ϣ,��ָ��SOAP�İ汾
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.implicitTypes = true;
        envelope.bodyOut = rpc;
        // �����Ƿ���õ���dotNet������WebService
        envelope.dotNet = true;
        // �ȼ���envelope.bodyOut = rpc;
        envelope.setOutputSoapObject(rpc);

        HttpTransportSE transport = new HttpTransportSE(endPoint);
        try {
            // ����WebService
            transport.call(soapAction, envelope);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ��ȡ���ص�����
        Object object = envelope.bodyIn;
        // SoapFault object = (SoapFault) envelope.bodyIn;

        if (object == null) {
            Log.i(TAG, "return object is null!");
            return;
        }
        if (object instanceof SoapFault) {
            Log.i(TAG, "SoapFault refult is :" + object.toString());
            return;
        } else if (object instanceof SoapObject) {
            // ��ȡ���صĽ��
            Log.i(TAG, "SoapObject refult is :" + object.toString());

            try {
                SoapObject result = (SoapObject) object;
                PropertyInfo info = new PropertyInfo();
                result.getPropertyInfo(0, info);
                String str = result.getProperty(0).toString();
                Log.i(TAG, str);
                JSONObject jsonobj = (JSONObject) JSONValue.parse(str);
                String code = (String) jsonobj.get("code");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }   */

}
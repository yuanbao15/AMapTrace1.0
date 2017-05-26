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
 * 网上找的例子，注册一个服务定时发送定位信息上传
 * 暂时不可用。未使用！
 * @author yuanbao15
 *
 */
public class LocationService extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

    /*private static final String TAG = "LocationService";

    //声明AMapLocationClient类对象
    AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "start LocationService!");
        netThread.start();
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
        //获取一次定位结果：
        //该方法默认为false。
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
     * 收发网络数据的线程
     *//*
    Thread netThread = new Thread(){
        @Override
        public void run() {
            Looper.prepare();
            netHandler = new Handler(){
                public void dispatchMessage(Message msg) {
                    Bundle data = msg.getData();
                    switch(msg.what){
                    case 0x1: //发送位置
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
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }
    //声明定位回调监听器
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

                    Double longitude = amapLocation.getLongitude();//获取经度
                    Double latitude = amapLocation.getLatitude();//获取纬度
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
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }
        return macSerial;
    }



    public void upDatePosition(String mac, String position, String recordTime, String reportTime) {
        Log.i(TAG, mac+";"+position+";"+recordTime);
        // 命名空间
        String nameSpace = "http://nfswit.cc/";

        // 调用的方法名称
        String methodName = "UpdateDevicePosition";

        // EndPoint
        String endPoint = "http://182.247.238.98:82/WebService1.asmx";

        // SOAP Action
        String soapAction = "http://nfswit.cc/UpdateDevicePosition";

        // 指定WebService的命名空间和调用的方法名
        SoapObject rpc = new SoapObject(nameSpace, methodName);

        // 指定参数
        rpc.addProperty("strDeviceMAC", mac);
        rpc.addProperty("strDevicePosition", position);
        rpc.addProperty("strDeviceRecordTime", recordTime);
        rpc.addProperty("strDeviceReportTime", reportTime);

        // 生成调用WebService方法的SOAP请求信息,并指定SOAP的版本
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.implicitTypes = true;
        envelope.bodyOut = rpc;
        // 设置是否调用的是dotNet开发的WebService
        envelope.dotNet = true;
        // 等价于envelope.bodyOut = rpc;
        envelope.setOutputSoapObject(rpc);

        HttpTransportSE transport = new HttpTransportSE(endPoint);
        try {
            // 调用WebService
            transport.call(soapAction, envelope);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 获取返回的数据
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
            // 获取返回的结果
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
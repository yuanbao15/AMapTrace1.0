package cn.yuanbao.test0503lbs_amap1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;

/*
 * �α�LBS��ϰ������ʹ�õ��ǰٶ�sdk ���Լ��õĸߵ�AMap sdk
 * ��MapViewʵ�ֵ�ͼ��ʾ��ʵ�ֶ�λ����ת����λҳ�棬��ȡ��λ������Ϣ��
 * 2017-5-6 ���ӹ켣���ߵĹ��ܣ���δʵ�ַ������洢
 * 2017-5-8 ʵ�����߳�ˢ�¶�λ�������켣����·�ߡ�
 * 2017-5-9 ���켣���Ƴ��Զ���ͼ������(��ͷ)���ü��ϴ�ŵ����ꡣʵ�ָ߾���ģʽ��bug��С�����������켣���Ǹ��������ֶ�λ��ʽ
 */

public class MainActivity extends Activity {

	MapView mapView = null;	//��ʾ��ͼ
	TextView position;	//��λ���꾭γ��
	double lat;	//γ��
	double lon;	//����
	private StringBuilder currentPosition;	//�����洢ʵʱλ������
	private AMapLocationClient aMapLocationClient;	//��λ�ͻ���
	private AMapLocationClientOption aMapLocationClientOption;	//��λ���õ�
	private AMap aMap = null;
	private UiSettings mUiSettings;
	private MyLocationStyle myLocationStyle;
	
	private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);//��λС�������ɫ
	private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
	
	private Button btn_locateThread;
	private Thread locateThread;
	
    private LatLng oldLatLng;  //��ǰ�Ķ�λ��  
//    private LatLng newLatLng;  //�µĶ�λ��  	//����Ū�ɳ�Ա��������Ҫÿ������new������ӵ�pointList��
    private boolean isFirstLatLng = true; //�Ƿ��ǵ�һ�ζ�λ  
    boolean threadable = false;  //����ȫ�ֱ���threadable�����ڿ����߳̿�����  
	int i=0;
	List<LatLng> pointList;
	private Polyline mPolyline;
    
    //ͨ�����̷߳�ʽ��ͣ��ȡ��λ��handler������Ϣ�����߳�.���ڴ˽���ui����
    Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case 1:
				//С����λ�ø���
				setupLocationStyle();
				aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE));
				
				Bundle b = msg.getData();
	    		lat = b.getDouble("lat");
	    		lon = b.getDouble("lon");
				LatLng newLatLng = new LatLng(lat,lon); 
				if(isFirstLatLng){  	//��ִ�е�һ�Σ����治��ִ�е���
		            //��¼��һ�εĶ�λ��Ϣ  
					pointList.add(newLatLng);
		            oldLatLng = newLatLng;  
		            isFirstLatLng = false;  
		        }  
		        //λ���б仯  
		        if(!oldLatLng.equals(newLatLng)){  //֮ǰ�� oldLatLng ��= newLatLng; �������ж�.//ǧ��ע���"!"
		        	pointList.add(newLatLng);
		        	if(mPolyline != null){
		        		mPolyline.remove();		//�Ȳ������ٸ�������
		        	}
	        		addPolylineInPlayGround(pointList);
		        	
//		            drawLineOnMap(oldLatLng, newLatLng);
		     		//����һ�����þ�γ�ȵ�CameraUpdate
		            CameraUpdate cu = CameraUpdateFactory.changeLatLng(newLatLng);
		            //���µ�ͼ����ʾ����
		            aMap.moveCamera(cu);
		            oldLatLng = newLatLng;  
		        }
		        Log.e("location", i+"_yuanbao��λTest "+pointList.size());
// 				Toast.makeText(MainActivity.this, i+"_yuanbao��λTest "+pointList.size()+" ʱ�䣺"+System.currentTimeMillis(), 0).show();
		        i++;
				break;
			default:
				break;
			}
    	}
    };
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// ����ʾ����ı�����
        setContentView(R.layout.activity_main);
        
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        position = (TextView) findViewById(R.id.tv_position);
        btn_locateThread = (Button) findViewById(R.id.btn_locate_thread);
        
        pointList = new ArrayList<LatLng>();
        initAMap();
        initPermission();	//��ʼ��Ȩ�޴���
        initLocation();	//�Զ����ʼ����ͼ��λ�� ��������
        
       /* //λ�ñ仯���Զ����¶�λ
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300, 8, new LocationListener() {
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				
			}
			
			@Override
			public void onProviderEnabled(String provider) {
				// ʹ��GPRS�ṩ�Ķ�λ��Ϣ������λ��
                updatePosition(locationManager.getLastKnownLocation(provider));
			}
			
			@Override
			public void onProviderDisabled(String provider) {
				
			}
			
			@Override
			public void onLocationChanged(Location location) {
				updatePosition(location);
			}
		});*/
    }
    
    /**
     * ��⵽λ�÷����仯��ķ���
     * @param location
     */
	protected void updatePosition(Location location) {
		LatLng newLatLng = new LatLng(location.getLatitude(), location.getLongitude()); 
		if(isFirstLatLng){  	//��ִ�е�һ�Σ����治��ִ�е���
            //��¼��һ�εĶ�λ��Ϣ  
            oldLatLng = newLatLng;  
            isFirstLatLng = false;  
        }  
        //λ���б仯  
        if(oldLatLng != newLatLng){  
            drawLineOnMap(oldLatLng, newLatLng);	//����
            //����һ�����þ�γ�ȵ�CameraUpdate
            CameraUpdate cu = CameraUpdateFactory.changeLatLng(newLatLng);
            //���µ�ͼ����ʾ����
            aMap.moveCamera(cu);
/*    		//������е�Marker�ȸ�����
            aMap.clear();*/
            //����һ��MarkerOptions����
            MarkerOptions markOptions = new MarkerOptions();
            markOptions.position(newLatLng);
            //���MarkerOptions��ʵ���������Marker��
            Marker marker = aMap.addMarker(markOptions);
            oldLatLng = newLatLng;  
        }
	}

	private void initAMap() {
		if(aMap == null){
        	aMap = mapView.getMap();
        	
        	mUiSettings = aMap.getUiSettings();		//���һ����ͼui���ö���
        	mUiSettings.setMyLocationButtonEnabled(true);// ����Ĭ�϶�λ��ť�Ƿ���ʾ
        	mUiSettings.setScaleControlsEnabled(true);	//���ñ������Ƿ���ʾ
        	
    		aMap.setMyLocationEnabled(true);// ����Ϊtrue��ʾ��ʾ��λ�㲢�ɴ�����λ��false��ʾ���ض�λ�㲢���ɴ�����λ��Ĭ����false
//    		setupLocationStyle();	//��ʼ����λС����
        }
		
		if(aMapLocationClient == null){
			aMapLocationClient = new AMapLocationClient(getApplicationContext());//��ʼ��client
		}
		
	}

	private void initPermission() {
		List<String> permissionList = new ArrayList<String>();	//Ȩ�޼��ϣ�ͳһ����
		
		if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) 
				!= PackageManager.PERMISSION_GRANTED){
			permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
		}
		if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) 
				!= PackageManager.PERMISSION_GRANTED){
			permissionList.add(Manifest.permission.READ_PHONE_STATE);
		}
		if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
				!= PackageManager.PERMISSION_GRANTED){
			permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
		
		if(!permissionList.isEmpty()){
			String[] permissions = permissionList.toArray(new String[permissionList.size()]);
			ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
		}else{
			//������λ  
	        aMapLocationClient.startLocation();  
		}
	}

	private void initLocation() {
		
		//��ʼ����λ����  
        aMapLocationClientOption = new AMapLocationClientOption();  
        //���ö�λģʽΪ�߾���ģʽ��Battery_SavingΪ�͹���ģʽ��Device_Sensors�ǽ��豸ģʽ  
        aMapLocationClientOption.setLocationMode(AMapLocationMode.Hight_Accuracy);  
        //�����Ƿ񷵻ص�ַ��Ϣ��Ĭ�Ϸ��ص�ַ��Ϣ��  
        aMapLocationClientOption.setNeedAddress(true);  
        //�����Ƿ�ֻ��λһ��,Ĭ��Ϊfalse  
        aMapLocationClientOption.setOnceLocation(false);  
        //�����Ƿ�ǿ��ˢ��WIFI��Ĭ��Ϊǿ��ˢ��  
        aMapLocationClientOption.setWifiScan(true);  //ԭ��ΪsetWifiActiveScan()�ѹ�ʱ
        //�����Ƿ�����ģ��λ��,Ĭ��Ϊfalse��������ģ��λ��  
        aMapLocationClientOption.setMockEnable(true);  //���˺�genymotionģ�����򿪲Ų��ҵ���
        //���ö�λ���,��λ����,Ĭ��Ϊ2000ms  
        aMapLocationClientOption.setInterval(1000);
        aMapLocationClientOption.setLocationCacheEnable(true); //��ѡ�������Ƿ�ʹ�û��涨λ��Ĭ��Ϊtrue
        //����λ�ͻ��˶������ö�λ����  
        aMapLocationClient.setLocationOption(aMapLocationClientOption);  
        aMapLocationClient.setLocationListener(aMapLocationListener);	//aMapLocationListener ���ö�λ�ص�����
        //������λ  
        aMapLocationClient.startLocation();  
        
     // �Զ���ϵͳ��λ����
		myLocationStyle = new MyLocationStyle();
	}
	
	//����ʱȨ�޴���
	@Override
	public void onRequestPermissionsResult(int requestCode,
			String[] permissions, int[] grantResults) {
		switch (requestCode) {
		case 1:
			if(grantResults.length > 0){
				for (int result : grantResults) {
					if(result != PackageManager.PERMISSION_GRANTED){
						Toast.makeText(this, "��ͬ������Ȩ�޲���ʹ�ñ�app", 0).show();
//						finish();	//��֪����ô����û�е�������ʱȨ������ĶԻ���
						return;
					}
				}
				//������λ  
		        aMapLocationClient.startLocation();  
			}else{
				Toast.makeText(this, "����δ֪������ж�غ���װ", 0).show();
//				finish();
			}
			break;
		default:
			break;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
    
	/*//�Զ���һ����λ��������
	public class MyLocationListener implements AMapLocationListener{

		@Override
		public void onLocationChanged(AMapLocation aMapLocation) {
			if(aMapLocation != null){
				if (aMapLocation.getErrorCode() == 0) {  
                  
					//�Լ����õ�����
                    lat = aMapLocation.getLatitude();  
                    lon = aMapLocation.getLongitude();  
                    Log.v("pcw","lat : "+lat+" lon : "+lon); 
                    
                    currentPosition = new StringBuilder();
                    currentPosition.append("γ�ȣ�").append(lat).append("\n");
            		currentPosition.append("���ȣ�").append(lon).append("\n");
            		
            		//��һЩ������Ϣ
            		currentPosition.append("���ң�").append(aMapLocation.getCountry()).append("\n");
            		currentPosition.append("ʡ��").append(aMapLocation.getProvince()).append("\n");
            		currentPosition.append("�У�").append(aMapLocation.getCity()).append("\n");
            		currentPosition.append("����").append(aMapLocation.getDistrict()).append("\n");
            		currentPosition.append("�ֵ���").append(aMapLocation.getStreet()).append("\n");
            		currentPosition.append("��ϸ��ַ��").append(aMapLocation.getAddress()).append("\n");
            		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
                    Date date = new Date(aMapLocation.getTime());  
                    currentPosition.append("��λʱ�䣺").append(df.format(date)).append("\n");
                    df.format(date);	//��λʱ�� 
            		
            		
            		currentPosition.append("��λ��ʽ��");
            		if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_GPS){
            			currentPosition.append("GPS");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_WIFI){
            			currentPosition.append("WIFI");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_CELL){
            			currentPosition.append("gprs����");
            		}else {
            			currentPosition.append("NULL");
            		}
            		
            		position.setText(currentPosition.toString());	//��ʾ����
            		
            		//��λ�仯�켣����
            		updatePosition(aMapLocation);
            		
            		
                } else {  
                    //��ʾ������ϢErrCode�Ǵ����룬errInfo�Ǵ�����Ϣ������������  
                    Log.e("AmapError", "location Error, ErrCode:"  
                            + aMapLocation.getErrorCode() + ", errInfo:"  
                            + aMapLocation.getErrorInfo());  
                }
			}
		}
	}*/
	
	/**��ʽ2
	 * ��λ�������󣬵��Ǹо�û����Ч���Զ�λ�仯û��������Ӧ�¼�
	 */
    AMapLocationListener aMapLocationListener = new AMapLocationListener() {
		int i=0;
		@Override
		public void onLocationChanged(AMapLocation aMapLocation) {
			if(aMapLocation != null){
				currentPosition = new StringBuilder();
				
				if (aMapLocation.getErrorCode() == 0) {  
                    //��λ�ɹ��ص���Ϣ�����������Ϣ  
                    lat = aMapLocation.getLatitude();  
                    lon = aMapLocation.getLongitude();  
                    Log.v("pcw","lat : "+lat+" lon : "+lon); 
                    
                    currentPosition.append("γ�ȣ�").append(lat).append("\n");
            		currentPosition.append("���ȣ�").append(lon).append("\n");
                    //��һЩ������Ϣ
            		currentPosition.append("���ң�").append(aMapLocation.getCountry()).append("\n");
            		currentPosition.append("ʡ��").append(aMapLocation.getProvince()).append("\n");
            		currentPosition.append("�У�").append(aMapLocation.getCity()).append("\n");
            		currentPosition.append("����").append(aMapLocation.getDistrict()).append("\n");
            		currentPosition.append("�ֵ���").append(aMapLocation.getStreet()).append("\n");
            		currentPosition.append("��ϸ��ַ��").append(aMapLocation.getAddress()).append("\n");
            		
            		SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
                    Date date1 = new Date(aMapLocation.getTime());  
                    currentPosition.append("��λʱ�䣺").append(df1.format(date1)).append("\n");
                    df1.format(date1);//��λʱ�䰴��ʽ���
            		
            		if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_GPS){
            			currentPosition.append("GPS");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_WIFI){
            			currentPosition.append("WIFI");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_CELL){
            			currentPosition.append("gprs����");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_OFFLINE){
            			currentPosition.append("����");
            		}
            		position.setText(currentPosition.toString());	//��ʾ����
            		
            		updatePosition(aMapLocation);
            		Toast.makeText(MainActivity.this, i+"��λ������", 0).show();
            		i++;
            		
                } else {  
                    //��ʾ������ϢErrCode�Ǵ����룬errInfo�Ǵ�����Ϣ������������  
                    Log.e("AmapError", "location Error, ErrCode:"  
                            + aMapLocation.getErrorCode() + ", errInfo:"  
                            + aMapLocation.getErrorInfo());  
                }
			}
		}
	};
	
	/**��ť1�������ʾ����λ�á�
	 * ���������λ������ҳ���ƶ������¶�λ�������켣���ߡ�
	 * @param v
	 */
	public void locate1(View v){
		setupLocationStyle();
		aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE));
      
		//������λ  
		aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.startLocation();  
        
        if(aMapLocationClient.getLastKnownLocation() != null){	//֮ǰ���ж�ʱģ�������������ָ��ҵ�
        	
        	LatLng newLatLng = new LatLng(aMapLocationClient.getLastKnownLocation().getLatitude(), 
        			aMapLocationClient.getLastKnownLocation().getLongitude()); 
        	if(isFirstLatLng){  	//��ִ�е�һ�Σ����治��ִ�е���
        		//��¼��һ�εĶ�λ��Ϣ  
        		oldLatLng = newLatLng;  
        		isFirstLatLng = false;  
        	}  
        	//λ���б仯  
        	if(!oldLatLng.equals(newLatLng)){  
//        		drawLineOnMap(oldLatLng, newLatLng);
        		//����һ�����þ�γ�ȵ�CameraUpdate
        		CameraUpdate cu = CameraUpdateFactory.changeLatLng(newLatLng);
        		//���µ�ͼ����ʾ����
        		aMap.moveCamera(cu);
        		oldLatLng = newLatLng;  
        	}
        	
        }else{
        	Toast.makeText(MainActivity.this, "yb���Եȣ���һ�ζ�λ��δ���", 0).show();
        }
		
	}
	
	public void locate2(View v){
		setupLocationStyle();
        aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE));

	}
	//��ת���¸�ҳ����ϸ��ʾ��λ������Ϣ
	public void locate3(View v){
		Intent intent = new Intent(MainActivity.this, LocaInfoActivity.class);
		intent.putExtra("locaInfo", currentPosition.toString());
		startActivity(intent);
	}
	
	//test1��ť��marker����
	public void test1(View v){
		aMap.setMyLocationEnabled(true);
		
		//ʹ��Marker��ǵ㣬������������
		MarkerOptions markerOption = new MarkerOptions();
	    markerOption.position(Constants.WUHAN)
	    	.title("�人��")	//����
	    	.snippet("ÿ�첻һ��,wuhan,new city��")	//����
	    	.draggable(true)	//����Marker���϶�
	    	.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
	    			.decodeResource(getResources(),R.drawable.poi_marker_1)));
	    // ��Marker����Ϊ������ʾ������˫ָ������ͼ�鿴Ч��
	    markerOption.setFlat(true);//����markerƽ����ͼЧ��
	    
	    Marker marker = aMap.addMarker(markerOption);//��Ҫ©��
//	    marker.showInfoWindow();	//��ʾ��Ϣ��������� marker.hideInfoWindow();
	    
	    
/*	    //���Ӷ���Ч��
	    Animation animation = new RotateAnimation(marker.getRotateAngle(),marker.getRotateAngle()+180,0,0,0);
	    animation.setDuration(1000);
	    animation.setInterpolator(new LinearInterpolator());
	    marker.setAnimation(animation);	
	    marker.startAnimation();*/
	    
	    //��Ϣ���ڵ����Ӧ
        OnInfoWindowClickListener listener = new OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Toast.makeText(MainActivity.this, "yuanbao,�����infowindow", 0).show();
            }
        };
        //����Ϣ������¼�
        aMap.setOnInfoWindowClickListener(listener);
		
	}
	
	//test2��ť�����߲���
	public void test2(View v){
		Polyline polyline = aMap.addPolyline((new PolylineOptions())
				.add(Constants.SHANGHAI, Constants.BEIJING, Constants.WUHAN)
				.width(7)
				.setDottedLine(true)//��������
				.color(Color.argb(255, 1, 1, 1)));
		
//		drawLineOnMap(new LatLng(30.55, 114.30), new LatLng(32.2, 115.3));	//������
		
/*  	List<LatLng> latLngs = new ArrayList<LatLng>();
        latLngs.add(new LatLng(39.999391,116.135972));
        latLngs.add(new LatLng(39.898323,116.057694));
        latLngs.add(new LatLng(39.900430,116.265061));
        latLngs.add(new LatLng(39.955192,116.140092));
        Polyline polyline = AMap.addPolyline(new PolylineOptions()
	        .addAll(latLngs)
	        .width(10)
	        .color(Color.argb(255, 1, 1, 1)));*/
	}
	
	//test3��ť���������̲߳��ԣ���λ��
	public void test3(View v){
		locateThread = new Thread(new MyThread());
		
		if(!threadable){
			btn_locateThread.setText("ֹͣ��λ�߳�");
			aMap.setMyLocationEnabled(true);
			threadable = true;
		}else{
			btn_locateThread.setText("������λ�߳�");
			threadable = false;
		}
		locateThread.start();
	}
	
	/**
	 * �����Զ��嶨λ����
	 */
	private void setupLocationStyle(){
		
		myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//������λ���ҽ��ӽ��ƶ�����ͼ���ĵ㣬��λ�������豸������ת�����һ�����豸�ƶ�����1��1�ζ�λ�����������myLocationType��Ĭ��Ҳ��ִ�д���ģʽ��
		myLocationStyle.interval(2000); //����������λģʽ�µĶ�λ�����ֻ��������λģʽ����Ч�����ζ�λģʽ�²�����Ч����λΪ���롣
		// �Զ��嶨λ����ͼ��
		myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
				fromResource(R.drawable.gps_point_yb2));
		// �Զ��徫�ȷ�Χ��Բ�α߿���ɫ
		myLocationStyle.strokeColor(STROKE_COLOR);
		//�Զ��徫�ȷ�Χ��Բ�α߿���
		myLocationStyle.strokeWidth(5);
		// ����Բ�ε������ɫ
		myLocationStyle.radiusFillColor(FILL_COLOR);
		/*//��λ����ͼ���ê�㷽��
		myLocationStyle.anchor(0, 0);*/
		
		// ���Զ���� myLocationStyle ������ӵ���ͼ��
		aMap.setMyLocationStyle(myLocationStyle);
		//aMap.getUiSettings().setMyLocationButtonEnabled(true);����Ĭ�϶�λ��ť�Ƿ���ʾ���Ǳ������á�
        aMap.setMyLocationEnabled(true);// ����Ϊtrue��ʾ������ʾ��λ���㣬false��ʾ���ض�λ���㲢�����ж�λ��Ĭ����false��	

	}
	
	/**
	 * �������������֮����߶�,����ǰλ�õ�����λ��
	 */  
    private void drawLineOnMap(LatLng oldLatLng, LatLng newLatLng ) {  
        // ����һ���������  
        aMap.addPolyline((new PolylineOptions())
        		.add(oldLatLng, newLatLng)  
                .geodesic(true)
                .color(Color.GREEN));  
    }
    /**
     * �ο�demo
     * yb��ӹ켣��	
     */
    private void addPolylineInPlayGround(List<LatLng> latlngList) {
        List<Integer> colorList = new ArrayList<Integer>();
        List<BitmapDescriptor> bitmapDescriptors = new ArrayList<BitmapDescriptor>();
        //������ɫyb
        int[] colors = new int[]{Color.argb(255, 0, 255, 0),Color.argb(255, 255, 255, 0),Color.argb(255, 255, 0, 0)};

        //��һ���������������
        List<BitmapDescriptor> textureList = new ArrayList<BitmapDescriptor>();
        textureList.add(BitmapDescriptorFactory.fromResource(R.drawable.custtexture));	//��ͷͼyb

        List<Integer> texIndexList = new ArrayList<Integer>();
        texIndexList.add(0);//��Ӧ����ĵ�0������
        texIndexList.add(1);
        texIndexList.add(2);

        Random random = new Random();
        for (int i = 0; i < pointList.size(); i++) {
            colorList.add(colors[random.nextInt(3)]);
            bitmapDescriptors.add(textureList.get(0));	//���ݳ��Ȳ��ϵ�ƴ�Ӽ�ͷͼyb

        }
        //����yb
        mPolyline = aMap.addPolyline(new PolylineOptions().setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.custtexture)) //setCustomTextureList(bitmapDescriptors)
//				.setCustomTextureIndex(texIndexList)
                .addAll(latlngList)
                .useGradient(true)
                .width(20));

//        LatLngBounds bounds = new LatLngBounds(pointList.get(0), pointList.get(pointList.size() - 2));	//��ʾ��Χ�ڿ�ʼ�㵽������yb
//        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));//��������������ʾ�ڹ涨��Ļ��Χ�ڵĵ�ͼ��γ�ȷ�Χyb
        //aMap.animateCamera(CameraUpdateFactory.changeLatLng(oldLatLng));//��������������ʾ�ڹ涨��Ļ��Χ�ڵĵ�ͼ��γ�ȷ�Χyb
    }
	
    //��λ���߳�
    public class MyThread implements Runnable{
    	@Override
    	public void run() {
	    	while (threadable) {
		    	try {
			    	Thread.sleep(2000);//�߳���ͣ����λ����
			    	//������λ  
					aMapLocationClient.setLocationOption(aMapLocationClientOption);
			        aMapLocationClient.startLocation();
			        Double lat1 = aMapLocationClient.getLastKnownLocation().getLatitude();//��¼��γ��
	        		Double lon1 = aMapLocationClient.getLastKnownLocation().getLongitude();
			    	
			    	Message message=new Message();
			    	message.what=1;
			    	//����λ��Ϣ�����bundle�������
			    	Bundle locationBundle = new Bundle();
			    	locationBundle.putDouble("lat", lat1);
			    	locationBundle.putDouble("lon", lon1);
			    	message.setData(locationBundle);
			    	handler.sendMessage(message);//������Ϣ
		    	} catch (InterruptedException e) {
		    		e.printStackTrace();
		    	}
	    	}
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mapView.onDestroy();
    	threadable = false; //�����ر��߳�
    	aMapLocationClient.onDestroy();
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	mapView.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mapView.onPause();
    }
    @Override
    protected void onStop() {
    	super.onStop();
    	aMapLocationClient.stopLocation();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mapView.onSaveInstanceState(outState);
    }

    
    /**
     * �ο�demo
     * ��ʼ�ƶ�--�켣���ţ���ʼ�ƶ�
     */
    public void test4(View v) {

        if (mPolyline == null) {
        	Toast.makeText(MainActivity.this, "yuanbao�����߳�·��", 0).show();
            return;
        }

        // ��ȡ�켣��
        // ���� �켣����ʾ����
//        LatLngBounds bounds = new LatLngBounds(pointList.get(0), pointList.get(pointList.size() - 2));
//        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

        // ʵ�� SmoothMoveMarker����
        SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
        // ���� ƽ���ƶ���ͼ��
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.icon_car));

        // ȡ�켣��ĵ�һ���� ��Ϊ ƽ���ƶ�������
        LatLng drivePoint = pointList.get(0);	//���
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(pointList, drivePoint);
        pointList.set(pair.first, drivePoint);
        List<LatLng> subList = pointList.subList(pair.first, pointList.size());

        // ���ù켣��
        smoothMarker.setPoints(subList);
        // ����ƽ���ƶ�����ʱ��  ��λ  ��
        smoothMarker.setTotalDuration(30);

        // ����  �Զ����InfoWindow ������
        aMap.setInfoWindowAdapter(infoWindowAdapter);
        // ��ʾ infowindow
        smoothMarker.getMarker().showInfoWindow();

        // �����ƶ��ļ����¼�  ���� ���յ�ľ���  ��λ ��
        smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
            @Override
            public void move(final double distance) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (infoWindowLayout != null && title != null) {
                            title.setText("yb�����յ㻹�У� " + (int)distance + "��");
                        }
                    }
                });

            }
        });

        // ��ʼ�ƶ�
        smoothMarker.startSmoothMove();
    }
    /**
     *  ���Ի����Ƶ���Ϣ������ͼ����
     *  ���Ҫ���ƻ���Ⱦ�����Ϣ���ڣ���Ҫ����getInfoWindow(Marker)������
     *  ���ֻ����Ҫ�滻��Ϣ���ڵ����ݣ�����Ҫ����getInfoContents(Marker)������
     */
    AMap.InfoWindowAdapter infoWindowAdapter = new AMap.InfoWindowAdapter(){

        // ���Ի�Marker��InfoWindow ��ͼ
        // ��������������null���򽫻�ʹ��Ĭ�ϵ���Ϣ���ڷ�����ݽ������getInfoContents(Marker)������ȡ
        @Override
        public View getInfoWindow(Marker marker) {

            return getInfoWindowView(marker);
        }

        // �������ֻ����getInfoWindow(Marker)����null ʱ�Żᱻ����
        // ���ƻ���view �������Ϣ���ڵ����ݣ��������null ����Ĭ��������Ⱦ
        @Override
        public View getInfoContents(Marker marker) {

            return getInfoWindowView(marker);
        }
    };

    LinearLayout infoWindowLayout;
    TextView title;
    TextView snippet;

    /**
     * �Զ���View���Ұ����ݷ���
     * @param marker �����Marker����
     * @return  �����Զ��崰�ڵ���ͼ
     */
    private View getInfoWindowView(Marker marker) {
        if (infoWindowLayout == null) {
            infoWindowLayout = new LinearLayout(this);
            infoWindowLayout.setOrientation(LinearLayout.VERTICAL);
            title = new TextView(this);
            snippet = new TextView(this);
            title.setTextColor(Color.BLACK);
            snippet.setTextColor(Color.BLACK);
            infoWindowLayout.setBackgroundResource(R.drawable.infowindow_bg);

            infoWindowLayout.addView(title);
            infoWindowLayout.addView(snippet);
        }

        return infoWindowLayout;
    }
    
}

////demo���ʵ�ֶ�λ����
//MyLocationStyle myLocationStyle = new MyLocationStyle();//��ʼ����λ������ʽ��
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//������λ���ҽ��ӽ��ƶ�����ͼ���ĵ㣬��λ�������豸������ת�����һ�����豸�ƶ�����1��1�ζ�λ�����������myLocationType��Ĭ��Ҳ��ִ�д���ģʽ��
//myLocationStyle.interval(2000); //����������λģʽ�µĶ�λ�����ֻ��������λģʽ����Ч�����ζ�λģʽ�²�����Ч����λΪ���롣
//aMap.setMyLocationStyle(myLocationStyle);//���ö�λ�����Style
////aMap.getUiSettings().setMyLocationButtonEnabled(true);����Ĭ�϶�λ��ť�Ƿ���ʾ���Ǳ������á�
//aMap.setMyLocationEnabled(true);// ����Ϊtrue��ʾ������ʾ��λ���㣬false��ʾ���ض�λ���㲢�����ж�λ��Ĭ����false��	
//
////��λ����5��ģʽ
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);//ֻ��λһ�Ρ�
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE) ;//��λһ�Σ��ҽ��ӽ��ƶ�����ͼ���ĵ㡣
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW) ;//������λ���ҽ��ӽ��ƶ�����ͼ���ĵ㣬��λ��������豸�ƶ�����1��1�ζ�λ��
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE);//������λ���ҽ��ӽ��ƶ�����ͼ���ĵ㣬��ͼ�����豸������ת����λ�������豸�ƶ�����1��1�ζ�λ��
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//������λ���ҽ��ӽ��ƶ�����ͼ���ĵ㣬��λ�������豸������ת�����һ�����豸�ƶ�����1��1�ζ�λ��Ĭ��ִ�д���ģʽ��

/*
//��λ�ɹ��ص���Ϣ�����������Ϣ  
	aMapLocation.getLocationType();//��ȡ��ǰ��λ�����Դ�������綨λ����������λ���ͱ�  
	aMapLocation.getLatitude();//��ȡγ��  
	aMapLocation.getLongitude();//��ȡ����  
	aMapLocation.getAccuracy();//��ȡ������Ϣ  
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
	Date date = new Date(aMapLocation.getTime());  
	df.format(date);//��λʱ��  
	aMapLocation.getAddress();//��ַ�����option������isNeedAddressΪfalse����û�д˽�������綨λ����л��е�ַ��Ϣ��GPS��λ�����ص�ַ��Ϣ��  
	aMapLocation.getCountry();//������Ϣ  
	aMapLocation.getProvince();//ʡ��Ϣ  
	aMapLocation.getCity();//������Ϣ  
	aMapLocation.getDistrict();//������Ϣ  
	aMapLocation.getStreet();//�ֵ���Ϣ  
	aMapLocation.getStreetNum();//�ֵ����ƺ���Ϣ  
	aMapLocation.getCityCode();//���б���  
	aMapLocation.getAdCode();//��������  
	aMapLocation.getAoiName();//��ȡ��ǰ��λ���AOI��Ϣ  
*/

/*
	aMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(Constants.BEIJING, 12));// ��������ָ���Ŀ��������ͼ
    

*/




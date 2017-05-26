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
 * 课本LBS练习，书上使用的是百度sdk ，自己用的高德AMap sdk
 * 用MapView实现地图显示。实现定位、跳转到定位页面，获取定位坐标信息。
 * 2017-5-6 增加轨迹划线的功能，还未实现服务器存储
 * 2017-5-8 实现子线程刷新定位，并将轨迹绘制路线。
 * 2017-5-9 将轨迹绘制成自定义图的曲线(箭头)，用集合存放点坐标。实现高精度模式。bug：小蓝点与所画轨迹各是各，即两种定位方式
 */

public class MainActivity extends Activity {

	MapView mapView = null;	//显示地图
	TextView position;	//定位坐标经纬度
	double lat;	//纬度
	double lon;	//经度
	private StringBuilder currentPosition;	//用来存储实时位置坐标
	private AMapLocationClient aMapLocationClient;	//定位客户端
	private AMapLocationClientOption aMapLocationClientOption;	//定位设置的
	private AMap aMap = null;
	private UiSettings mUiSettings;
	private MyLocationStyle myLocationStyle;
	
	private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);//定位小蓝点的颜色
	private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
	
	private Button btn_locateThread;
	private Thread locateThread;
	
    private LatLng oldLatLng;  //以前的定位点  
//    private LatLng newLatLng;  //新的定位点  	//不能弄成成员变量，需要每个都能new出来添加到pointList中
    private boolean isFirstLatLng = true; //是否是第一次定位  
    boolean threadable = false;  //定义全局变量threadable，用于控制线程开、关  
	int i=0;
	List<LatLng> pointList;
	private Polyline mPolyline;
    
    //通过子线程方式不停调取定位。handler发送信息给主线程.可在此进行ui操作
    Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case 1:
				//小蓝点位置更新
				setupLocationStyle();
				aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE));
				
				Bundle b = msg.getData();
	    		lat = b.getDouble("lat");
	    		lon = b.getDouble("lon");
				LatLng newLatLng = new LatLng(lat,lon); 
				if(isFirstLatLng){  	//就执行第一次，后面不再执行到了
		            //记录第一次的定位信息  
					pointList.add(newLatLng);
		            oldLatLng = newLatLng;  
		            isFirstLatLng = false;  
		        }  
		        //位置有变化  
		        if(!oldLatLng.equals(newLatLng)){  //之前是 oldLatLng ！= newLatLng; 并不能判断.//千万注意加"!"
		        	pointList.add(newLatLng);
		        	if(mPolyline != null){
		        		mPolyline.remove();		//先擦除，再更新线条
		        	}
	        		addPolylineInPlayGround(pointList);
		        	
//		            drawLineOnMap(oldLatLng, newLatLng);
		     		//创建一个设置经纬度的CameraUpdate
		            CameraUpdate cu = CameraUpdateFactory.changeLatLng(newLatLng);
		            //更新地图的显示区域
		            aMap.moveCamera(cu);
		            oldLatLng = newLatLng;  
		        }
		        Log.e("location", i+"_yuanbao定位Test "+pointList.size());
// 				Toast.makeText(MainActivity.this, i+"_yuanbao定位Test "+pointList.size()+" 时间："+System.currentTimeMillis(), 0).show();
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏
        setContentView(R.layout.activity_main);
        
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        position = (TextView) findViewById(R.id.tv_position);
        btn_locateThread = (Button) findViewById(R.id.btn_locate_thread);
        
        pointList = new ArrayList<LatLng>();
        initAMap();
        initPermission();	//初始化权限处理
        initLocation();	//自定义初始化地图定位的 基本设置
        
       /* //位置变化后自动更新定位
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300, 8, new LocationListener() {
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				
			}
			
			@Override
			public void onProviderEnabled(String provider) {
				// 使用GPRS提供的定位信息来更新位置
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
     * 检测到位置发生变化后的方法
     * @param location
     */
	protected void updatePosition(Location location) {
		LatLng newLatLng = new LatLng(location.getLatitude(), location.getLongitude()); 
		if(isFirstLatLng){  	//就执行第一次，后面不再执行到了
            //记录第一次的定位信息  
            oldLatLng = newLatLng;  
            isFirstLatLng = false;  
        }  
        //位置有变化  
        if(oldLatLng != newLatLng){  
            drawLineOnMap(oldLatLng, newLatLng);	//划线
            //创建一个设置经纬度的CameraUpdate
            CameraUpdate cu = CameraUpdateFactory.changeLatLng(newLatLng);
            //更新地图的显示区域
            aMap.moveCamera(cu);
/*    		//清除所有的Marker等覆盖物
            aMap.clear();*/
            //创建一个MarkerOptions对象
            MarkerOptions markOptions = new MarkerOptions();
            markOptions.position(newLatLng);
            //添加MarkerOptions（实际上是添加Marker）
            Marker marker = aMap.addMarker(markOptions);
            oldLatLng = newLatLng;  
        }
	}

	private void initAMap() {
		if(aMap == null){
        	aMap = mapView.getMap();
        	
        	mUiSettings = aMap.getUiSettings();		//获得一个地图ui设置对象
        	mUiSettings.setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        	mUiSettings.setScaleControlsEnabled(true);	//设置比例尺是否显示
        	
    		aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
//    		setupLocationStyle();	//初始化定位小蓝点
        }
		
		if(aMapLocationClient == null){
			aMapLocationClient = new AMapLocationClient(getApplicationContext());//初始化client
		}
		
	}

	private void initPermission() {
		List<String> permissionList = new ArrayList<String>();	//权限集合，统一申请
		
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
			//启动定位  
	        aMapLocationClient.startLocation();  
		}
	}

	private void initLocation() {
		
		//初始化定位参数  
        aMapLocationClientOption = new AMapLocationClientOption();  
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式  
        aMapLocationClientOption.setLocationMode(AMapLocationMode.Hight_Accuracy);  
        //设置是否返回地址信息（默认返回地址信息）  
        aMapLocationClientOption.setNeedAddress(true);  
        //设置是否只定位一次,默认为false  
        aMapLocationClientOption.setOnceLocation(false);  
        //设置是否强制刷新WIFI，默认为强制刷新  
        aMapLocationClientOption.setWifiScan(true);  //原来为setWifiActiveScan()已过时
        //设置是否允许模拟位置,默认为false，不允许模拟位置  
        aMapLocationClientOption.setMockEnable(true);  //开了后genymotion模拟器打开才不挂掉了
        //设置定位间隔,单位毫秒,默认为2000ms  
        aMapLocationClientOption.setInterval(1000);
        aMapLocationClientOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        //给定位客户端对象设置定位参数  
        aMapLocationClient.setLocationOption(aMapLocationClientOption);  
        aMapLocationClient.setLocationListener(aMapLocationListener);	//aMapLocationListener 设置定位回调监听
        //启动定位  
        aMapLocationClient.startLocation();  
        
     // 自定义系统定位蓝点
		myLocationStyle = new MyLocationStyle();
	}
	
	//运行时权限处理
	@Override
	public void onRequestPermissionsResult(int requestCode,
			String[] permissions, int[] grantResults) {
		switch (requestCode) {
		case 1:
			if(grantResults.length > 0){
				for (int result : grantResults) {
					if(result != PackageManager.PERMISSION_GRANTED){
						Toast.makeText(this, "请同意所有权限才能使用本app", 0).show();
//						finish();	//不知道怎么回事没有弹出运行时权限申请的对话框
						return;
					}
				}
				//启动定位  
		        aMapLocationClient.startLocation();  
			}else{
				Toast.makeText(this, "发生未知错误，请卸载后重装", 0).show();
//				finish();
			}
			break;
		default:
			break;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
    
	/*//自定义一个定位监听对象
	public class MyLocationListener implements AMapLocationListener{

		@Override
		public void onLocationChanged(AMapLocation aMapLocation) {
			if(aMapLocation != null){
				if (aMapLocation.getErrorCode() == 0) {  
                  
					//自己调用的内容
                    lat = aMapLocation.getLatitude();  
                    lon = aMapLocation.getLongitude();  
                    Log.v("pcw","lat : "+lat+" lon : "+lon); 
                    
                    currentPosition = new StringBuilder();
                    currentPosition.append("纬度：").append(lat).append("\n");
            		currentPosition.append("经度：").append(lon).append("\n");
            		
            		//加一些其他信息
            		currentPosition.append("国家：").append(aMapLocation.getCountry()).append("\n");
            		currentPosition.append("省：").append(aMapLocation.getProvince()).append("\n");
            		currentPosition.append("市：").append(aMapLocation.getCity()).append("\n");
            		currentPosition.append("区：").append(aMapLocation.getDistrict()).append("\n");
            		currentPosition.append("街道：").append(aMapLocation.getStreet()).append("\n");
            		currentPosition.append("详细地址：").append(aMapLocation.getAddress()).append("\n");
            		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
                    Date date = new Date(aMapLocation.getTime());  
                    currentPosition.append("定位时间：").append(df.format(date)).append("\n");
                    df.format(date);	//定位时间 
            		
            		
            		currentPosition.append("定位方式：");
            		if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_GPS){
            			currentPosition.append("GPS");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_WIFI){
            			currentPosition.append("WIFI");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_CELL){
            			currentPosition.append("gprs流量");
            		}else {
            			currentPosition.append("NULL");
            		}
            		
            		position.setText(currentPosition.toString());	//显示出来
            		
            		//定位变化轨迹划线
            		updatePosition(aMapLocation);
            		
            		
                } else {  
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。  
                    Log.e("AmapError", "location Error, ErrCode:"  
                            + aMapLocation.getErrorCode() + ", errInfo:"  
                            + aMapLocation.getErrorInfo());  
                }
			}
		}
	}*/
	
	/**方式2
	 * 定位监听对象，但是感觉没有生效，对定位变化没有做出响应事件
	 */
    AMapLocationListener aMapLocationListener = new AMapLocationListener() {
		int i=0;
		@Override
		public void onLocationChanged(AMapLocation aMapLocation) {
			if(aMapLocation != null){
				currentPosition = new StringBuilder();
				
				if (aMapLocation.getErrorCode() == 0) {  
                    //定位成功回调信息，设置相关消息  
                    lat = aMapLocation.getLatitude();  
                    lon = aMapLocation.getLongitude();  
                    Log.v("pcw","lat : "+lat+" lon : "+lon); 
                    
                    currentPosition.append("纬度：").append(lat).append("\n");
            		currentPosition.append("经度：").append(lon).append("\n");
                    //加一些其他信息
            		currentPosition.append("国家：").append(aMapLocation.getCountry()).append("\n");
            		currentPosition.append("省：").append(aMapLocation.getProvince()).append("\n");
            		currentPosition.append("市：").append(aMapLocation.getCity()).append("\n");
            		currentPosition.append("区：").append(aMapLocation.getDistrict()).append("\n");
            		currentPosition.append("街道：").append(aMapLocation.getStreet()).append("\n");
            		currentPosition.append("详细地址：").append(aMapLocation.getAddress()).append("\n");
            		
            		SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
                    Date date1 = new Date(aMapLocation.getTime());  
                    currentPosition.append("定位时间：").append(df1.format(date1)).append("\n");
                    df1.format(date1);//定位时间按格式输出
            		
            		if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_GPS){
            			currentPosition.append("GPS");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_WIFI){
            			currentPosition.append("WIFI");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_CELL){
            			currentPosition.append("gprs流量");
            		}else if(aMapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_OFFLINE){
            			currentPosition.append("离线");
            		}
            		position.setText(currentPosition.toString());	//显示出来
            		
            		updatePosition(aMapLocation);
            		Toast.makeText(MainActivity.this, i+"定位更新了", 0).show();
            		i++;
            		
                } else {  
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。  
                    Log.e("AmapError", "location Error, ErrCode:"  
                            + aMapLocation.getErrorCode() + ", errInfo:"  
                            + aMapLocation.getErrorInfo());  
                }
			}
		}
	};
	
	/**按钮1点击，显示坐标位置。
	 * 点击启动定位，并将页面移动至最新定位出，将轨迹划线。
	 * @param v
	 */
	public void locate1(View v){
		setupLocationStyle();
		aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE));
      
		//启动定位  
		aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.startLocation();  
        
        if(aMapLocationClient.getLastKnownLocation() != null){	//之前不判断时模拟器在这儿报空指针挂掉
        	
        	LatLng newLatLng = new LatLng(aMapLocationClient.getLastKnownLocation().getLatitude(), 
        			aMapLocationClient.getLastKnownLocation().getLongitude()); 
        	if(isFirstLatLng){  	//就执行第一次，后面不再执行到了
        		//记录第一次的定位信息  
        		oldLatLng = newLatLng;  
        		isFirstLatLng = false;  
        	}  
        	//位置有变化  
        	if(!oldLatLng.equals(newLatLng)){  
//        		drawLineOnMap(oldLatLng, newLatLng);
        		//创建一个设置经纬度的CameraUpdate
        		CameraUpdate cu = CameraUpdateFactory.changeLatLng(newLatLng);
        		//更新地图的显示区域
        		aMap.moveCamera(cu);
        		oldLatLng = newLatLng;  
        	}
        	
        }else{
        	Toast.makeText(MainActivity.this, "yb请稍等，第一次定位还未完成", 0).show();
        }
		
	}
	
	public void locate2(View v){
		setupLocationStyle();
        aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE));

	}
	//跳转到下个页面详细显示定位地理信息
	public void locate3(View v){
		Intent intent = new Intent(MainActivity.this, LocaInfoActivity.class);
		intent.putExtra("locaInfo", currentPosition.toString());
		startActivity(intent);
	}
	
	//test1按钮：marker测试
	public void test1(View v){
		aMap.setMyLocationEnabled(true);
		
		//使用Marker标记点，点击后有详情框
		MarkerOptions markerOption = new MarkerOptions();
	    markerOption.position(Constants.WUHAN)
	    	.title("武汉市")	//标题
	    	.snippet("每天不一样,wuhan,new city！")	//描述
	    	.draggable(true)	//设置Marker可拖动
	    	.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
	    			.decodeResource(getResources(),R.drawable.poi_marker_1)));
	    // 将Marker设置为贴地显示，可以双指下拉地图查看效果
	    markerOption.setFlat(true);//设置marker平贴地图效果
	    
	    Marker marker = aMap.addMarker(markerOption);//不要漏掉
//	    marker.showInfoWindow();	//显示信息详情框。隐藏 marker.hideInfoWindow();
	    
	    
/*	    //增加动画效果
	    Animation animation = new RotateAnimation(marker.getRotateAngle(),marker.getRotateAngle()+180,0,0,0);
	    animation.setDuration(1000);
	    animation.setInterpolator(new LinearInterpolator());
	    marker.setAnimation(animation);	
	    marker.startAnimation();*/
	    
	    //信息窗口点击响应
        OnInfoWindowClickListener listener = new OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Toast.makeText(MainActivity.this, "yuanbao,点击了infowindow", 0).show();
            }
        };
        //绑定信息窗点击事件
        aMap.setOnInfoWindowClickListener(listener);
		
	}
	
	//test2按钮：画线测试
	public void test2(View v){
		Polyline polyline = aMap.addPolyline((new PolylineOptions())
				.add(Constants.SHANGHAI, Constants.BEIJING, Constants.WUHAN)
				.width(7)
				.setDottedLine(true)//设置虚线
				.color(Color.argb(255, 1, 1, 1)));
		
//		drawLineOnMap(new LatLng(30.55, 114.30), new LatLng(32.2, 115.3));	//测试用
		
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
	
	//test3按钮：开启子线程测试（定位）
	public void test3(View v){
		locateThread = new Thread(new MyThread());
		
		if(!threadable){
			btn_locateThread.setText("停止定位线程");
			aMap.setMyLocationEnabled(true);
			threadable = true;
		}else{
			btn_locateThread.setText("继续定位线程");
			threadable = false;
		}
		locateThread.start();
	}
	
	/**
	 * 设置自定义定位蓝点
	 */
	private void setupLocationStyle(){
		
		myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
		myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
		// 自定义定位蓝点图标
		myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
				fromResource(R.drawable.gps_point_yb2));
		// 自定义精度范围的圆形边框颜色
		myLocationStyle.strokeColor(STROKE_COLOR);
		//自定义精度范围的圆形边框宽度
		myLocationStyle.strokeWidth(5);
		// 设置圆形的填充颜色
		myLocationStyle.radiusFillColor(FILL_COLOR);
		/*//定位蓝点图标的锚点方法
		myLocationStyle.anchor(0, 0);*/
		
		// 将自定义的 myLocationStyle 对象添加到地图上
		aMap.setMyLocationStyle(myLocationStyle);
		//aMap.getUiSettings().setMyLocationButtonEnabled(true);设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。	

	}
	
	/**
	 * 绘制两个坐标点之间的线段,从以前位置到现在位置
	 */  
    private void drawLineOnMap(LatLng oldLatLng, LatLng newLatLng ) {  
        // 绘制一个大地曲线  
        aMap.addPolyline((new PolylineOptions())
        		.add(oldLatLng, newLatLng)  
                .geodesic(true)
                .color(Color.GREEN));  
    }
    /**
     * 参考demo
     * yb添加轨迹线	
     */
    private void addPolylineInPlayGround(List<LatLng> latlngList) {
        List<Integer> colorList = new ArrayList<Integer>();
        List<BitmapDescriptor> bitmapDescriptors = new ArrayList<BitmapDescriptor>();
        //三种颜色yb
        int[] colors = new int[]{Color.argb(255, 0, 255, 0),Color.argb(255, 255, 255, 0),Color.argb(255, 255, 0, 0)};

        //用一个数组来存放纹理
        List<BitmapDescriptor> textureList = new ArrayList<BitmapDescriptor>();
        textureList.add(BitmapDescriptorFactory.fromResource(R.drawable.custtexture));	//箭头图yb

        List<Integer> texIndexList = new ArrayList<Integer>();
        texIndexList.add(0);//对应上面的第0个纹理
        texIndexList.add(1);
        texIndexList.add(2);

        Random random = new Random();
        for (int i = 0; i < pointList.size(); i++) {
            colorList.add(colors[random.nextInt(3)]);
            bitmapDescriptors.add(textureList.get(0));	//根据长度不断的拼接箭头图yb

        }
        //画线yb
        mPolyline = aMap.addPolyline(new PolylineOptions().setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.custtexture)) //setCustomTextureList(bitmapDescriptors)
//				.setCustomTextureIndex(texIndexList)
                .addAll(latlngList)
                .useGradient(true)
                .width(20));

//        LatLngBounds bounds = new LatLngBounds(pointList.get(0), pointList.get(pointList.size() - 2));	//显示范围在开始点到结束点yb
//        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));//动画更新设置显示在规定屏幕范围内的地图经纬度范围yb
        //aMap.animateCamera(CameraUpdateFactory.changeLatLng(oldLatLng));//动画更新设置显示在规定屏幕范围内的地图经纬度范围yb
    }
	
    //定位子线程
    public class MyThread implements Runnable{
    	@Override
    	public void run() {
	    	while (threadable) {
		    	try {
			    	Thread.sleep(2000);//线程暂停，单位毫秒
			    	//启动定位  
					aMapLocationClient.setLocationOption(aMapLocationClientOption);
			        aMapLocationClient.startLocation();
			        Double lat1 = aMapLocationClient.getLastKnownLocation().getLatitude();//记录经纬度
	        		Double lon1 = aMapLocationClient.getLastKnownLocation().getLongitude();
			    	
			    	Message message=new Message();
			    	message.what=1;
			    	//将定位信息打包成bundle打包发送
			    	Bundle locationBundle = new Bundle();
			    	locationBundle.putDouble("lat", lat1);
			    	locationBundle.putDouble("lon", lon1);
			    	message.setData(locationBundle);
			    	handler.sendMessage(message);//发送消息
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
    	threadable = false; //用来关闭线程
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
     * 参考demo
     * 开始移动--轨迹播放：开始移动
     */
    public void test4(View v) {

        if (mPolyline == null) {
        	Toast.makeText(MainActivity.this, "yuanbao请先走出路线", 0).show();
            return;
        }

        // 读取轨迹点
        // 构建 轨迹的显示区域
//        LatLngBounds bounds = new LatLngBounds(pointList.get(0), pointList.get(pointList.size() - 2));
//        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

        // 实例 SmoothMoveMarker对象
        SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
        // 设置 平滑移动的图标
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.icon_car));

        // 取轨迹点的第一个点 作为 平滑移动的启动
        LatLng drivePoint = pointList.get(0);	//起点
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(pointList, drivePoint);
        pointList.set(pair.first, drivePoint);
        List<LatLng> subList = pointList.subList(pair.first, pointList.size());

        // 设置轨迹点
        smoothMarker.setPoints(subList);
        // 设置平滑移动的总时间  单位  秒
        smoothMarker.setTotalDuration(30);

        // 设置  自定义的InfoWindow 适配器
        aMap.setInfoWindowAdapter(infoWindowAdapter);
        // 显示 infowindow
        smoothMarker.getMarker().showInfoWindow();

        // 设置移动的监听事件  返回 距终点的距离  单位 米
        smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
            @Override
            public void move(final double distance) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (infoWindowLayout != null && title != null) {
                            title.setText("yb距离终点还有： " + (int)distance + "米");
                        }
                    }
                });

            }
        });

        // 开始移动
        smoothMarker.startSmoothMove();
    }
    /**
     *  个性化定制的信息窗口视图的类
     *  如果要定制化渲染这个信息窗口，需要重载getInfoWindow(Marker)方法。
     *  如果只是需要替换信息窗口的内容，则需要重载getInfoContents(Marker)方法。
     */
    AMap.InfoWindowAdapter infoWindowAdapter = new AMap.InfoWindowAdapter(){

        // 个性化Marker的InfoWindow 视图
        // 如果这个方法返回null，则将会使用默认的信息窗口风格，内容将会调用getInfoContents(Marker)方法获取
        @Override
        public View getInfoWindow(Marker marker) {

            return getInfoWindowView(marker);
        }

        // 这个方法只有在getInfoWindow(Marker)返回null 时才会被调用
        // 定制化的view 做这个信息窗口的内容，如果返回null 将以默认内容渲染
        @Override
        public View getInfoContents(Marker marker) {

            return getInfoWindowView(marker);
        }
    };

    LinearLayout infoWindowLayout;
    TextView title;
    TextView snippet;

    /**
     * 自定义View并且绑定数据方法
     * @param marker 点击的Marker对象
     * @return  返回自定义窗口的视图
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

////demo里的实现定位蓝点
//MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
//myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
//aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
////aMap.getUiSettings().setMyLocationButtonEnabled(true);设置默认定位按钮是否显示，非必需设置。
//aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。	
//
////定位蓝点5种模式
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);//只定位一次。
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE) ;//定位一次，且将视角移动到地图中心点。
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW) ;//连续定位、且将视角移动到地图中心点，定位蓝点跟随设备移动。（1秒1次定位）
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE);//连续定位、且将视角移动到地图中心点，地图依照设备方向旋转，定位点会跟随设备移动。（1秒1次定位）
//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）默认执行此种模式。

/*
//定位成功回调信息，设置相关消息  
	aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表  
	aMapLocation.getLatitude();//获取纬度  
	aMapLocation.getLongitude();//获取经度  
	aMapLocation.getAccuracy();//获取精度信息  
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
	Date date = new Date(aMapLocation.getTime());  
	df.format(date);//定位时间  
	aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。  
	aMapLocation.getCountry();//国家信息  
	aMapLocation.getProvince();//省信息  
	aMapLocation.getCity();//城市信息  
	aMapLocation.getDistrict();//城区信息  
	aMapLocation.getStreet();//街道信息  
	aMapLocation.getStreetNum();//街道门牌号信息  
	aMapLocation.getCityCode();//城市编码  
	aMapLocation.getAdCode();//地区编码  
	aMapLocation.getAoiName();//获取当前定位点的AOI信息  
*/

/*
	aMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(Constants.BEIJING, 12));// 更新设置指定的可视区域地图
    

*/




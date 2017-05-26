package cn.yuanbao.test0503lbs_amap1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

/**
 * 用来暂时定位详细信息的活动界面，接收上一个activity传来的数据
 * @author yuanbao15
 *
 */

public class LocaInfoActivity extends Activity {
	TextView position;	//定位坐标经纬度
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏
        setContentView(R.layout.activity_loca_info);
		
        position = (TextView) findViewById(R.id.tv_position);
        
        Intent intent = getIntent();
        String locaInfo = intent.getStringExtra("locaInfo");
        position.setText(locaInfo);
	}
}

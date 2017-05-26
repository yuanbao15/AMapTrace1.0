package cn.yuanbao.test0503lbs_amap1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

/**
 * ������ʱ��λ��ϸ��Ϣ�Ļ���棬������һ��activity����������
 * @author yuanbao15
 *
 */

public class LocaInfoActivity extends Activity {
	TextView position;	//��λ���꾭γ��
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// ����ʾ����ı�����
        setContentView(R.layout.activity_loca_info);
		
        position = (TextView) findViewById(R.id.tv_position);
        
        Intent intent = getIntent();
        String locaInfo = intent.getStringExtra("locaInfo");
        position.setText(locaInfo);
	}
}

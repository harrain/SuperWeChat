package cn.ucai.superwechat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;

import cn.ucai.superwechat.SigninActivity;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.utils.MFGT;

import com.hyphenate.util.EasyUtils;

/**
 * 开屏页
 *
 */
public class SplashActivity extends BaseActivity {

	private static final int sleepTime = 2000;

	@Override
	protected void onCreate(Bundle arg0) {
		setContentView(R.layout.em_activity_splash);
		super.onCreate(arg0);

		RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.splash_root);


		AlphaAnimation animation = new AlphaAnimation(0.3f, 1.0f);
		animation.setDuration(1500);
		rootLayout.startAnimation(animation);


	}

	@Override
	protected void onStart() {
		super.onStart();

		Runnable runnable = new Runnable() {
			public void run() {
				if (SuperWeChatHelper.getInstance().isLoggedIn()) {
					// auto login mode, make sure all group and conversation is loaed before enter the main screen
					long start = System.currentTimeMillis();
					EMClient.getInstance().chatManager().loadAllConversations();
					EMClient.getInstance().groupManager().loadAllGroups();
					long costTime = System.currentTimeMillis() - start;
					//wait
					if (sleepTime - costTime > 0) {
						try {
							Thread.sleep(sleepTime - costTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					String topActivityName = EasyUtils.getTopActivityName(EMClient.getInstance().getContext());
					if (topActivityName != null && (topActivityName.equals(VideoCallActivity.class.getName()) || topActivityName.equals(VoiceCallActivity.class.getName()))) {
						// nop
						// avoid main screen overlap Calling Activity
					} else {
						//enter main screen
						startActivity(new Intent(SplashActivity.this, MainActivity.class));
					}
					MFGT.finish(SplashActivity.this);
				}else {
					MFGT.gotoSign(SplashActivity.this);
					MFGT.finish(SplashActivity.this);
				}
			}
		};
		new Handler().postDelayed(runnable,1000);
	}
	
	/**
	 * get sdk version
	 */
	private String getVersion() {
	    return EMClient.getInstance().VERSION;
	}
}

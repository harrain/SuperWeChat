package cn.ucai.superwechat.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import cn.ucai.superwechat.R;
import cn.ucai.superwechat.ui.LoginActivity;
import cn.ucai.superwechat.ui.MainActivity;

/**
 * Created by CDLX on 2017/5/19.
 */

public class MFGT {

    private static void startActivity(Context context,Class clazz){
        context.startActivity(new Intent(context,clazz));
        ((Activity)context).overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }

    public static void gotoMain(Activity activity){
        startActivity(activity, MainActivity.class);
    }

    public static void gotoLogin(Activity activity){
        startActivity(activity, LoginActivity.class);
    }

    public static void finish(Activity activity){
        activity.overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
        activity.finish();
    }
}

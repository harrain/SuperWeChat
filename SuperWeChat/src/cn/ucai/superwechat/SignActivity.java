package cn.ucai.superwechat;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import cn.ucai.superwechat.ui.LoginActivity;
import cn.ucai.superwechat.ui.RegisterActivity;
import cn.ucai.superwechat.ui.SplashActivity;

public class SignActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_sign);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignActivity.this, LoginActivity.class));
                overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
                finish();
            }
        });
        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignActivity.this, RegisterActivity.class));
                overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
                finish();
            }
        });
    }

}

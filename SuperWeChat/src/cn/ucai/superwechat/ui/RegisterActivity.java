/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.data.OnCompleteListener;
import cn.ucai.superwechat.data.net.IUserModel;
import cn.ucai.superwechat.data.net.UserModel;
import cn.ucai.superwechat.model.RegisterInfo;
import cn.ucai.superwechat.model.User;
import cn.ucai.superwechat.utils.MD5;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;
import cn.ucai.superwechat.utils.L;

/**
 * register screen
 *
 */
public class RegisterActivity extends BaseActivity {
    @BindView(R.id.username)
    EditText mUsername;
    @BindView(R.id.nickname_tv)
    EditText mNick;
    @BindView(R.id.password)
    EditText mPassword;
    @BindView(R.id.confirm_password)
    EditText mConfirmPassword;

    IUserModel model;
    String username,usernick;
    private String pwd;
    private String confirm_pwd;
    private String TAG = "registacitviy";
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_register);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.regist_toobar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void register(View view) {

        if (checkInput()) {
            pd = new ProgressDialog(this);
            pd.setMessage(getResources().getString(R.string.Is_the_registered));
            pd.show();

            new Thread(new Runnable() {
                public void run() {
                    try {
                        // call method in SDK
                        EMClient.getInstance().createAccount(username, pwd);
                        registToAppServer();

                    } catch (final HyphenateException e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (!RegisterActivity.this.isFinishing())
                                    pd.dismiss();
                                unRegister();
                                int errorCode = e.getErrorCode();
                                if (errorCode == EMError.NETWORK_ERROR) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }).start();

        }
    }

    private void registToAppServer() {

            if (checkInput()) {
                Log.e(TAG,"check完毕");
                model = new UserModel();
                model.register(RegisterActivity.this, username, usernick, MD5.getMessageDigest(pwd),
                        new OnCompleteListener<String>() {
                            @Override
                            public void onSuccess(String s) {
                                L.e(TAG, "s=" + s);

                                handleJson(s);

                            }

                            @Override
                            public void onError(String error) {
                            }
                        });
            }

    }

    private void handleJson(String s){
        if (s != null) {
            Result result = ResultUtils.getResultFromJson(s, RegisterInfo.class);
            if (result != null) {
                Log.e(TAG,result.toString());
                if (result.getRetCode() == I.MSG_REGISTER_USERNAME_EXISTS) {
                    mUsername.requestFocus();
                    mUsername.setError(getString(R.string.register_fail_exists));
                } else if (result.getRetCode() == I.MSG_REGISTER_FAIL) {
                    mUsername.requestFocus();
                    mUsername.setError(getString(R.string.register_fail));
                } else {
                    registerSuccess();
                }
            }

        }
    }

    private void registerSuccess(){
        Log.e(TAG,"registsuccess");
        if (!RegisterActivity.this.isFinishing())
            pd.dismiss();
        // save current user
        SuperWeChatHelper.getInstance().setCurrentUserName(username);
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();

        MFGT.gotoLogin(this);
        MFGT.finish(this);
    }
    private void dismiasDialog(){}
    private boolean checkInput() {
        username = mUsername.getText().toString().trim();
        usernick = mNick.getText().toString().trim();
        pwd = mPassword.getText().toString().trim();
        confirm_pwd = mConfirmPassword.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, getResources().getString(R.string.User_name_cannot_be_empty), Toast.LENGTH_SHORT).show();
            mUsername.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            mPassword.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(confirm_pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Confirm_password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            mConfirmPassword.requestFocus();
            return false;
        } else if (!pwd.equals(confirm_pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Two_input_password), Toast.LENGTH_SHORT).show();
            return false;
        }else if (TextUtils.isEmpty(usernick)){
            Toast.makeText(this, getResources().getString(R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void unRegister(){
        model.unregister(this, username, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                Toast.makeText(RegisterActivity.this,"删除账号成功",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RegisterActivity.this,"删除账号失败",Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*public void back(View view) {
        finish();
    }*/

}

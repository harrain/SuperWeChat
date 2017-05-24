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

import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.utils.EaseCommonUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.data.net.IUserModel;
import cn.ucai.superwechat.db.SuperWeChatDBManager;
import cn.ucai.superwechat.utils.MFGT;


/**
 * Login screen
 *
 */
public class LoginActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "LoginActivity";
    public static final int REQUEST_CODE_SETNICK = 1;
    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.txt_title)
    TextView txtTitle;
    private AutoCompleteTextView usernameEditText;
    private EditText passwordEditText;

    private boolean progressShow;
    private boolean autoLogin = false;
    private IUserModel model;
    private String currentUsername;
    private String currentPassword;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_login);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);
        imgBack.setVisibility(View.VISIBLE);
        txtTitle.setVisibility(View.VISIBLE);

        /*// enter the main activity if already logged in
        if (SuperWeChatHelper.getInstance().isLoggedIn()) {
            autoLogin = true;
            startActivity(new Intent(LoginActivity.this, MainActivity.class));

            return;
        }*/


        usernameEditText = (AutoCompleteTextView) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);

        // if user changed, clear the password
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordEditText.setText(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))) {
                    login(null);
                    return true;
                } else {
                    return false;
                }
            }
        });

        if (SuperWeChatHelper.getInstance().getCurrentUsernName() != null) {
            usernameEditText.setText(SuperWeChatHelper.getInstance().getCurrentUsernName());
        }
        populateAutoComplete();
    }



    @OnClick(R.id.img_back)
    public void backToFinish(){
        MFGT.finish(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MFGT.finish(this);
    }

    private void populateAutoComplete() {


        if (Build.VERSION.SDK_INT >= 14) {
            // Use ContactsContract.Profile (API 14+)
            getLoaderManager().initLoader(0, null, this);
        }
    }

    /**
     * login
     *
     * @param view
     */
    public void login(View view) {
        if (!EaseCommonUtils.isNetWorkConnected(this)) {
            Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }
        currentUsername = usernameEditText.getText().toString().trim();
        currentPassword = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(currentUsername)) {
            Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        progressShow = true;
        pd = new ProgressDialog(LoginActivity.this);
        pd.setCanceledOnTouchOutside(false);
        pd.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "EMClient.getInstance().onCancel");
                progressShow = false;
            }
        });
        pd.setMessage(getString(R.string.Is_landing));
        pd.show();

        // After logout，the DemoDB may still be accessed due to async callback, so the DemoDB will be re-opened again.
        // close it before login to make sure DemoDB not overlap
        SuperWeChatDBManager.getInstance().closeDB();

        // reset current user name before login
        SuperWeChatHelper.getInstance().setCurrentUserName(currentUsername);

        final long start = System.currentTimeMillis();
        // call login method
        Log.d(TAG, "EMClient.getInstance().login");
        EMClient.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "login: onSuccess");

                logonForSet();
                //loginToAppServer();

            }

            @Override
            public void onProgress(int progress, String status) {
                Log.d(TAG, "login: onProgress");
            }

            @Override
            public void onError(final int code, final String message) {
                Log.d(TAG, "login: onError: " + code);
                if (!progressShow) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        pd.dismiss();
                        Toast.makeText(getApplicationContext(), getString(R.string.Login_failed) + message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    //都登录成功后的环信配置
    private void logonForSet() {
        // ** manually load all local groups and conversation
        EMClient.getInstance().groupManager().loadAllGroups();
        EMClient.getInstance().chatManager().loadAllConversations();

        // update current user's display name for APNs
        boolean updatenick = EMClient.getInstance().pushManager().updatePushNickname(
                SuperWeChatApplication.currentUserNick.trim());
        if (!updatenick) {
            Log.e("LoginActivity", "update current user nick fail");
        }

        if (!LoginActivity.this.isFinishing() && pd.isShowing()) {
            pd.dismiss();
        }
        // get user's info (this should be get from App's server or 3rd party service)
        //从第三方或者自己的服务器获取用户信息
        SuperWeChatHelper.getInstance().getUserProfileManager().asyncGetCurrentAppUserInfo();

        MFGT.gotoMain(this);
        MFGT.finish(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /*private void loginToAppServer() {
=======
        SuperWeChatHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();
    }

    private void loginToAppServer() {
>>>>>>> origin/master
        model = new UserModel();
        model.login(this, currentUsername, MD5.getMessageDigest(currentPassword), new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                L.e(TAG, result);
                handleJson(result);
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    private void handleJson(String s) {
        if (s != null) {
            Result result = ResultUtils.getResultFromJson(s, User.class);
            if (result != null) {
                Log.e(TAG, result.toString());
                if (result.getRetCode() == I.MSG_LOGIN_UNKNOW_USER) {
                    usernameEditText.requestFocus();
                    usernameEditText.setError(getString(R.string.login_unkown_user));
                } else if (result.getRetCode() == I.MSG_LOGIN_ERROR_PASSWORD) {
                    passwordEditText.requestFocus();
                    passwordEditText.setError(getString(R.string.login_password_error));
                } else {
                    loginSuccess();
                }
            }

        }
    }

    private void loginSuccess() {
        logonForSet();
        MFGT.gotoMain(this);
        MFGT.finish(this);
    }*/


    /**
     * register
     *
     * @param view
     */
    public void register(View view) {
        startActivityForResult(new Intent(this, RegisterActivity.class), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (autoLogin) {
            return;
        }
    }


}

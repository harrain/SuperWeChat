package cn.ucai.superwechat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseUserUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.data.OnCompleteListener;
import cn.ucai.superwechat.data.net.IUserModel;
import cn.ucai.superwechat.data.net.UserModel;
import cn.ucai.superwechat.db.UserDao;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;

public class ProfileActivity extends BaseActivity {

    @BindView(R.id.profile_image)
    ImageView mProfileImage;
    @BindView(R.id.tv_userinfo_nick)
    TextView mTvUserinfoNick;
    @BindView(R.id.tv_userinfo_name)
    TextView mTvUserinfoName;
    @BindView(R.id.btn_add_contact)
    Button mBtnAddContact;
    @BindView(R.id.btn_send_msg)
    Button mBtnSendMsg;
    @BindView(R.id.btn_send_video)
    Button mBtnSendVideo;
    User user = null;
    private String username;
    private IUserModel model;
    private static String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle arg0) {
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        super.onCreate(arg0);
        showLeftBack();
        model = new UserModel();
        initData();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    private void initData() {
        username = getIntent().getStringExtra(I.User.USER_NAME);
        user = (User) getIntent().getSerializableExtra(I.User.TABLE_NAME);
        if (username == null && user == null){
            finish();
            return;
        }
        if (user == null) {
            user = SuperWeChatHelper.getInstance().getAppContactList().get(username);
        }
        if (user == null && username.equals(EMClient.getInstance().getCurrentUser())){
            user = SuperWeChatHelper.getInstance().getUserProfileManager().getCurrentAppUserInfo();
        }

        if (user!=null){
            showInfo();
        }else{
            showUser();
        }
        syncUserInfo();
    }

    private void syncUserInfo() {

        model.loadUserInfo(ProfileActivity.this, username, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.e(TAG,"syncUserInfo:"+s);
                boolean isSuccess = false;
                if (s != null){
                    Result<User> result = ResultUtils.getResultFromJson(s,User.class);
                    if (result != null && result.isRetMsg()){
                        isSuccess = true;
                        user = result.getRetData();
                    }
                }
                if (!isSuccess){
                    showUser();
                }else {
                    showInfo();
                    if (SuperWeChatHelper.getInstance().getAppContactList().containsKey(username)){
                        saveUser2DB();
                    }
                }
            }

            @Override
            public void onError(String error) {
                showUser();
            }
        });
    }

    private void saveUser2DB(){
        UserDao userDao = new UserDao(ProfileActivity.this);
        userDao.saveAppContact(user);
        SuperWeChatHelper.getInstance().getAppContactList().put(user.getMUserName(),user);
        sendBroadcast(new Intent(Constant.ACTION_CONTACT_CHANAGED));
    }

    private void showUser(){
        mTvUserinfoName.setText(username);
        EaseUserUtils.setAppUserNick(username, mTvUserinfoNick);
        EaseUserUtils.setAppUserAvatar(ProfileActivity.this, username, mProfileImage);
    }

    private void showInfo() {

            mTvUserinfoName.setText(user.getMUserName());
            EaseUserUtils.setAppUserNick(user.getMUserNick(), mTvUserinfoNick);
            EaseUserUtils.setAppUserAvatar(ProfileActivity.this, username, mProfileImage);
            showButton(SuperWeChatHelper.getInstance().getAppContactList().containsKey(user.getMUserName()));

    }

    private void showButton(boolean isContact) {
        if (!user.getMUserName().equals(EMClient.getInstance().getCurrentUser())) {
            mBtnAddContact.setVisibility(isContact ? View.GONE : View.VISIBLE);
            mBtnSendMsg.setVisibility(isContact ? View.VISIBLE : View.GONE);
            mBtnSendVideo.setVisibility(isContact ? View.VISIBLE : View.GONE);
        }
    }

    @OnClick(R.id.btn_add_contact)
    public void sendAddContactMsg(){
        MFGT.gotoSendMsg(ProfileActivity.this,user.getMUserName());
    }

    @OnClick(R.id.btn_send_msg)
    public void gotoChat(){
        startActivity(new Intent(this, ChatActivity.class).putExtra("userId", user.getMUserName()));
    }


    /**
     * make a video call
     */
    @OnClick(R.id.btn_send_video)
    protected void startVideoCall() {
        if (!EMClient.getInstance().isConnected())
            Toast.makeText(getApplicationContext(), R.string.not_connect_to_server, Toast.LENGTH_SHORT).show();
        else {
            startActivity(new Intent(this, VideoCallActivity.class).putExtra("username", user.getMUserName())
                    .putExtra("isComingCall", false));
            // videoCallBtn.setEnabled(false);
            //inputMenu.hideExtendMenuContainer();
        }
    }
}

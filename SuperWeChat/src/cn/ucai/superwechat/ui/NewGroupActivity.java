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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMGroupManager.EMGroupOptions;
import com.hyphenate.chat.EMGroupManager.EMGroupStyle;
import com.hyphenate.easeui.domain.Group;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.exceptions.HyphenateException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.data.OnCompleteListener;
import cn.ucai.superwechat.data.net.GroupModel;
import cn.ucai.superwechat.data.net.IGroupModel;
import cn.ucai.superwechat.utils.CommonUtils;
import cn.ucai.superwechat.utils.L;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;

public class NewGroupActivity extends BaseActivity {
    @BindView(R.id.select_galary)
    Button selectGalary;
    private EditText groupNameEditText;
    private ProgressDialog progressDialog;
    private EditText introductionEditText;
    private CheckBox publibCheckBox;
    private CheckBox memberCheckbox;
    private TextView secondTextView;

    private IGroupModel model;
    File file = null;
    private static final int REQUESTCODE_PICK = 1;
    private static final int REQUESTCODE_CUTTING = 2;
    private static String TAG = "NewGroupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.em_activity_new_group);
        ButterKnife.bind(this);
        super.onCreate(savedInstanceState);
        showLeftBack();
        groupNameEditText = (EditText) findViewById(R.id.edit_group_name);
        introductionEditText = (EditText) findViewById(R.id.edit_group_introduction);
        publibCheckBox = (CheckBox) findViewById(R.id.cb_public);
        memberCheckbox = (CheckBox) findViewById(R.id.cb_member_inviter);
        secondTextView = (TextView) findViewById(R.id.second_desc);

        publibCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    secondTextView.setText(R.string.join_need_owner_approval);
                } else {
                    secondTextView.setText(R.string.Open_group_members_invited);
                }
            }
        });
        model = new GroupModel();
        setListener();
    }

    private void setListener() {
        titleBar.getRightLayout().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
    }

    /**
     * @param
     */
    public void save() {
        String name = groupNameEditText.getText().toString();
        if (TextUtils.isEmpty(name)) {
            new EaseAlertDialog(this, R.string.Group_name_cannot_be_empty).show();
        } else {
            // select from contact list
            startActivityForResult(new Intent(this, GroupPickContactsActivity.class).putExtra("groupName", name), 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String st1 = getResources().getString(R.string.Is_to_create_a_group_chat);
        final String st2 = getResources().getString(R.string.Failed_to_create_groups);
        switch (requestCode){
            case 0:
                doGroup(resultCode,st1,data);
                break;
            case REQUESTCODE_PICK:
                if (data == null || data.getData() == null) {
                    return;
                }
                startPhotoZoom(data.getData());
                break;
            case REQUESTCODE_CUTTING:
                if (data != null) {
                    setPicToView(data);
                }
                break;
        }

    }

    private void doGroup(int resultCode, String st1, final Intent data){
        if (resultCode == RESULT_OK) {
            //new group
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(st1);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String groupName = groupNameEditText.getText().toString().trim();
                    String desc = introductionEditText.getText().toString();
                    String[] members = data.getStringArrayExtra("newmembers");
                    try {
                        EMGroupOptions option = new EMGroupOptions();
                        option.maxUsers = 200;
                        option.inviteNeedConfirm = true;

                        String reason = NewGroupActivity.this.getString(R.string.invite_join_group);
                        reason = EMClient.getInstance().getCurrentUser() + reason + groupName;

                        if (publibCheckBox.isChecked()) {
                            option.style = memberCheckbox.isChecked() ? EMGroupStyle.EMGroupStylePublicJoinNeedApproval : EMGroupStyle.EMGroupStylePublicOpenJoin;
                        } else {
                            option.style = memberCheckbox.isChecked() ? EMGroupStyle.EMGroupStylePrivateMemberCanInvite : EMGroupStyle.EMGroupStylePrivateOnlyOwnerInvite;
                        }
                        EMGroup group = EMClient.getInstance().groupManager().createGroup(groupName, desc, members, reason, option);
                        createAppGroup(group,members);

                    } catch (final HyphenateException e) {
                        Log.e(TAG,"环信异常："+e.getMessage());
                        createFaile(e);
                    }

                }
            }).start();
        }
    }

    private void createFaile(final HyphenateException e) {
        final String st2 = getResources().getString(R.string.Failed_to_create_groups);
        runOnUiThread(new Runnable() {
            public void run() {
                progressDialog.dismiss();
                if (e != null) {
                    Toast.makeText(NewGroupActivity.this, st2 + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                } else {
                    CommonUtils.showLongToast(st2);
                }
            }
        });
    }

    private void createSuccess() {
        runOnUiThread(new Runnable() {
            public void run() {
                progressDialog.dismiss();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void createAppGroup(final EMGroup group, final String[] members) {
        Log.e(TAG,"创建Appgroup：file"+file.getAbsolutePath());
        model.createGroup(NewGroupActivity.this, group.getGroupId(), group.getGroupName(), group.getDescription(),
                group.getOwner(), group.isPublic(), group.isMemberAllowToInvite(), file,
                new OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        Log.e(TAG,"createAppGroup:"+s);
                        boolean isSuccess = false;
                        if (s != null) {
                            Result<Group> result = ResultUtils.getResultFromJson(s, Group.class);
                            if (result != null && result.isRetMsg()) {
                                isSuccess = true;
                                if (members!=null && members.length > 0){
                                    addGroupMembers(members,group.getGroupId());
                                }
                            }
                        }
                        if (!isSuccess) {
                            Log.e(TAG,"createAppGroup:onSuccess.."+"fail");
                            createFaile(null);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG,"createAppGroup:onError.."+error);
                        createFaile(null);
                    }
                });
    }

    private void addGroupMembers(String[] members,String hxid) {
        StringBuilder sb = new StringBuilder();
        for (String member : members) {
            sb.append(member);
            sb.append(",");
        }
        Log.e(TAG,"addGroupMembers:usernames.."+sb.toString());
        IGroupModel groupModel = new GroupModel();
        groupModel.addGroupMembers(NewGroupActivity.this, sb.toString(), hxid, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.e(TAG,"addGroupMembers:"+s);
                boolean isSuccess = false;
                if (s != null) {
                    Result<Group> result = ResultUtils.getResultFromJson(s, Group.class);
                    if (result != null && result.isRetMsg()) {
                        isSuccess = true;
                        createSuccess();
                    }
                }
                if (!isSuccess) {
                    Log.e(TAG,"addGroupMembers:onSuccess..fail");
                    createFaile(null);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG,"addGroupMembers:onError.."+error);
                createFaile(null);
            }
        });
    }

    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("return-data", true);
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, REQUESTCODE_CUTTING);
    }

    public void setPicToView(Intent picdata) {
        Bundle extras = picdata.getExtras();
        if (extras != null) {
            Bitmap photo = extras.getParcelable("data");

            file = saveBitmapFile(photo);
            String p = file.getAbsolutePath();
            if (TextUtils.isEmpty(p)){
                Log.e(TAG,"群组头像路径为空");
            }else {
                selectGalary.setText(p);
            }
        }
    }

    private File saveBitmapFile(Bitmap bitmap) {
        if (bitmap != null) {
            String imagePath = getAvatarPath(this, I.AVATAR_TYPE_GROUP_PATH)+"/"+getAvatarName()+".jpg";
            File file = new File(imagePath);//将要保存图片的路径
            L.e("file path="+file.getAbsolutePath());
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return file;
        }
        return null;
    }

    public static String getAvatarPath(Context context, String path){
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File folder = new File(dir,path);
        if(!folder.exists()){
            folder.mkdir();
        }
        return folder.getAbsolutePath();
    }

	private String getAvatarName() {
		String avatarName = String.valueOf(System.currentTimeMillis()) ;
		return avatarName;
	}

    public void back(View view) {
        finish();
    }

    @OnClick(R.id.select_galary)
    public void onViewClicked() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(pickIntent, REQUESTCODE_PICK);
    }
}

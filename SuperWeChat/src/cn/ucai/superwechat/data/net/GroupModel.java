package cn.ucai.superwechat.data.net;

import android.content.Context;

import java.io.File;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.data.OnCompleteListener;
import cn.ucai.superwechat.utils.OkHttpUtils;

/**
 * Created by CDLX on 2017/6/2.
 */

public class GroupModel implements IGroupModel {
    @Override
    public void createGroup(Context context, String hxid, String name, String des, String owner, boolean isPublic, boolean isInviets, File file, OnCompleteListener<String> listener) {
        OkHttpUtils<String> utils = new OkHttpUtils<>(context);
        utils.setRequestUrl(I.REQUEST_CREATE_GROUP)
                .addParam(I.Group.HX_ID,hxid)
                .addParam(I.Group.NAME,name)
                .addParam(I.Group.DESCRIPTION,des)
                .addParam(I.Group.OWNER,owner)
                .addParam(I.Group.IS_PUBLIC,String.valueOf(isPublic))
                .addParam(I.Group.ALLOW_INVITES,String.valueOf(isInviets))
                .addFile2(file)
                .targetClass(String.class)
                .post()
                .execute(listener);
    }

    @Override
    public void addGroupMembers(Context context, String usernames, String hxid, OnCompleteListener<String> listener) {
        OkHttpUtils<String> utils = new OkHttpUtils<>(context);
        utils.setRequestUrl(I.REQUEST_ADD_GROUP_MEMBERS)
                .addParam(I.Member.USER_NAME,usernames)
                .addParam(I.Group.HX_ID,hxid)
                .targetClass(String.class)
                .post()
                .execute(listener);
}
}

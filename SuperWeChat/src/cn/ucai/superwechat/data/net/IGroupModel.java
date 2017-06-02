package cn.ucai.superwechat.data.net;

import android.content.Context;

import java.io.File;

import cn.ucai.superwechat.data.OnCompleteListener;

/**
 * Created by CDLX on 2017/6/2.
 */

public interface IGroupModel {
    void createGroup(Context context, String hxid, String name, String des, String owner,
                     boolean isPublic, boolean isInviets, File file, OnCompleteListener<String> listener);
}

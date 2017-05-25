package cn.ucai.superwechat.parse;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.data.OnCompleteListener;
import cn.ucai.superwechat.data.net.IUserModel;
import cn.ucai.superwechat.data.net.UserModel;
import cn.ucai.superwechat.utils.PreferenceManager;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;


public class UserProfileManager {

	/**
	 * application context
	 */
	protected Context appContext = null;

	/**
	 * init flag: test if the sdk has been inited before, we don't need to init
	 * again
	 */
	private boolean sdkInited = false;

	/**
	 * HuanXin sync contact nick and avatar listener
	 */
	private List<SuperWeChatHelper.DataSyncListener> syncContactInfosListeners;

	private boolean isSyncingContactInfosWithServer = false;

	private EaseUser currentUser;

	private User currentAppUser;

	private IUserModel model;

	public UserProfileManager() {
	}

	public synchronized boolean init(Context context) {
		if (sdkInited) {
			return true;
		}
		ParseManager.getInstance().onInit(context);
		syncContactInfosListeners = new ArrayList<SuperWeChatHelper.DataSyncListener>();
		sdkInited = true;
		model = new UserModel();
		appContext = context;
		return true;
	}

	public void addSyncContactInfoListener(SuperWeChatHelper.DataSyncListener listener) {
		if (listener == null) {
			return;
		}
		if (!syncContactInfosListeners.contains(listener)) {
			syncContactInfosListeners.add(listener);
		}
	}

	public void removeSyncContactInfoListener(SuperWeChatHelper.DataSyncListener listener) {
		if (listener == null) {
			return;
		}
		if (syncContactInfosListeners.contains(listener)) {
			syncContactInfosListeners.remove(listener);
		}
	}

	public void asyncFetchContactInfosFromServer(List<String> usernames, final EMValueCallBack<List<EaseUser>> callback) {
		if (isSyncingContactInfosWithServer) {
			return;
		}
		isSyncingContactInfosWithServer = true;
		ParseManager.getInstance().getContactInfos(usernames, new EMValueCallBack<List<EaseUser>>() {

			@Override
			public void onSuccess(List<EaseUser> value) {
				isSyncingContactInfosWithServer = false;
				// in case that logout already before server returns,we should
				// return immediately
				if (!SuperWeChatHelper.getInstance().isLoggedIn()) {
					return;
				}
				if (callback != null) {
					callback.onSuccess(value);
				}
			}

			@Override
			public void onError(int error, String errorMsg) {
				isSyncingContactInfosWithServer = false;
				if (callback != null) {
					callback.onError(error, errorMsg);
				}
			}

		});

	}

	public void notifyContactInfosSyncListener(boolean success) {
		for (SuperWeChatHelper.DataSyncListener listener : syncContactInfosListeners) {
			listener.onSyncComplete(success);
		}
	}

	public boolean isSyncingContactInfoWithServer() {
		return isSyncingContactInfosWithServer;
	}

	public synchronized void reset() {
		isSyncingContactInfosWithServer = false;
		currentUser = null;
		currentAppUser = null;

		PreferenceManager.getInstance().removeCurrentUserInfo();
	}

	public synchronized EaseUser getCurrentUserInfo() {
		if (currentUser == null) {
			String username = EMClient.getInstance().getCurrentUser();
			currentUser = new EaseUser(username);
			String nick = getCurrentUserNick();
			currentUser.setNick((nick != null) ? nick : username);
			currentUser.setAvatar(getCurrentUserAvatar());
		}
		return currentUser;
	}


	public synchronized User getCurrentAppUserInfo() {
		if (currentAppUser == null) {
			String username = EMClient.getInstance().getCurrentUser();
			currentAppUser = new User(username);
			String nick = getCurrentUserNick();
			currentAppUser.setMUserNick((nick != null) ? nick : username);
			currentAppUser.setAvatar(getCurrentUserAvatar());
		}
		return currentAppUser;
	}


	public boolean updateCurrentUserNickName(final String nickname) {
		boolean isSuccess = ParseManager.getInstance().updateParseNickName(nickname);
		if (isSuccess) {
			setCurrentUserNick(nickname);
		}
		return isSuccess;
	}

	public void updateCurrentAppUserNickName(String nickname) {

			setCurrentUserNick(nickname);

	}

	public String uploadUserAvatar(byte[] data) {
		String avatarUrl = ParseManager.getInstance().uploadParseAvatar(data);
		if (avatarUrl != null) {
			setCurrentUserAvatar(avatarUrl);
		}
		return avatarUrl;
	}

	public void uploadAppUserAvatar(File file){
		model.uploadAvatar(appContext, EMClient.getInstance().getCurrentUser(), I.AVATAR_TYPE_USER_PATH,
				file, new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						boolean isSuccess = false;
						if (s!=null){
							Result<User> result = ResultUtils.getResultFromJson(s, User.class);
							if (result!=null){
								if (result.isRetMsg()){
									User user = result.getRetData();
									if (user!=null){
										isSuccess = true;
										setCurrentAppUserAvatar(user.getAvatar());
									}
								}
							}
						}
						appContext.sendBroadcast(new Intent(I.BROADCAST_UPDATE_AVATAR)
								.putExtra(I.RESULT_UPDATE_AVATAR,isSuccess));
					}

					@Override
					public void onError(String error) {
						appContext.sendBroadcast(new Intent(I.BROADCAST_UPDATE_AVATAR)
								.putExtra(I.RESULT_UPDATE_AVATAR,false));
					}
				});
	}

	public void asyncGetCurrentUserInfo() {
		ParseManager.getInstance().asyncGetCurrentUserInfo(new EMValueCallBack<EaseUser>() {

			@Override
			public void onSuccess(EaseUser value) {
			    if(value != null){
    				setCurrentUserNick(value.getNick());
    				setCurrentUserAvatar(value.getAvatar());
			    }
			}

			@Override
			public void onError(int error, String errorMsg) {

			}
		});

	}


	public void asyncGetCurrentAppUserInfo(){
		model.loadUserInfo(appContext, EMClient.getInstance().getCurrentUser(), new OnCompleteListener<String>() {
			@Override
			public void onSuccess(String s) {
				if (s != null){
					Log.e("UPM",s);
					Result<User> result = ResultUtils.getResultFromJson(s,User.class);
					if (result != null && result.isRetMsg()){
						User user = result.getRetData();
						if (user != null){
							setCurrentAppUserNick(user.getMUserNick());
							setCurrentAppUserAvatar(user.getAvatar());
						}
					}
				}
			}

			@Override
			public void onError(String error) {

			}
		});
	}


	public void asyncGetUserInfo(final String username,final EMValueCallBack<EaseUser> callback){
		ParseManager.getInstance().asyncGetUserInfo(username, callback);
	}
	private void setCurrentUserNick(String nickname) {
		getCurrentAppUserInfo().setMUserNick(nickname);
		PreferenceManager.getInstance().setCurrentUserNick(nickname);
	}

	private void setCurrentUserAvatar(String avatar) {
		getCurrentUserInfo().setAvatar(avatar);
		PreferenceManager.getInstance().setCurrentUserAvatar(avatar);
	}


	private void setCurrentAppUserNick(String nickname) {
		getCurrentAppUserInfo().setMUserNick(nickname);
		PreferenceManager.getInstance().setCurrentUserNick(nickname);
	}

	private void setCurrentAppUserAvatar(String avatar) {
		getCurrentAppUserInfo().setAvatar(avatar);
		PreferenceManager.getInstance().setCurrentUserAvatar(avatar);
	}


	private String getCurrentUserNick() {
		return PreferenceManager.getInstance().getCurrentUserNick();
	}

	private String getCurrentUserAvatar() {
		return PreferenceManager.getInstance().getCurrentUserAvatar();
	}

}

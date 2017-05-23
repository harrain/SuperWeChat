package cn.ucai.superwechat.model;

/**
 * Created by CDLX on 2017/5/19.
 */

public class RegisterInfo {

    /**
     * retCode : 0
     * retMsg : true
     * retData : null
     */

    private int retCode;
    private boolean retMsg;
    private Object retData;

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public boolean isRetMsg() {
        return retMsg;
    }

    public void setRetMsg(boolean retMsg) {
        this.retMsg = retMsg;
    }

    public Object getRetData() {
        return retData;
    }

    public void setRetData(Object retData) {
        this.retData = retData;
    }
}

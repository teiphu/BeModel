package android.bemodel.com.bemodel.bean;

import cn.bmob.v3.BmobObject;

/**
 * Created by Administrator on 2017.07.24.
 */

public class MessagesInfo extends BmobObject {

    private UserInfo user;

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }
}
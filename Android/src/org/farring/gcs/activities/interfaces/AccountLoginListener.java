package org.farring.gcs.activities.interfaces;

public interface AccountLoginListener {
    // 登陆成功
    void onLogin();

    // 登陆失败
    void onFailedLogin();

    // 退出登陆
    void onLogout();
}

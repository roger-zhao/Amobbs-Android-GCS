package com.evenbus;

/**
 * Created by LinJieqiang on 2015/10/5.
 */
public enum ActionEvent {

    /**
     * Action used to broadcast updates to the period for the spoken status summary.
     */
    ACTION_UPDATED_STATUS_PERIOD,

    /**
     * Action used to broadcast updates to the gps hdop display preference.
     */
    ACTION_PREF_HDOP_UPDATE,

    /**
     * Action used to broadcast updates to the unit system.
     */
    ACTION_PREF_UNIT_SYSTEM_UPDATE,
    ACTION_LOCATION_SETTINGS_UPDATED,
    ACTION_ADVANCED_MENU_UPDATED,

    /**
     * Used to notify of an update to the map rotation preference.
     */
    ACTION_MAP_ROTATION_PREFERENCE_UPDATED,
    ACTION_WIDGET_PREFERENCE_UPDATED,

    // 定义动作“更新任务”
    ACTION_MISSION_PROXY_UPDATE,

    // “return to me”更新
    ACTION_PREF_RETURN_TO_ME_UPDATED,

    /**
     * Action used for message to be delivered by the tts speech engine.
     */
    ACTION_SPEAK_MESSAGE,

    ACTION_UPDATE_MAP,

    // 【用户事件更新】
    ACTION_UPDATE_USER,
    // 获取路线通知
    ACTION_RECEIVE_MSG,
    // 设置路线通知
    ACTION_GET_LATLON,

    // 更新语音选项
    ACTION_UPDATE_VOICE,

    // 飞机是否面临坠毁？
    ACTION_GROUND_COLLISION_IMMINENT,

    // 日志保存成功
    ACTION_TLOG_SAVE_SUCCESS,

    // tile未翻墙
    ACTION_GOOGLE_TILE_ERROR,
}

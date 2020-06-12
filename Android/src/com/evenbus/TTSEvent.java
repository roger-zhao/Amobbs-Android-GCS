package com.evenbus;

/**
 * Created by LinJieqiang on 2016/1/19.
 */
public class TTSEvent {

    // 语音的类型
    private TTSEvents type;
    // 语音的内容
    private String contents;

    public TTSEvent(TTSEvents type, String contents) {
        this.type = type;
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }

    public TTSEvents getType() {
        return type;
    }

    public enum TTSEvents {
        TTS_CALIBRATION_IMU,
        TTS_MSG_ERR,
    }
}

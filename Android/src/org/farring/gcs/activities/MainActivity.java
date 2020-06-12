package org.farring.gcs.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 作者： 林杰强
 * 日期： 2016/3/2 21:47.
 * 备注:
 */
public class MainActivity extends SuperUI {

    @BindView(R.id.app_VersionId)
    TextView appVersionId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ButterKnife.bind(this);
        // 设置版本号
        appVersionId.setText(Utils.getVersion(this));

        // 权限获取完成，进入应用，否则将会卡屏！
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startActivity(new Intent(MainActivity.this, EditorActivity.class));
                finish();
            }
        }, 1000);
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }
}

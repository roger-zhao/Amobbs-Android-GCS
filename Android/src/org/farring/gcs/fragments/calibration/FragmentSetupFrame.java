package org.farring.gcs.fragments.calibration;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.dronekit.core.drone.DroneInterfaces;
import com.dronekit.core.drone.profiles.ParameterManager;
import com.dronekit.core.drone.property.Parameter;

import org.farring.gcs.R;
import org.farring.gcs.fragments.helpers.BaseFragment;


public class FragmentSetupFrame extends BaseFragment implements DroneInterfaces.OnParameterManagerListener{

    private ParameterManager parameterManager;
    private Parameter frameType, frameClass, frameParam;
    private Button frameClassQuadBtn;
    private Button frameClassHexaBtn;
    private Button frameClassOctaBtn;
    private Button frameTypePlusBtn;
    private Button frameTypeXBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_frame, container, false);
        super.onViewCreated(view, savedInstanceState);
//
        final Context context = getContext();

        parameterManager = getDrone().getParameterManager();

        frameClassQuadBtn = (Button) view.findViewById(R.id.frameClassQuadBtn);
        frameClassQuadBtn.setEnabled(true);
        frameClassQuadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSetFrame(v);
            }
        });

        frameClassHexaBtn = (Button) view.findViewById(R.id.frameClassHexaBtn);
        frameClassHexaBtn.setEnabled(true);
        frameClassHexaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSetFrame(v);
            }
        });

        frameClassOctaBtn = (Button) view.findViewById(R.id.frameClassOctaBtn);
        frameClassOctaBtn.setEnabled(true);
        frameClassOctaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSetFrame(v);
            }
        });

        frameTypeXBtn = (Button) view.findViewById(R.id.frameTypeXBtn);
        frameTypeXBtn.setEnabled(true);
        frameTypeXBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSetFrame(v);
            }
        });

        frameTypePlusBtn = (Button) view.findViewById(R.id.frameTypePlusBtn);
        frameTypePlusBtn.setEnabled(true);
        frameTypePlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSetFrame(v);
            }
        });


        if (getDrone().isConnected()) {
            frameClass = parameterManager.getParameter("FRAME_CLASS");
            int fClass = (int)frameClass.getValue();
            switch (fClass)
            {
                case 1:
                    frameClassQuadBtn.setBackgroundColor(Color.GREEN);
                    frameClassHexaBtn.setBackgroundColor(Color.GRAY);
                    frameClassOctaBtn.setBackgroundColor(Color.GRAY);
                    break;
                case 2:
                    frameClassQuadBtn.setBackgroundColor(Color.GRAY);
                    frameClassHexaBtn.setBackgroundColor(Color.GREEN);
                    frameClassOctaBtn.setBackgroundColor(Color.GRAY);
                    break;
                case 3:
                    frameClassQuadBtn.setBackgroundColor(Color.GRAY);
                    frameClassHexaBtn.setBackgroundColor(Color.GRAY);
                    frameClassOctaBtn.setBackgroundColor(Color.GREEN);
                    break;
                case 0:
                default:
                    frameClassQuadBtn.setBackgroundColor(Color.GRAY);
                    frameClassHexaBtn.setBackgroundColor(Color.GRAY);
                    frameClassOctaBtn.setBackgroundColor(Color.GRAY);
                    break;
            }

            frameType = parameterManager.getParameter("FRAME_TYPE");
            int fType = (int)frameType.getValue();
            switch (fType)
            {
                case 0:
                    frameTypePlusBtn.setBackgroundColor(Color.GREEN);
                    frameTypeXBtn.setBackgroundColor(Color.GRAY);
                    break;
                case 1:
                    frameTypePlusBtn.setBackgroundColor(Color.GRAY);
                    frameTypeXBtn.setBackgroundColor(Color.GREEN);
                    break;
                default:
                    frameTypePlusBtn.setBackgroundColor(Color.GRAY);
                    frameTypeXBtn.setBackgroundColor(Color.GRAY);
                    break;
            }
        }

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        // 添加参数对象监听器
        parameterManager.setParameterListener(this);
        // 打开遥控数据流
    }
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onBeginReceivingParameters() {

    }

    @Override
    public void onParameterReceived(Parameter parameter, int index, int count) {

    }

    @Override
    public void onEndReceivingParameters() {

    }

    public void doSetFrame(View v) {
        int val = 0;
        String frame_item = "";
        switch (v.getId()) {
            case R.id.frameClassQuadBtn:
                val = 1;
                frame_item = "FRAME_CLASS";
                frameClassHexaBtn.setBackgroundColor(Color.GRAY);
                frameClassOctaBtn.setBackgroundColor(Color.GRAY);
                break;
            case R.id.frameClassHexaBtn:
                val = 2;
                frame_item = "FRAME_CLASS";
                frameClassQuadBtn.setBackgroundColor(Color.GRAY);
                frameClassOctaBtn.setBackgroundColor(Color.GRAY);
                break;
            case R.id.frameClassOctaBtn:
                val = 3;
                frame_item = "FRAME_CLASS";
                frameClassQuadBtn.setBackgroundColor(Color.GRAY);
                frameClassHexaBtn.setBackgroundColor(Color.GRAY);
                break;
            case R.id.frameTypePlusBtn:
                val = 0;
                frame_item = "FRAME_TYPE";
                frameTypeXBtn.setBackgroundColor(Color.GRAY);
                break;
            case R.id.frameTypeXBtn:
                val = 1;
                frame_item = "FRAME_TYPE";
                frameTypePlusBtn.setBackgroundColor(Color.GRAY);
                 break;
        }
        if(getDrone().isConnected())
        {
            frameParam = parameterManager.getParameter(frame_item);
            frameParam.setValue(val);
            parameterManager.sendParameter(frameParam);
            v.setBackgroundColor(Color.GREEN);
        }
        else
        {
            Toast.makeText(getActivity(), "飞控未连接，无法执行操作", Toast.LENGTH_SHORT).show();
        }
    }


}

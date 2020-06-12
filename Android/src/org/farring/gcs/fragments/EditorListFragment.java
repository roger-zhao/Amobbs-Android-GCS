package org.farring.gcs.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.evenbus.ActionEvent;

import org.greenrobot.eventbus.Subscribe;
import org.farring.gcs.R;
import org.farring.gcs.activities.interfaces.OnEditorInteraction;
import org.farring.gcs.fragments.helpers.BaseFragment;
import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.proxy.mission.MissionSelection.OnSelectionUpdateListener;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;
import org.farring.gcs.utils.ReorderRecyclerView;
import org.farring.gcs.view.adapterViews.MissionItemListAdapter;

import java.util.List;

public class EditorListFragment extends BaseFragment implements OnSelectionUpdateListener {
    private MissionProxy missionProxy;
    private OnEditorInteraction editorListener;
    private ReorderRecyclerView recyclerView;
    private ReorderRecyclerView.Adapter recyclerAdapter;

    @Subscribe
    public void onReceiveActionEvent(ActionEvent actionEvent) {
        super.onReceiveActionEvent(actionEvent);
        switch (actionEvent) {
            case ACTION_MISSION_PROXY_UPDATE:
                recyclerAdapter.notifyDataSetChanged();
                updateViewVisibility();
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof OnEditorInteraction)) {
            throw new IllegalStateException("Parent activity must implement " + OnEditorInteraction.class.getName());
        }

        editorListener = (OnEditorInteraction) (activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editor_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = (ReorderRecyclerView) view.findViewById(R.id.mission_item_recycler_view);

        // use this setting to improve performance if you know that changes in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        //use a linear layout manager
        final RecyclerView.LayoutManager recyclerLayoutMgr = new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(recyclerLayoutMgr);

    }

    public void enableDeleteMode(boolean isEnabled) {
        if (isEnabled)
//            recyclerView.setBackgroundResource(android.R.color.holo_red_light);
        recyclerView.setBackgroundResource(R.color.editor_bar);
        else
            recyclerView.setBackgroundResource(R.color.editor_bar);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateViewVisibility();

        missionProxy = getMissionProxy();

        recyclerAdapter = new MissionItemListAdapter(getContext(), missionProxy, editorListener);
        recyclerView.setAdapter(recyclerAdapter);

        missionProxy.selection.addSelectionUpdateListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (missionProxy != null)
            missionProxy.selection.removeSelectionUpdateListener(this);
    }

    /**
     * Updates the fragment view visibility based on the count of stored mission items.
     */
    public void updateViewVisibility() {
        View view = getView();
        if (recyclerAdapter != null && view != null) {
            if (recyclerAdapter.getItemCount() > 0)
                view.setVisibility(View.VISIBLE);
            else
                view.setVisibility(View.INVISIBLE);
            editorListener.onListVisibilityChanged();
        }
    }

    @Override
    public void onSelectionUpdate(List<MissionItemProxy> selected) {
        recyclerAdapter.notifyDataSetChanged();
    }
}

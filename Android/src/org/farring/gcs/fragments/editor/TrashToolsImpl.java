package org.farring.gcs.fragments.editor;

import android.view.View;

import org.farring.gcs.R;
import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

import java.util.List;

/**
 * Created by Fredia Huya-Kouadio on 8/25/15.
 */
class TrashToolsImpl extends EditorToolsImpl implements View.OnClickListener {

    TrashToolsImpl(EditorToolsFragment fragment) {
        super(fragment);
    }

    @Override
    public void onListItemClick(MissionItemProxy item) {
        if (missionProxy == null)
            return;


        missionProxy.selection.clearSelection();
        missionProxy.removeItem(item);

        if (missionProxy.getItems().size() <= 0) {
            editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
        }
    }

    @Override
    public void onSelectionUpdate(List<MissionItemProxy> selected) {
        super.onSelectionUpdate(selected);
        editorToolsFragment.clearSelected.setEnabled(!selected.isEmpty());
    }

    @Override
    public EditorToolsFragment.EditorTools getEditorTools() {
        return EditorToolsFragment.EditorTools.TRASH;
    }

    @Override
    public void setup() {
//        EditorToolsFragment.EditorToolListener listener = editorToolsFragment.listener;
//        if (listener != null) {
//            listener.enableGestureDetection(false);
//            listener.skipMarkerClickEvents(false);
//        }
//
//        if (missionProxy != null) {
//            List<MissionItemProxy> selected = missionProxy.selection.getSelected();
//            editorToolsFragment.clearSelected.setEnabled(!selected.isEmpty());
//
//            final List<MissionItemProxy> missionItems = missionProxy.getItems();
//            editorToolsFragment.clearMission.setEnabled(!missionItems.isEmpty());
//        }
        ( (SuperUI)editorToolsFragment.getActivity()).downloadMission();


    }

//    private void doClearMissionConfirmation() {
//        if (missionProxy == null || missionProxy.getItems().isEmpty())
//            return;
//
//        final Context context = editorToolsFragment.getContext();
//        new MaterialDialog.Builder(context)
//                .iconRes(R.drawable.ic_launcher)
//                .limitIconToDefaultSize() // limits the displayed icon size to 48dp
//                .title(context.getString(R.string.dlg_clear_mission_title))
//                .content(context.getString(R.string.dlg_clear_mission_confirm))
//                .positiveText(context.getString(android.R.string.yes))
//                .negativeText(context.getString(android.R.string.no))
//                .onPositive(new MaterialDialog.SingleButtonCallback() {
//                    @Override
//                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                        if (missionProxy != null) {
//                            missionProxy.clear();
//                            editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
//                        }
//                    }
//                })
//                .show();
//    }
//
//    private void deleteSelectedItems() {
//        final Context context = editorToolsFragment.getContext();
//        new MaterialDialog.Builder(context)
//                .iconRes(R.drawable.ic_launcher)
//                .limitIconToDefaultSize() // limits the displayed icon size to 48dp
//                .title(context.getString(R.string.delete_selected_waypoints_title))
//                .content(context.getString(R.string.delete_selected_waypoints_confirm))
//                .positiveText(context.getString(android.R.string.yes))
//                .negativeText(context.getString(android.R.string.no))
//                .onPositive(new MaterialDialog.SingleButtonCallback() {
//                    @Override
//                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                        if (missionProxy != null) {
//                            missionProxy.removeSelection(missionProxy.selection);
//                            if (missionProxy.selection.getSelected().isEmpty())
//                                editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
//                        }
//                    }
//                })
//                .onNegative(new MaterialDialog.SingleButtonCallback() {
//                    @Override
//                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                        if (missionProxy != null)
//                            missionProxy.selection.clearSelection();
//                    }
//                })
//                .show();
//    }
//
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear_mission_button:
//                doClearMissionConfirmation();
                break;

            case R.id.clear_selected_button:
//                deleteSelectedItems();
                break;
        }
    }
}

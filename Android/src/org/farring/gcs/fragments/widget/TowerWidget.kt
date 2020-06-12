package org.farring.gcs.fragments.widget

import org.farring.gcs.fragments.helpers.BaseFragment

abstract class TowerWidget : BaseFragment() {

    abstract fun getWidgetType(): TowerWidgets
}
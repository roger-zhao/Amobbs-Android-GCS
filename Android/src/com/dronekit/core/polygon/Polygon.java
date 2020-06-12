package com.dronekit.core.polygon;

import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.geoTools.GeoTools;
import com.dronekit.core.helpers.geoTools.LineLatLong;
import com.dronekit.core.helpers.units.Area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 多边形
public class Polygon {

    // 组成多边形一系列的点
    private List<LatLong> points = new ArrayList<LatLong>();

    // 添加集合航点
    public void addPoints(List<LatLong> pointList) {
        for (LatLong point : pointList) {
            addPoint(point);
        }
    }

    // 添加单个航点
    public void addPoint(LatLong coord) {
        points.add(coord);
    }

    // 清除多边形
    public void clearPolygon() {
        points.clear();
    }

    // 获取多边形组成的航点集合
    public List<LatLong> getPoints() {
        return points;
    }

    // 获取线
    public List<LineLatLong> getLines() {
        List<LineLatLong> list = new ArrayList<LineLatLong>();
        for (int i = 0; i < points.size(); i++) {
            int endIndex = (i == 0) ? points.size() - 1 : i - 1;
            list.add(new LineLatLong(points.get(i), points.get(endIndex)));
        }
        return list;
    }

    public void movePoint(LatLong coord, int number) {
        points.get(number).set(coord);
    }

    public Area getArea() {
        return GeoTools.getArea(this);
    }

    public void checkIfValid() throws Exception {
        if (points.size() < 3) {
            throw new InvalidPolygon(points.size());
        }
    }

    public void reversePoints() {
        Collections.reverse(points);
    }

    public class InvalidPolygon extends Exception {
        private static final long serialVersionUID = 1L;
        public int size;

        public InvalidPolygon(int size) {
            this.size = size;
        }
    }
}

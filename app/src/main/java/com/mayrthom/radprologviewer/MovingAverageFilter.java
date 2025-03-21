package com.mayrthom.radprologviewer;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MovingAverageFilter {
    private final int windowSize;
        private final Queue<Float> window;
        private float sum;

        public MovingAverageFilter(int size) {
            this.windowSize = size;
            this.window = new LinkedList<>();
            this.sum = 0.0f;
        }

        private float addDataPoint(float radiation) {
            window.add(radiation);
            sum += radiation;

            if (window.size() > windowSize) {
                sum -= window.poll();
            }

            return sum / window.size();
        }

        public List<Entry> applyFilter(List<Entry> entries) {
            List<Entry> smoothedList = new ArrayList<>();

            for (Entry entry : entries) {
                float yFiltered = this.addDataPoint(entry.getY());
                smoothedList.add(new Entry(entry.getX(), yFiltered));
            }

            return smoothedList;
        }
}

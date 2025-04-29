package com.mayrthom.radprologviewer.ui;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.database.DataList;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PlotFragment extends androidx.fragment.app.Fragment {
    private LineChart chart;
    private boolean grid = true;
    private boolean circles = false;
    private boolean filterOn = false;
    private SeekBar seekBar;
    private TextView filterText;
    private enum Unit {CPM, CPS, SIEVERT}
    private Unit unit = Unit.CPM;
    private DataList dataList;
    private int filterWindow = 0;
    private final int filterMax = 100;
    private int lineColor, backcolor, textcolor;
    SharedViewModel sharedViewModel;
    boolean store = false, stored = false;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;


    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_plot,container, false);
        loadPreferences();
        if (getArguments() != null) {
            store = getArguments().getBoolean("store");
        }
        SharedViewModelFactory factory = new SharedViewModelFactory(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);
        sharedViewModel.getDataList().observe(getViewLifecycleOwner(), d -> {
            if (d != null) {
                dataList = d;
                seekBar.setMax(Math.min(d.size(), filterMax));
                updateFilter();
                setData();
            }});

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_plot, menu);
                if(store && !dataList.isEmpty())
                    menu.findItem(R.id.store).setVisible(true);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                // Handle menu item clicks
                int id = item.getItemId();

                if(id == R.id.circles)
                    toggleCircles();
                else if(id == R.id.grid)
                    toggleGrid();
                else if(id == R.id.unit)
                    showSetUnitDialog();
                else if(id == R.id.filter)
                {
                    filterOn = !filterOn;
                    editor.putBoolean("filterOn", filterOn);
                    editor.apply();
                    updateFilter();
                    setData();
                }
                else if(id == R.id.store)
                {
                    LoadingDialog dialog = new LoadingDialog(requireContext(),"Storing Data");
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                    sharedViewModel.addDatalogWithEntries(dataList);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        stored = true;
                        Toast.makeText(requireContext(), "Datalog stored!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        });
                    });

                }
                requireActivity().invalidateOptionsMenu(); // reload the menu
                chart.notifyDataSetChanged();
                chart.invalidate();
                return false;
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                menu.findItem(R.id.filter).setChecked(filterOn);
                menu.findItem(R.id.store).setChecked(stored);
                menu.findItem(R.id.store).setEnabled(!stored);
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        seekBar = view.findViewById(R.id.seekBar1);
        filterText = view.findViewById(R.id.filterText);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                filterText.setText("Filtersize: " + progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                filterWindow = seekBar.getProgress();
                setData();
            }
        });


        /*get the colors according to the current theme*/
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        lineColor = typedValue.data;
        requireContext().getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        backcolor = typedValue.data;
        requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        textcolor = requireContext().getColorStateList(typedValue.resourceId).getDefaultColor();

        chart = view.findViewById(R.id.chart1);
        chart.getDescription().setEnabled(false);

        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        // set an alternative background color
        chart.setBackgroundColor(backcolor);
        chart.setNoDataText("No Data Available");

        Legend l = chart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(grid);
        xAxis.setTextColor(textcolor);
        xAxis.setTextSize(12f);
        xAxis.setLabelRotationAngle(35f);
        xAxis.setCenterAxisLabels(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setDrawGridLines(grid);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setGranularityEnabled(false);

        leftAxis.setTextColor(lineColor);
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(-1);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        return view;
    }

    /* Navigating back to the last fragment */
    @Override
    public void onResume() {
        super.onResume();
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Plot");
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().popBackStack();
            }
        });
    }



    private void setData() {
        //get Entryset filtered or unfiltered according to the menu option
        List<Entry> entries = toggleUnit(filterOn ? dataList.getFilteredEntrySet(filterWindow) : dataList.getEntrySet() ,unit);
        //now set data

        if(!entries.isEmpty()) {
            LineDataSet set = new LineDataSet(entries, "Radiation");
            set.setAxisDependency(AxisDependency.LEFT);
            set.setColor(lineColor);
            set.setLineWidth(2f);
            set.setDrawValues(false);
            set.setCircleColor(lineColor);
            set.setCircleHoleColor(lineColor);
            set.setDrawCircles(circles);
            LineData lineData = new LineData(set);
            /* Format the time Axis*/
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new ValueFormatter() {
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                final ZoneId zoneId = ZoneId.systemDefault();
                @Override
                public String getFormattedValue(float value) {
                    ZonedDateTime zonedDateTime = Instant.ofEpochSecond(( (long) value + dataList.getStartPoint())).atZone(zoneId);
                    return zonedDateTime.format(formatter);
                }
            });

            YAxis yAxis = chart.getAxisLeft();
            yAxis.setAxisMinimum(getMaxVal(entries)* -0.05f);
            yAxis.setAxisMaximum(getMaxVal(entries)*1.1f);

            // Format the legend of the Y Axis according to the unit
            yAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    switch (unit) {
                        case SIEVERT:
                            return String.format(Locale.US, "%.2fμSv/h", value);
                        case CPM:
                            return String.format(Locale.US,"%.1fCPM", value);
                        case CPS:
                        default:
                            return String.format(Locale.US,"%.2fCPS", value);
                    }
                }
            });
            /* Plot the data */
            chart.setData(lineData);
            new Handler(Looper.getMainLooper()).post(() -> //update the chart after the data is inserted
            {
                chart.notifyDataSetChanged();
                chart.requestLayout();
                chart.invalidate();
            });
        }
    }

    private void toggleCircles()
    {
        LineDataSet lineDataSet0 = (LineDataSet) chart.getLineData().getDataSetByIndex(0);
        circles = !circles;
        editor.putBoolean("circles",circles);
        editor.apply();
        lineDataSet0.setDrawCircles(circles);

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void toggleGrid()
    {
        grid = !grid;
        editor.putBoolean("grid",grid);
        editor.apply();

        chart.getXAxis().setDrawGridLines(grid);
        chart.getAxisLeft().setDrawGridLines(grid);

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private float getMaxVal(List<Entry> entries)
    {
        return entries.stream()
                .map(Entry::getY)
                .max(Float::compare)
                .orElse(Float.NaN);
    }

    private List<Entry> toggleUnit(List<Entry> entries, Unit unit)
    {
        switch(unit)
        {
            case CPM:
                return entries.stream()
                        .map(entry -> new Entry(entry.getX(), entry.getY() * 60 ))
                        .collect(Collectors.toList());
            case SIEVERT:
                return entries.stream()
                        .map(entry -> new Entry(entry.getX(), entry.getY() * (60)/dataList.getConversionFactor() ))
                        .collect(Collectors.toList());
            case CPS:
            default:
                return entries;
        }
    }
    private void updateFilter()
    {
        if(filterWindow == 0) {
            filterWindow = seekBar.getMax() / 2;
            seekBar.setProgress(filterWindow);
        }

        if (filterOn) {
            seekBar.setVisibility(View.VISIBLE);
            filterText.setVisibility(View.VISIBLE);
        }

        else {
            seekBar.setVisibility(View.GONE);
            filterText.setVisibility(View.GONE);
        }
    }

    private void showSetUnitDialog() {
        final String[] units = {"CPM", "CPS", "μSv/h", };

        new AlertDialog.Builder(getContext())
                .setTitle("Choose Unit")
                .setItems(units, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int num) {
                        unit = Unit.values()[num];
                        editor.putInt("unit", num);
                        editor.apply();
                        setData();
                    }
                })
                .show();
    }

    void loadPreferences()
    {
        sharedPreferences = requireContext().getSharedPreferences("plotSettings", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        circles = sharedPreferences.getBoolean("circles", false);
        grid = sharedPreferences.getBoolean("grid", true);
        unit = Unit.values()[sharedPreferences.getInt("unit",Unit.CPM.ordinal())];
        filterOn = sharedPreferences.getBoolean("filterOn", false);
    }


}
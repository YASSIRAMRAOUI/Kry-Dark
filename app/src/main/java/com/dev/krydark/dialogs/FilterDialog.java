package com.dev.krydark.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.dev.krydark.R;
import com.dev.krydark.utils.PropertyFilter;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterDialog extends DialogFragment {
    private RangeSlider priceRangeSlider;
    private AutoCompleteTextView locationFilter;
    private Spinner bedroomsSpinner, bathroomsSpinner;
    private Button btnApply, btnReset;

    private PropertyFilter currentFilter;
    private FilterListener listener;

    public interface FilterListener {
        void onFilterApplied(PropertyFilter filter);
        void onFilterReset();
    }

    public FilterDialog(PropertyFilter currentFilter) {
        this.currentFilter = currentFilter;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (FilterListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment() + " must implement FilterListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_filter, null);

        // Initialize views
        priceRangeSlider = view.findViewById(R.id.price_range_slider);
        locationFilter = view.findViewById(R.id.location_filter);
        bedroomsSpinner = view.findViewById(R.id.spinner_bedrooms);
        bathroomsSpinner = view.findViewById(R.id.spinner_bathrooms);
        btnApply = view.findViewById(R.id.btn_apply);
        btnReset = view.findViewById(R.id.btn_reset);

        // Setup price range slider
        priceRangeSlider.setValueFrom(0);
        priceRangeSlider.setValueTo(1000000);

        if (currentFilter != null) {
            priceRangeSlider.setValues(
                    (float) currentFilter.getMinPrice(),
                    (float) currentFilter.getMaxPrice()
            );
        } else {
            priceRangeSlider.setValues(0f, 1000000f);
        }

        // Setup location filter
        setupLocationDropdown();

        // Setup bedroom and bathroom spinners
        setupSpinners();

        // Set click listeners
        btnApply.setOnClickListener(v -> applyFilter());
        btnReset.setOnClickListener(v -> resetFilter());

        builder.setView(view)
                .setTitle("Filter Properties");

        return builder.create();
    }

    private void setupLocationDropdown() {
        // This would ideally be populated from the database
        String[] locations = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, locations);
        locationFilter.setAdapter(adapter);

        if (currentFilter != null && currentFilter.getLocation() != null) {
            locationFilter.setText(currentFilter.getLocation(), false);
        }
    }

    private void setupSpinners() {
        // Setup bedroom spinner
        List<String> bedroomOptions = new ArrayList<>(Arrays.asList("Any", "1", "2", "3", "4", "5+"));
        ArrayAdapter<String> bedroomAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, bedroomOptions);
        bedroomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bedroomsSpinner.setAdapter(bedroomAdapter);

        // Setup bathroom spinner
        List<String> bathroomOptions = new ArrayList<>(Arrays.asList("Any", "1", "2", "3", "4", "5+"));
        ArrayAdapter<String> bathroomAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, bathroomOptions);
        bathroomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bathroomsSpinner.setAdapter(bathroomAdapter);

        // Set current values if filter exists
        if (currentFilter != null) {
            int bedroomCount = currentFilter.getBedroomCount();
            if (bedroomCount > 0) {
                if (bedroomCount >= 5) {
                    bedroomsSpinner.setSelection(5);
                } else {
                    bedroomsSpinner.setSelection(bedroomCount);
                }
            }

            int bathroomCount = currentFilter.getBathroomCount();
            if (bathroomCount > 0) {
                if (bathroomCount >= 5) {
                    bathroomsSpinner.setSelection(5);
                } else {
                    bathroomsSpinner.setSelection(bathroomCount);
                }
            }
        }
    }

    private void applyFilter() {
        List<Float> priceValues = priceRangeSlider.getValues();
        double minPrice = priceValues.get(0);
        double maxPrice = priceValues.get(1);

        String location = locationFilter.getText().toString();

        int bedroomCount = 0;
        if (!bedroomsSpinner.getSelectedItem().toString().equals("Any")) {
            String bedroomValue = bedroomsSpinner.getSelectedItem().toString();
            if (bedroomValue.endsWith("+")) {
                bedroomCount = Integer.parseInt(bedroomValue.substring(0, bedroomValue.length() - 1));
            } else {
                bedroomCount = Integer.parseInt(bedroomValue);
            }
        }

        int bathroomCount = 0;
        if (!bathroomsSpinner.getSelectedItem().toString().equals("Any")) {
            String bathroomValue = bathroomsSpinner.getSelectedItem().toString();
            if (bathroomValue.endsWith("+")) {
                bathroomCount = Integer.parseInt(bathroomValue.substring(0, bathroomValue.length() - 1));
            } else {
                bathroomCount = Integer.parseInt(bathroomValue);
            }
        }

        PropertyFilter filter = new PropertyFilter(minPrice, maxPrice, location, bedroomCount, bathroomCount);

        if (listener != null) {
            listener.onFilterApplied(filter);
        }

        dismiss();
    }

    private void resetFilter() {
        if (listener != null) {
            listener.onFilterReset();
        }

        dismiss();
    }
}
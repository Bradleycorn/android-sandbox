package net.bradball.android.sandbox;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends SingleFragmentActivity implements SandboxFragment.Callbacks {

    private Spinner mToolbarSpinner;

    public Fragment createFragment() {
        return SandboxFragment.newInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbarSpinner();

    }

    @Override
    public String getSelectedYear() {
        return mToolbarSpinner.getSelectedItem().toString();
    }

    private void setupToolbarSpinner() {
        Toolbar toolbar = getActionBarToolbar();

        View spinnerContainer = LayoutInflater.from(this).inflate(R.layout.toolbar_spinner, toolbar, false);
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        toolbar.addView(spinnerContainer, lp);

        final YearSpinnerAdapter spinnerAdapter = new YearSpinnerAdapter();

        mToolbarSpinner = (Spinner) spinnerContainer.findViewById(R.id.toolbar_spinner);
        mToolbarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SandboxFragment fragment = (SandboxFragment) getFragment();

                fragment.setYear(spinnerAdapter.getItem(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mToolbarSpinner.setAdapter(spinnerAdapter);
    }

    private class YearSpinnerAdapter extends BaseAdapter {
        private List<String> mItems = new ArrayList<String>();


        public YearSpinnerAdapter() {
            for (int i=1995; i>=1965; i--) {
                mItems.add(Integer.toString(i));
            }
        }

        public void addItems(List<String> years) {
            mItems.addAll(years);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public String getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup parent) {
            if (view == null || !view.getTag().toString().equals("DROPDOWN")) {
                view = getLayoutInflater().inflate(R.layout.toolbar_spinner_item_dropdown, parent, false);
                view.setTag("DROPDOWN");
            }

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(mItems.get(position));

            return view;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null || !view.getTag().toString().equals("NON_DROPDOWN")) {
                view = getLayoutInflater().inflate(R.layout.toolbar_spinner_item_actionbar, parent, false);
                view.setTag("NON_DROPDOWN");
            }
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(getTitle(position));
            return view;
        }

        private String getTitle(int position) {
            return position >= 0 && position < mItems.size() ? getString(R.string.toolbar_browse_spinner_title, mItems.get(position)) : "";
        }
    }

}

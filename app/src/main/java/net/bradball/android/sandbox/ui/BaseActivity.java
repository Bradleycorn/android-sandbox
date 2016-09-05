package net.bradball.android.sandbox.ui;

import android.app.ActivityOptions;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import net.bradball.android.sandbox.R;
import net.bradball.android.sandbox.util.LogHelper;


public abstract class BaseActivity extends AppCompatActivity {
    private final static String TAG = LogHelper.makeLogTag(BaseActivity.class);

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mClickedDrawerItem = -1;
    private boolean mToolbarInitialized = false;

    protected abstract boolean usesNavDrawer();

    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_no_controls; }

    @IdRes
    protected int getToolbarId() { return R.id.default_toolbar; }

    @IdRes
    protected int getDrawerId() { return R.id.drawer_layout; }

    @IdRes
    protected int getNavViewId() { return R.id.nav_view; }

    /*
     * getIntentForDrawerItem
     * getDrawerItemForActivity
     *
     * These two methods are used to link activities to the
     * drawer items. Both should be updated when you have a new
     * top level activity that needs to show up in the drawer.
     */

    /**
     * This method is responsible for getting intents that match
     * drawer items. Given a specific Drawer Item's ID, this method
     * will return an Intent suitable for passing to startActivity()
     * to start the correct activity that goes with the menu item.
     *
     * @param itemId - A resource id for a drawer menu item
     * @return an Intent that can be used to start an activity that goes with the drawer menu item.
     */
    private Intent getIntentForDrawerItem(int itemId) {
        switch (itemId) {
            case R.id.navigation_allmusic:
                return BrowserActivity.getIntent(this);
            default:
                return null;
        }
    }

    /**
     * Looks at the current activity and tries to match it
     * to one of the Menu Items in the drawer. If a match
     * is found, returns the id of the associated menu item.
     *
     * @return The R.id of the menu item that goes with the current activity, or -1 if no match is found.
     *
     */
    private int getDrawerItemForActivity() {
        Class thisActivity = getClass();

        if (BrowserActivity.class.isAssignableFrom(thisActivity)) {
            return R.id.navigation_allmusic;
        }

        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(getLayoutResourceId());

        if (usesNavDrawer()) {
            initializeNavDrawer();
        } else {
            initializeToolbar();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //getMenuInflater().inflate(R.menu.main, menu);
        return false; //Change this to true if/when you have a menu to show.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the drawer is open, back will close it
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        // Otherwise, it may return to the previous fragment stack
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    private void initializeToolbar() {
        mToolbar = (Toolbar) findViewById(getToolbarId());
        if (mToolbar == null) {
            throw new IllegalStateException("Activities are required to include a Toolbar widget. If you do not use the default toolbar widget, you must override the getToolbarId method");
        }

        //mToolbar.inflateMenu(R.menu.main);
        setSupportActionBar(mToolbar);
        mToolbarInitialized = true;
    }

    private void initializeNavDrawer() {
        initializeToolbar();

        mDrawerLayout = (DrawerLayout) findViewById(getDrawerId());
        if (mDrawerLayout != null) {
            NavigationView navigationView = (NavigationView) findViewById(getNavViewId());
            if (navigationView == null) {
                throw new IllegalStateException("DrawerActivities are required to include a NavigationView widget. If you do not use the default NavigationView widget, you must override the getNavigationViewId method");
            }
            setupDrawerToggle();
            setupNavigationView(navigationView);
            updateDrawerToggle();
        }
    }

    private void setupDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.app_name);
                }
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (mClickedDrawerItem >= 0) {
                    Intent activityIntent = getIntentForDrawerItem(mClickedDrawerItem);

                    if (activityIntent != null) {
                        Bundle extras = ActivityOptions.makeCustomAnimation(BaseActivity.this, R.anim.fade_in, R.anim.fade_out).toBundle();
                        startActivity(activityIntent, extras);
                        finish();
                    }
                }
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void setupNavigationView(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);

                mClickedDrawerItem = menuItem.getItemId();
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

        int selectedItem = getDrawerItemForActivity();
        if (selectedItem >= 0) {
            navigationView.setCheckedItem(selectedItem);
        }
    }

    protected void updateDrawerToggle() {
        if (mDrawerToggle == null) {
            return;
        }
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        if (getSupportActionBar() != null) {
            //getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        //if (isRoot) {
            mDrawerToggle.syncState();
        //}
    }





}

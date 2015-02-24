package com.microchip.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iangclifton.android.floatlabel.FloatLabel;
import com.microchip.DataStore;
import com.microchip.R;
import com.microchip.adapters.MenuListAdapter;
import com.microchip.api.Api;
import com.microchip.api.ApiService;
import com.microchip.fragments.MainFragment;
import com.microchip.model.ResponseModel;
import com.microchip.provider.response.ResponseColumns;
import com.microchip.provider.response.ResponseCursor;
import com.microchip.provider.response.ResponseProvider;
import com.microchip.utils.InputValidator;
import com.microchip.widget.MicrochipDrawerLayout;
import com.microchip.widget.MicrochipSwitch;

public class MainActivity extends BaseActivity implements MainFragment.MainListener, LoaderManager.LoaderCallbacks<Cursor>{

    private final static int RESPONSE_CALLBACK_ID = 1234;
    private static final int FETCH_DATA = 2;

    private static final int FETCH_DATA_DELAY = 1000;

    private Toolbar toolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private MicrochipDrawerLayout mDrawerLayout;
    private RecyclerView drawerRecyclerView;
    private MenuListAdapter adapter;

    private FrameLayout contentView;
    private MainFragment mainFragment;
    private RelativeLayout settingsContainer;
    private FloatLabel serverAddress;
    private FloatLabel deviceUuid;

    private MenuItem deleteMenuItem;
    private MenuItem settingsMenuItem;

    private int settingsWidth;
    private int settingsHeight;

    private LocalBroadcastManager broadcastManager;
    private ApiReceiver apiReceiver;
    private boolean fetching = false;

    private DrawerListener drawerListener;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case FETCH_DATA:

                    if(!DataStore.getAwsAmi(MainActivity.this).isEmpty() && !DataStore.getUuid(MainActivity.this).isEmpty() && !fetching) {
                        fetching = true;
                        Api.getStatus(MainActivity.this);
                    }

                    if(mainFragment != null){
                        mainFragment.updateContent();
                    }

                    sendFetchDataMessage(FETCH_DATA_DELAY);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        setupDrawerLayout();
        setupActionBar();

        mainFragment = new MainFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(contentView.getId(), mainFragment);
        ft.commit();

        getSupportLoaderManager().initLoader(RESPONSE_CALLBACK_ID, null, this);

        broadcastManager = LocalBroadcastManager.getInstance(this);
        apiReceiver = new ApiReceiver();
    }

    @Override
    public void onResume(){
        super.onResume();
        broadcastManager.registerReceiver(apiReceiver, new IntentFilter(ApiService.BROADCAST));
        startFetchingData();
    }

    @Override
    public void onPause(){
        super.onPause();
        stopFetchingData();
        broadcastManager.unregisterReceiver(apiReceiver);

        closeDrawer();
    }

    @Override
    public void onBackPressed(){
        if(settingsContainer.getVisibility() == View.VISIBLE){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                concealCircleAnimation(settingsContainer, settingsWidth, settingsHeight);
            } else {
                concealSquareAnimation(settingsContainer, settingsWidth, settingsHeight);
            }

        }else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.settings, menu);
        getMenuInflater().inflate(R.menu.delete, menu);

        settingsMenuItem = menu.findItem(R.id.menu_settings);
        deleteMenuItem = menu.findItem(R.id.menu_delete);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(GravityCompat.START);
        settingsMenuItem.setVisible(!drawerOpen);
        deleteMenuItem.setVisible(drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.isDrawerIndicatorEnabled()) {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        }
        // Handle your other action bar items...
        switch (item.getItemId()) {
            case R.id.menu_settings:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    revealCircleAnimation(settingsContainer, settingsWidth, settingsHeight);
                } else {
                    revealSquareAnimation(settingsContainer, settingsWidth, settingsHeight);
                }
                break;

            case R.id.menu_delete:
                adapter.removeAllItems();
                ResponseProvider.deleteAll(this);

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void findViews() {

        // Find Views
        contentView = (FrameLayout) findViewById(R.id.container);
        mDrawerLayout = (MicrochipDrawerLayout) findViewById(R.id.drawer_layout);
        drawerRecyclerView = (RecyclerView) findViewById(R.id.navigation_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        settingsContainer = (RelativeLayout) findViewById(R.id.settings_container);
        serverAddress = (FloatLabel) findViewById(R.id.settings_server_address);

        ImageView wtaAttribution = (ImageView) findViewById(R.id.settings_wta_attribution);
        Toolbar settingsToolBar = (Toolbar) findViewById(R.id.settings_toolbar);
        deviceUuid = (FloatLabel) findViewById(R.id.settings_device_uuid);
        MicrochipSwitch ignoreErrors = (MicrochipSwitch) findViewById(R.id.ignore_error_switch);


        // Settings Container Layout Listener
        settingsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    settingsContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    settingsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                settingsHeight = settingsContainer.getMeasuredHeight();
                settingsWidth = settingsContainer.getMeasuredWidth();
                settingsContainer.setVisibility(View.GONE);
            }
        });

        // Setting ToolBar
        settingsToolBar.setTitle(getString(R.string.settings));
        settingsToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        settingsToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // WTA Attribution
        wtaAttribution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.wta_attribution_url)));
                startActivity(i);
            }
        });

        // Server Address text change listener and validation
        serverAddress.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String url = String.valueOf(s);

                DataStore.persistAwsAmi(MainActivity.this, url);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        serverAddress.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_ENTER:
                        if (!InputValidator.IsUrl(((TextView) v).getText().toString())) {
                            serverAddress.getEditText().setError(getString(R.string.settings_server_address_no_valid));
                            return true;
                        } else {
                            serverAddress.getEditText().setError(null);
                            deviceUuid.requestFocus(); // nextFocus is broken in the FloatLabel library, workaround.
                            return false;
                        }
                    default:
                        return false;
                }
            }
        });
        serverAddress.getEditText().setText(DataStore.getAwsAmi(this));

        // UUID text listener
        deviceUuid.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                DataStore.persistUuid(MainActivity.this, String.valueOf(s));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        deviceUuid.getEditText().setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case KeyEvent.KEYCODE_ENTER:
                        hideSoftKeyboard(v);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
        deviceUuid.getEditText().setText(DataStore.getUuid(this));

        // SSL Errors ignore swich listener
        ignoreErrors.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DataStore.persistIgnoreSllErrors(MainActivity.this, isChecked);
            }
        });
        ignoreErrors.setChecked(DataStore.getIgnoreSllErrors(this));
    }

    public void setupActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void setupDrawerLayout() {
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        adapter = new MenuListAdapter(this, layoutManager);
        adapter.setItems(ResponseProvider.getAll(this));

        drawerRecyclerView.setLayoutManager(layoutManager);
        drawerRecyclerView.setItemAnimator(new DefaultItemAnimator());
        drawerRecyclerView.setAdapter(adapter);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                // Work magic
                if(mainFragment != null)
                    mainFragment.setSlideOffset(slideOffset);
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(getString(R.string.drawer_close));
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                if(drawerListener != null)
                    drawerListener.onDrawerClosed();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(getString(R.string.drawer_open));
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.setDrawerViewWithoutIntercepting(drawerRecyclerView);
    }

    public void closeDrawer(){
        closeDrawer(null);
    }

    public void closeDrawer(DrawerListener listener) {
        drawerListener = listener;
        // If drawer layout is open, close it.
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public interface DrawerListener{
        public void onDrawerClosed();
    }

    @Override
    public void onSwitchesLabelXPosition(int x) {
        if(x != 0){
            DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) drawerRecyclerView.getLayoutParams();
            params.width = x;
            drawerRecyclerView.setLayoutParams(params);
        }
    }

    private class ApiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            int api_type = intent.getIntExtra(ApiService.API_TYPE, -1);
            int status = intent.getExtras().getInt(ApiService.STATUS);

            switch (api_type){
                case ApiService.TYPE_GET_STATUS:
                    switch (status){
                        case ApiService.SUCCESS:
                            fetching = false;
                            break;
                        case ApiService.ERROR:
                            fetching = false;
                            break;
                    }
                    break;
            }

        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, ResponseColumns.CONTENT_URI, null, null, null,
                ResponseColumns.DEFAULT_ORDER + " DESC LIMIT 1");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        ResponseCursor responseCursor = new ResponseCursor(cursor);
        ResponseModel item = ResponseProvider.getSingleItem(responseCursor);

        if(item != null && adapter.items != null){
            if(adapter.items.size() == 0 || !adapter.items.get(0).sha1.equals(item.sha1)){
                adapter.addItem(item);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void startFetchingData(){
        handler.removeMessages(FETCH_DATA);
        sendFetchDataMessage(0);
    }

    private void sendFetchDataMessage(int delay) {
        Message message = new Message();
        message.what = FETCH_DATA;
        handler.sendMessageDelayed(message, delay);
    }

    private void stopFetchingData() {
        handler.removeMessages(FETCH_DATA);
        fetching = false;
    }
}
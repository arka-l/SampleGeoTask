package com.example.vall.geotask;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPositionCreator;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DirectionActivity extends ActionBarActivity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private static final String BUNDLE_KEY_ADAPTER_ITEM_HEIGHT = "BUNDLE_KEY_ADAPTER_ITEM_HEIGHT";
    private static final String BUNDLE_KEY_IF_SCROLLBAR_FADING_ENABLED = "BUNDLE_KEY_IF_SCROLLBAR_FADING_ENABLED";
    private static final String BUNDLE_KEY_COORDS_TO_SHOW = "BUNDLE_KEY_COORDS_TO_SHOW";


    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    //Количество вкладок
    private static final int NUM_PAGES = 2;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    LocationRequest mLocationRequest;
    //ViewPager для реализации вкладок
    private ViewPager mPager;
    //Количество адресов в выпадающем списке
    private int mSearchResultNumber = 7;
    //Количество показываемых адресов
    private int mShownSearchResultNumber = 3;
    //Высота строки одного показываемого адреса
    private int mAdapterItemHeight = 0;
    //Ширина иконки отмены (крестик)
    private int mCancelImageViewWidth = 0;
    //Параметры для ListView
    ViewGroup.LayoutParams mListViewParams;

    //Состояние ScrollBars ListView
    private boolean ifScrollbarFadingEnabled = false;

    //Вспомогательная переменная для опредяления mAdapterItemHeight
    private boolean mIsAdapterItemHeightSet = false;

    //Видно ли listView с результатами геоэнкодинга строки
    private boolean ifListViewVisible = false;


    private PagerAdapter mPagerAdapter;
    //Карта вкладки откуда
    private GoogleMap mMapFrom;
    //Карта вкладки куда
    private GoogleMap mMapTo;

    //Текущий AsyncTask
    GetLocationsTask mGetLocationsTask;

    //Таймер для фильтрации ввода текста
    CountDownTimer mCountDownTimer;
    //Время фильтрации ввода текста в миллисекундах
    private static final long EDIT_TEXT_FILTRATION = (long) (MILLISECONDS_PER_SECOND * 0.7);

    //Текст для поиска
    private String mTextToFind;

    //Флаг необходимости поиска адресов, true - если есть необходимость послать запрос к google maps
    private boolean isNeedToSearch;

    private LinearLayout mLinearLayoutFocusable;

    private EditText mEditText;
    private ImageView mImageView;
    private ListView mListView;
    private Button mSearchButton;

    //	private Resources mResources;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private boolean mUpdatesRequested;
    private ActionBar mActionBar;
    private Location mCurrentLocation;
    private List<Address> mAddresses;
    private Address mAddressFrom;
    private Address mAddressTo;
    private ArrayAdapter<Address> mAdapter;
    private LocationClient mLocationClient;

    //Тоаст сообщений об ошибках.
    private static Toast mToastToShow = null;

    //Таг для логов
    private final static String TAG_LOG = DirectionActivity.class.toString();

    public void setGoogleMap(int position, GoogleMap googleMap) {

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mEditText.clearFocus();
            }
        });
        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
//                mEditText.clearFocus();

                if (cameraPosition.bearing != 0) {

                }
            }
        });
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                mEditText.clearFocus();

            }
        });
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mEditText.clearFocus();
                return false;
            }
        });
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {
                mEditText.clearFocus();

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

            }
        });


        if (position == 0) {
            mMapFrom = googleMap;
        } else if (position == 1) {
            mMapTo = googleMap;
        }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            /*
             * Так как есть Bundle, активити запускается не первый раз. Запускаем AsyncTask для восстановления списка адресов
             */
            mGetLocationsTask = new GetLocationsTask(getApplicationContext(), mSearchResultNumber);
            mGetLocationsTask.execute(mTextToFind);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_directions_tabs);

        mEditText = (EditText) findViewById(R.id.geocode_search_box);
//        mEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);    //Отмена suggestions


        mImageView = (ImageView) findViewById(R.id.icon_cancel);
        final View iconCancel = findViewById(R.id.icon_cancel);
        ViewTreeObserver vto = iconCancel.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mCancelImageViewWidth = iconCancel.getMeasuredHeight();
                setListViewHeight();
                ViewTreeObserver obs = iconCancel.getViewTreeObserver();
                if (obs.isAlive()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        obs.removeOnGlobalLayoutListener(this);
                    } else {
                        obs.removeGlobalOnLayoutListener(this);
                    }
                }
                int paddingRight = (int) (iconCancel.getMeasuredWidth() * 1.1);
                int paddingLeft = mEditText.getPaddingLeft();
                int paddingTop = mEditText.getPaddingTop();
                int paddingBottom = mEditText.getPaddingBottom();
                mEditText.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            }
        });


        mTextToFind = mEditText.getText().toString().trim();
        mListView = (ListView) findViewById(R.id.list_view);
        mLinearLayoutFocusable = (LinearLayout) findViewById(R.id.focusable_view);

        mListViewParams = mListView.getLayoutParams();
        if (savedInstanceState != null) {
            mAdapterItemHeight = savedInstanceState.getInt(BUNDLE_KEY_ADAPTER_ITEM_HEIGHT, 0);
            ifScrollbarFadingEnabled = savedInstanceState.getBoolean(BUNDLE_KEY_IF_SCROLLBAR_FADING_ENABLED);
            if (mAdapterItemHeight != 0) {
                mIsAdapterItemHeightSet = true;
            }
        }

        mListView.setScrollbarFadingEnabled(ifScrollbarFadingEnabled);

        mPager = (ViewPager) findViewById(R.id.pager);
        mSearchButton = (Button)findViewById(R.id.search_button);

        mActionBar = getSupportActionBar();

        Resources resources = getResources();
        mLocationClient = new LocationClient(this, this, this);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Open the shared preferences
        mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        // Get a SharedPreferences editor
        mEditor = mPrefs.edit();
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        // Start with updates turned off
        mUpdatesRequested = false;

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(true);

        //        new AsynkMapLoading().execute(new ViewPager[]{mPager,});

        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        mPager.setAdapter(mPagerAdapter);

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mActionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mPagerAdapter.getCount(); i++) {
            mActionBar.addTab(mActionBar.newTab().setText(mPagerAdapter.getPageTitle(i)).setTabListener(new ActionBar
                    .TabListener() {
                @Override
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                    int position = tab.getPosition();
                    mPager.setCurrentItem(position, true);

                    String textToSet = "";
                    switch (position) {
                        case (0): {
                            if (mAddressFrom != null) {
                                textToSet = mAddressFrom.toString();
                            }
                            break;
                        }
                        case (1): {
                            if (mAddressTo != null) {
                                textToSet = mAddressTo.toString();
                            }
                            break;
                        }
                    }
                    mEditText.setText(textToSet);

                    if (mAdapter != null) {
                        //Удаляем текущую подборку адресов
                        mAdapter.clear();
                        mAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

                }

                @Override
                public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

                }
            }));
        }

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

				/*
                 * Проверяем, что нажата клавиша Enter
				 */
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    /**
                     * При нажатии клавиши Enter выбираем первый элемент из списка (если он есть)
                     */
                    setAddress(0);
                    return true;

                }
                return false;
            }
        });

        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mListView != null) {
                    if (hasFocus) {
                        isNeedToSearch = true;
                        mListView.setVisibility(View.VISIBLE);

                        Address curAddress = null;
                        GoogleMap curMap = null;
                        final int curPage = mPager.getCurrentItem();
                        switch (curPage) {
                            case 0: {
                                if (mAddressFrom != null) {
                                    curAddress = mAddressFrom;
                                    curMap = mMapFrom;
                                }
                                break;
                            }
                            case 1: {
                                if (mAddressTo != null) {
                                    curAddress = mAddressTo;
                                    curMap = mMapTo;
                                }
                                break;
                            }
                        }

                        if (curAddress != null) {
                            //Определяем текущие выбранные координаты
                            LatLng location = new LatLng(curAddress.getLatitude(), curAddress.getLongitude());
                            //Двигаем камеру к данной метке
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(location).zoom(8).build();
                            curMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }

                    } else {
                        mListView.setVisibility(View.INVISIBLE);
                        isNeedToSearch = false;
                    }
                }
            }
        });


        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mTextToFind = s.toString().trim();
                if (!mTextToFind.equals("")) {
                    mImageView.setVisibility(View.VISIBLE);
                } else {
                    mImageView.setVisibility(View.INVISIBLE);
                }

                if (isNeedToSearch && mEditText.isFocused()) {
                    if (mCountDownTimer == null) {
                        mCountDownTimer = new CountDownTimer(EDIT_TEXT_FILTRATION, EDIT_TEXT_FILTRATION) {
                            @Override
                            public void onTick(long millisUntilFinished) {

                            }

                            @Override
                            public void onFinish() {
                                mGetLocationsTask = new GetLocationsTask(getApplicationContext(), mSearchResultNumber);
                                mGetLocationsTask.execute(mTextToFind);
                            }
                        }.start();
                    } else {
                        mCountDownTimer.cancel();
                        mCountDownTimer.start();
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPrefs.contains("KEY_UPDATES_ON")) {
            mUpdatesRequested = mPrefs.getBoolean("KEY_UPDATES_ON", false);
        } else {
            mEditor.putBoolean("KEY_UPDATES_ON", false);
            mEditor.commit();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
        mEditor.commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLocationClient.isConnected()) {

            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPager != null) {
            mPager = null;
        }
        if (mPagerAdapter != null) {
            mPagerAdapter = null;
        }
        if (mEditText != null) {
            mEditText = null;
        }

        if (mLocationClient != null) {
            mLocationClient = null;
        }
        if (mLocationRequest != null) {
            mLocationRequest = null;
        }
        if (mPrefs != null) {
            mPrefs = null;
        }
        if (mEditor != null) {
            mEditor = null;
        }
        if (mActionBar != null) {
            mActionBar = null;
        }
        if (mMapTo != null) {
            mMapTo = null;
        }
        if (mMapFrom != null) {
            mMapFrom = null;
        }

        if (mAdapter != null) {
            mAdapter = null;
        }

        if (mListView != null) {
            mListView = null;
        }

        if (mAddresses != null) {
            mAddresses = null;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_KEY_ADAPTER_ITEM_HEIGHT, mAdapterItemHeight);
        outState.putBoolean(BUNDLE_KEY_IF_SCROLLBAR_FADING_ENABLED, ifScrollbarFadingEnabled);

		/*
         * Отменяем текущий асинктаск
		 */
        final GetLocationsTask task = mGetLocationsTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            task.cancel(true);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mUpdatesRequested) {
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }
        mCurrentLocation = mLocationClient.getLastLocation();

        if (mAddressTo!=null && mAddressFrom!=null && mCurrentLocation!=null){
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSearchButton.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_BOTTOM,0);
        }

//        if (mMapFrom != null && mCurrentLocation != null) {
//            mMapFrom.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(),
//                    mCurrentLocation.getLongitude())).title("onConnected"));
//        }

    }


    /*
     * Методы от  GooglePlayServicesClient.ConnectionCallbacks
     */

    @Override
    public void onDisconnected() {

    }

    /*
     * Методы от GooglePlayServicesClient.OnConnectionFailedListener
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
		 * Стандартные подход от Google:
		 * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, MyActivity.CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
			/*
			 * mToastToShow - статический тоаст Activity. Используем для показа сообщений
			 */
            String errorToShow = getResources().getString(R.string.google_services_error_text);
            if (mToastToShow == null) {
                mToastToShow = Toast.makeText(getApplicationContext(), errorToShow, Toast.LENGTH_SHORT);
            } else {
                mToastToShow.setText(errorToShow);
            }
            mToastToShow.show();
        }
    }

    /*
     * Методы от LocationListener
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (mMapFrom != null) {
            mMapFrom.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude())).title("onLocationChanged"));
        }
    }


    public void onCancelClick(View v) {
        clearAddress(mPager.getCurrentItem());
    }

    public void onButtonClick(View v){
        Intent intent = new Intent(this, ResultActivity.class);
        Bundle bundle = new Bundle();
        bundle.putDoubleArray(BUNDLE_KEY_COORDS_TO_SHOW, new double[]{mAddressFrom.getLatitude(),mAddressFrom.getLongitude()
                ,mAddressTo.getLatitude(),mAddressTo.getLongitude(),mCurrentLocation.getLatitude()
                    ,mCurrentLocation.getLongitude()});
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void setAddress(int addressPosition) {

        isNeedToSearch = false;

        GoogleMap curMap = null;
        Address curAddress = null;
        final int curPage = mPager.getCurrentItem();
        if (mAddresses != null && mAddresses.size() > 0) {
            switch (curPage) {
                case 0: {
                    curMap = mMapFrom;
                    curAddress = mAddressFrom = mAddresses.get(addressPosition);
                    break;
                }
                case 1: {
                    curMap = mMapTo;
                    curAddress = mAddressTo = mAddresses.get(addressPosition);
                    break;
                }
            }

            if (mAddressTo!=null && mAddressFrom!=null && mCurrentLocation!=null){
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSearchButton.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_BOTTOM,0);
            }

            mEditText.setText(curAddress.toString());
            //Стираем предыдущие метки, если они есть
            curMap.clear();

            //Ставим новую метку
            LatLng location = new LatLng(curAddress.getLatitude(), curAddress.getLongitude());
            MarkerOptions curMarkerOptions = new MarkerOptions()
                    .position(location)
                    .title(curAddress.getAddressLine(0));
            Marker marker = curMap.addMarker(curMarkerOptions);
            marker.showInfoWindow();


            //Двигаем камеру к данной метке
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(location).zoom(8).build();
            curMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }


        /**
         * Убираем клавиатуру
         */
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            View focusedView = getCurrentFocus();
            if (focusedView != null) {
                inputManager.hideSoftInputFromWindow(focusedView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }

		/*
		 * Убираем фокус с EditText
		 */
        mEditText.clearFocus();
    }

    private void clearAddress(int pageNumber) {

        mEditText.setText("");

        switch (pageNumber) {
            case (0): {
                mAddressFrom = null;
                mMapFrom.clear();
                break;
            }
            case (1): {
                mAddressTo = null;
                mMapTo.clear();
                break;
            }
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSearchButton.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_BOTTOM,R.id.search_layout);

        /**
         * Убираем клавиатуру
         */
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            View focusedView = getCurrentFocus();
            if (focusedView != null) {
                inputManager.hideSoftInputFromWindow(focusedView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }

		/*
		 * Убираем фокус с EditText
		 */
        mEditText.clearFocus();

        /**
         * Делаем изменения в адаптере
         */
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setListViewWithAddresses(List<Address> addresses) {

        mAddresses = addresses;
		/*
		* Если мы нашли какие-то адреса
		*/
        if (addresses != null) {
			/*
			* Если адаптер еще не был инициализирован
			*/
            if (mAdapter == null) {

//			   /*
//				* Находим высоту одного элемента в ListView программно.
//				* Так как поддерживаем 10 версию,
//				* приходится использовать addOnGlobalLayoutListener. Listner
//				* вызываем только один раз (проверка по mIsAdapterItemHeightSet)
//				*/
                mAdapter = new ArrayAdapter<Address>(this, R.layout.adapter_item, addresses) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View rowView = super.getView(position, convertView, parent);
                        if (!mIsAdapterItemHeightSet) {
                            mIsAdapterItemHeightSet = true;
                            final View ref = rowView;
                            ViewTreeObserver vto = ref.getViewTreeObserver();
                            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mAdapterItemHeight = ref.getMeasuredHeight();
                                    setListViewHeight();
                                    ViewTreeObserver obs = ref.getViewTreeObserver();
                                    if (obs.isAlive()) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                            obs.removeOnGlobalLayoutListener(this);
                                        } else {
                                            obs.removeGlobalOnLayoutListener(this);
                                        }
                                    }
                                }
                            });

                        }
                        return rowView;
                    }
                };

                if (mListView != null) {
                    mListView.setAdapter(mAdapter);

                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            setAddress(position);
                        }
                    });
                }
				/*
				 * Если адаптер уже был инициализирован, то сообщаем об изменениях в адаптере
				 */
            } else {
                mAdapter.clear();
                for (Address a : addresses) {
                    mAdapter.add(a);
                }
                mAdapter.notifyDataSetChanged();
            }

		/*
		* Если количество адресов на поисковый запрос = 0, то
		*/
        } else {
			/*
			* Если адаптер еще не был инициализирован - не делаем ничего, если был, сообщаем
			* об изменениях
			*/
            if (mAdapter != null) {
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();
            }

        }
		/*
		 * Изменяем высоту ListView в зависимости от количества найденных адресов
		 */
        setListViewHeight();
        Toast.makeText(this, "setListViewWithAddresses", Toast.LENGTH_SHORT).show();
    }

	/*
	 * Так как мы хотим, чтобы на экране было не более определенного кол-ва полей найденных результатов,
	 * остальное scroll, мы вручную рассчитываем высоту ListView. Если количество результатов >=
	 * mShownSearchResultNumber,
	 * задаем высоту = mShownSearchResultNumber, если количество меньше mShownSearchResultNumber,
	 * задаем высоту соответственно
	 */

    private void setListViewHeight() {

        if (mAdapterItemHeight != 0 && mListView != null) {
            int itemsNumber = 0; //Количество показываемых элементов
            int listViewHeight = 0; //Текущая высота listView
            ifScrollbarFadingEnabled = true; //Включаем затухание полос прокрутки по умолчанию

		   /*
			* Если высота элемента уже определена и количесто адресов отлично от 0, считаем количество элементов
			*/
            if (mAddresses != null) {
                itemsNumber = mAdapter.getCount();
			/*
			* Если количество результатов больше mShownSearchResultNumber, принудительно ограничиваем до
			* mShownSearchResultNumber
			*/
                if (itemsNumber > mShownSearchResultNumber) {
                    ifScrollbarFadingEnabled = false; //включаем полосы прокрутки
                    itemsNumber = mShownSearchResultNumber;
                }
            }
            if (itemsNumber == 0) {
                listViewHeight = 0;
            } else {
                listViewHeight = mAdapterItemHeight * itemsNumber + mListView.getDividerHeight() * (itemsNumber - 1);
            }
            if (listViewHeight != mListViewParams.height) {
                mListViewParams.height = listViewHeight;
                mListView.setLayoutParams(mListViewParams);
                mListView.requestLayout();
            }
            mListView.setScrollbarFadingEnabled(ifScrollbarFadingEnabled);
        }
    }

    /*
     * АсинкТаск для геокодинга строки
     * Object - результат в формате Object[] {List<Address>, new Integer}, где
     * List<Address> - ArrayList полученных адресов, Integer - код ответа
     */
    private class GetLocationsTask extends AsyncTask<String, Void, Object> {

        private static final int CODE_OK = 100;
        private static final int CODE_HTTP_ERROR = 101;
        private static final int CODE_IO_EXCEPTION = 102;

        private Context mContext;

        //Количесто адресов, получаемых из поискового запроса
        private int mSearchResultsNumber = 7;

        public GetLocationsTask(Context context, int searchResultsNumber) {
            super();
            mContext = context;
            mSearchResultsNumber = searchResultsNumber;
        }

        @Override
        protected void onPostExecute(Object asyncTaskResult) {

            List<Address> addresses = (List<Address>) ((Object[]) asyncTaskResult)[0];
            int responseCode = (Integer) ((Object[]) asyncTaskResult)[1];

            String errorToShow = null;

            Resources resources = getResources();
			/*
			 * Показ ошибок из doInBackground
			 */
            switch (responseCode) {
                case CODE_HTTP_ERROR: {
                    errorToShow = resources.getString(R.string.google_services_error_text);
                    break;
                }
                case CODE_IO_EXCEPTION: {
                    errorToShow = resources.getString(R.string.http_error_text);
                    break;
                }
            }

			/*
			 * mToastToShow - статический тоаст Activity. Используем для показа сообщений
			 */
            if (errorToShow != null) {
                if (mToastToShow == null) {
                    mToastToShow = Toast.makeText(mContext, errorToShow, Toast.LENGTH_SHORT);
                } else {
                    mToastToShow.setText(errorToShow);
                }
                mToastToShow.show();
            }

            setListViewWithAddresses(addresses);

            //Убираем ссылку на текущий AsyncTask
            mGetLocationsTask = null;
        }

        @Override
        protected Object doInBackground(String... params) {

            List<Address> addresses = null;
            Object[] asyncTaskResult = new Object[2];
            asyncTaskResult[0] = addresses;
            asyncTaskResult[1] = CODE_OK;

            String textToSearch = params[0];

            /*
			 * Энкодим строку
             */
            if (!textToSearch.equals("")) {
                try {
                    textToSearch = URLEncoder.encode(textToSearch, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    asyncTaskResult[0] = addresses;
                    return asyncTaskResult;
                }
            }

            /*
			 * Формируем URL
             */
            String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?address=" +
                    textToSearch + "&sensor=false&language=ru";
            URL url = null;
            try {
                url = new URL(googleMapUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                asyncTaskResult[0] = addresses;
                return asyncTaskResult;
            }

            StringBuilder response = new StringBuilder("");
            HttpURLConnection httpconn = null;
            try {
                httpconn = (HttpURLConnection) url.openConnection();

                /*
				 * Если жизнь удалась, и соединение установлено
                 */
                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()), 8192);
                    String strLine = null;
                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                    String jsonOutput = response.toString();
                    try {
                        JSONObject googleMapResponse = new JSONObject(jsonOutput);
                        JSONArray results = (JSONArray) googleMapResponse.get("results");

                        for (int i = 0; i < results.length(); i++) {

                            JSONObject result = results.getJSONObject(i);
                            Address address = new Address(Locale.getDefault()) {
                                @Override
                                public String toString() {
                                    return this.getAddressLine(0);
                                }
                            };

                             /*
							  * Заполняем отдельные компоненты адреса
                              */
                            if (result.has("address_components")) {
                                JSONArray addressComponents = result.getJSONArray("address_components");
                                for (int j = 0; j < addressComponents.length(); j++) {
                                    JSONObject addressComponent = addressComponents.getJSONObject(j);
                                    if (addressComponent.has("types")) {
                                        String typeParam = addressComponent.getJSONArray("types").getString(0);
                                        String paramName = "";
                                        if (addressComponent.has("long_name")) {
                                            paramName = addressComponent.getString("long_name");
                                        } else if (addressComponent.has("short_name")) {
                                            paramName = addressComponent.getString("short_name");
                                        }
                                        if (typeParam.equals("locality")) {
                                            address.setLocality(paramName);
                                        } else if (typeParam.equals("sublocality")) {
                                            address.setSubLocality(paramName);
                                        } else if (typeParam.equals("country")) {
                                            address.setCountryName(paramName);
                                            if (addressComponent.has("short_name")) {
                                                address.setCountryCode(addressComponent.getString("short_name"));
                                            }
                                        } else if (typeParam.equals("administrative_area_level_1")) {
                                            address.setAdminArea(paramName);
                                        } else if (typeParam.equals("administrative_area_level_2")) {
                                            address.setSubAdminArea(paramName);
                                        } else if (typeParam.equals("administrative_area_level_3")) {
                                            address.setSubAdminArea(paramName);
                                        } else if (typeParam.equals("premise")) {
                                            address.setPremises(paramName);
                                        } else if (typeParam.equals("postal_code")) {
                                            address.setPostalCode(paramName);
                                        } else if (typeParam.equals("natural_feature")) {
                                            address.setFeatureName(paramName);
                                        } else if (typeParam.equals("route")) {
                                            address.setThoroughfare(paramName);
                                        }
                                    }
                                }
                            }

                            /*
							 * Заполняем форматированный адрес
                             */
                            if (result.has("formatted_address")) {
                                address.setAddressLine(0, result.getString("formatted_address"));
                            }

                            /*
							 * Заполняем координаты
                             */
                            if (result.has("geometry")) {
                                JSONObject geometry = result.getJSONObject("geometry");
                                if (geometry.has("location")) {
                                    JSONObject location = geometry.getJSONObject("location");
                                    address.setLatitude(location.getDouble("lat"));
                                    address.setLongitude(location.getDouble("lng"));
                                }
                            }

                            /*
							 * Добавляем адрес в ArrayList
                             */
                            if (addresses == null) {
                                addresses = new ArrayList<Address>();
                            }
                            addresses.add(address);


                            /*
							 * Если это крайний результат, установленный в mSearchResultsNumber,
                             * то выходим из цикла
                             */
                            if (i == mSearchResultsNumber - 1) {
                                break;
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        asyncTaskResult[0] = addresses;
                        return asyncTaskResult;
                    }

                 /*
				 * Если мы в печали, так как что-то пошло не так
                 */
                } else {
                    asyncTaskResult[0] = addresses;
                    asyncTaskResult[1] = CODE_HTTP_ERROR;
                    return asyncTaskResult;
                }
			/*
			* Если мы в еще большей печали (нет интернетика?)
            */
            } catch (IOException e) {
                Log.e(TAG_LOG, "IOException from doInBackground");
                e.printStackTrace();
                asyncTaskResult[0] = addresses;
                asyncTaskResult[1] = CODE_IO_EXCEPTION;
                return asyncTaskResult;
            } finally {
                if (httpconn != null) {
                    httpconn.disconnect();
                }
            }

            asyncTaskResult[0] = addresses;
            return asyncTaskResult;
        }

    }

    /*
     * Адаптер для PageViewer
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return CustomSupportMapFragment.newInstance(i);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }

    /*
     * SupportMapFragment.
     */
    public static class CustomSupportMapFragment extends SupportMapFragment {

        private static final String POSITION_KEY = "POSITION_KEY";
        @IdRes
        private static final int LOCATION_BUTTON_ID = 0x2;
        @IdRes
        private static final int ZOOM_CONTROLS_ID = 0x1;

        //        private int mPosition;

        //        public CustomSupportMapFragment(int position) {
        //            mPosition = position;
        //        }

        public static CustomSupportMapFragment newInstance(int position) {
            Bundle args = new Bundle();
            args.putInt(POSITION_KEY, position);
            CustomSupportMapFragment customSupportMapFragment = new CustomSupportMapFragment();
            customSupportMapFragment.setArguments(args);
            return customSupportMapFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View curView = super.onCreateView(inflater, container, savedInstanceState);
            GoogleMap googleMap = this.getMap();
            int position = this.getArguments().getInt(POSITION_KEY);

            View zoomControls = curView.findViewById(ZOOM_CONTROLS_ID);
            View locationButton = curView.findViewById(LOCATION_BUTTON_ID);

            if (locationButton != null && locationButton.getLayoutParams() instanceof RelativeLayout.LayoutParams &&
                    zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                params.addRule(RelativeLayout.ABOVE, ZOOM_CONTROLS_ID);
                params.addRule(RelativeLayout.ALIGN_RIGHT, ZOOM_CONTROLS_ID);


//                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

//                // Update margins, set to 10dp
//                final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
//                        getResources().getDisplayMetrics());
//                params.setMargins(margin, margin, margin, margin);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.setMyLocationEnabled(true);
            } else {
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.setMyLocationEnabled(false);
            }

            googleMap.getUiSettings().setCompassEnabled(false);

			/*
			 * Передаем ссылки на карты в Activity
			 */
            ((DirectionActivity) getActivity()).setGoogleMap(position, googleMap);

//            int leftPadding = curView.getPaddingLeft();
//            int rightPadding = curView.getPaddingRight();
//            int topPadding = curView.getPaddingTop();
//            int bottomPadding = curView.getPaddingBottom();
//
//            Resources r = getResources();
//            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, r.getDimension(R.dimen.margin_small),
//                    getResources().getDisplayMetrics());
//            switch (position) {
//                case (0): {
//                    rightPadding = margin;
//                    break;
//                }
//                case (1): {
//                    leftPadding = margin;
//                    break;
//                }
//            }
//            curView.
//            curView.setPadding(leftPadding,topPadding,rightPadding,bottomPadding);
            return curView;
        }
    }
}

//    /*
//     * Обработка нажатий назад для вкладок
//     */
//    @Override
//    public void onBackPressed() {
//        if (mPager.getCurrentItem() == 0) {
//            super.onBackPressed();
//        } else {
//            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
//        }
//    }

//                                        String addressText = String.format(
//                                                "%s, %s, %s",
//                                                this.getMaxAddressLineIndex() > 0 ?
//                                                        this.getAddressLine(0) : "",
//                                                this.getLocality(),
//                                                this.getCountryName());

//                textToSearch.replaceAll(" ", "%20");         //Подготавливаем строку для запроса

//                File f = new File(getExternalCacheDir(), "samplefile1.txt"
//                );
//                FileWriter os;
//                try {
//                    os = new FileWriter(f);
//                    os.write(googleMapUrl);
//                    os.close();
//                } catch (FileNotFoundException e) {
//                } catch (IOException e) {
//                }

//                    Toast.makeText(getBaseContext(),"httpconn",Toast.LENGTH_SHORT).show();

//                    File f1 = new File(getExternalCacheDir(), "samplefile.txt");
//                    FileWriter os1;
//                    try {
//                        os1 = new FileWriter(f1);
//                        os1.write("HttpURLConnection httpconn = null");
//                        os1.close();
//                    } catch (FileNotFoundException e) {
//                    } catch (IOException e) {
//                    }

//                        try {
//                            os1 = new FileWriter(f1);
//                            os1.write("httpconn = (HttpURLConnection) url.openConnection()");
//                            os1.close();
//                        } catch (FileNotFoundException e) {
//                        } catch (IOException e) {
//                        }

//            JSONObject googleMapResponse = new JSONObject(ANDROID_HTTP_CLIENT.execute(new
// HttpGet(googleMapUrl),
//                    new BasicResponseHandler()));

//            try {
//
//            } catch (Exception ignored) {
//                ignored.printStackTrace();
//            }

//            String addressToSearch = params[0];
//            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
//            List<Address> addresses = null;
//
//            try {
//                /*
//                 * Return 1 address.
//                 */
//                addresses = geocoder.getFromLocationName(addressToSearch, 7);
//                Address a = new Address(Locale.getDefault());
//            } catch (IOException e1) {
//                Log.e("LocationSampleActivity",
//                        "IO Exception in getFromLocation()");
//                e1.printStackTrace();
////                return ("IO Exception trying to get address");
////                return null;
//            } catch (IllegalArgumentException e2) {
//                // Error message to post in the log
////                String errorString = "Illegal arguments " +
////                        Double.toString(loc.getLatitude()) +
////                        " , " +
////                        Double.toString(loc.getLongitude()) +
////                        " passed to address service";
//                Log.e("LocationSampleActivity", "IllegalArgumentException");
//                e2.printStackTrace();
//
////                return errorString;
//            }
//            return addresses;

//    public void getAddresses(String stringToSearch) {
//        if (Build.VERSION.SDK_INT >=
//                Build.VERSION_CODES.GINGERBREAD
//                &&
//                Geocoder.isPresent()) {
//            (new GetLocationsTask(this)).execute(new String[]{stringToSearch,});
//        }
//
//    }

//    @Override
//    public void onMapLoaded() {
//
//        if (mMapFrom != null) {
//            mMapFrom.addMarker(new MarkerOptions()
//                    .position(new LatLng(10, 10))
//                    .title("Hello world"));
//        }
//    }

//    private class AsynkMapLoading extends AsyncTask<ViewPager, Void, Object> {
//
//
//        @Override
//        protected void onPostExecute(Object result) {
//
//            Object[] returnResult = (Object[]) result;
//            mPager = (ViewPager)returnResult[0];
//            mPagerAdapter = (PagerAdapter)returnResult[1];
//
//            mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
//                @Override
//                public void onPageSelected(int position) {
//                    mActionBar.setSelectedNavigationItem(position);
//                }
//            });
//
//            for (int i = 0; i < mPagerAdapter.getCount(); i++) {`
//                mActionBar.addTab(
//                        mActionBar.newTab()
//                                .setText(mPagerAdapter.getPageTitle(i))
//                                .setTabListener(new ActionBar.TabListener() {
//                                    @Override
//                                    public void onTabSelected(ActionBar.Tab tab,
// FragmentTransaction fragmentTransaction) {
//                                        int position = tab.getPosition();
//                                        mPager.setCurrentItem(position, false);
//
//                                    }
//
//                                    @Override
//                                    public void onTabUnselected(ActionBar.Tab tab,
// FragmentTransaction fragmentTransaction) {
//
//                                    }
//
//                                    @Override
//                                    public void onTabReselected(ActionBar.Tab tab,
// FragmentTransaction fragmentTransaction) {
//
//                                    }
//                                }));
//            }
//        }
//
//        @Override
//        protected Object doInBackground(ViewPager... viewPager) {
//            PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
//            ViewPager pager = viewPager[0];
//            pager.setAdapter(pagerAdapter);
//            return new Object[] {pager,pagerAdapter};
//        }
//    }
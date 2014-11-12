package com.example.vall.geotask;

import android.content.Context;
import android.content.IntentSender;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks
        , GooglePlayServicesClient.OnConnectionFailedListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private Toast mToastToShow;
    private LatLng mLocationFrom;
    private LatLng mLocationTo;
    private LatLng mCurLocation;

    //Текущий AsyncTask
    GetDirectionsTask mGetDirectionsTask;

    //Таг для логов
    private final static String TAG_LOG = ResultActivity.class.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mLocationClient = new LocationClient(this, this, this);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        double[] coords =  getIntent().getExtras().getDoubleArray(DirectionActivity.BUNDLE_KEY_COORDS_TO_SHOW);
        mLocationFrom = new LatLng(coords[0],coords[1]);
        mLocationTo = new LatLng(coords[2],coords[3]);

//        mCurLocation = new LatLng(coords[4],coords[5]);

        setUpMapIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient = null;
        }
        if (mLocationRequest != null) {
            mLocationRequest = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                double[] coords =  getIntent().getExtras().getDoubleArray(DirectionActivity.BUNDLE_KEY_COORDS_TO_SHOW);
                setUpMap(coords);
            }
        }
    }


    private void setUpMap(double[] coords) {
//        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        if (mGetDirectionsTask==null){
            mGetDirectionsTask = new GetDirectionsTask(this);
            Double a = (Double)coords[0];
            Double[] coordsInDouble = new Double[coords.length];
            for (int i=0; i<coords.length;i++){
                coordsInDouble[i] = coords[i];
            }
            mGetDirectionsTask.execute(coordsInDouble);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = mLocationClient.getLastLocation();
        if (mCurrentLocation!=null) {
            mCurLocation = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        }

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, MyActivity.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
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


    private  class GetDirectionsTask extends AsyncTask<Double, Void, Object>{

        private static final int DIRECTIONS_NOT_FOUND = 99;
        private static final int CODE_OK = 100;
        private static final int CODE_HTTP_ERROR = 101;
        private static final int CODE_IO_EXCEPTION = 102;

        private Context mContext;

        public GetDirectionsTask(Context context){
            super();
            mContext = context;
        }


        @Override
        protected void onPostExecute(Object asyncTaskResult) {

            List<LatLng> directionCoords = (List<LatLng>) ((Object[]) asyncTaskResult)[0];
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
            if (directionCoords!=null){
                drawPath(directionCoords);
            } else {
                Toast.makeText(mContext,"null",Toast.LENGTH_SHORT).show();
            }


            //Убираем ссылку на текущий AsyncTask
            mGetDirectionsTask = null;
        }

        @Override
        protected Object doInBackground(Double... coords) {

            List<LatLng> directCoords = null;
            Object[] asyncTaskResult = new Object[2];
            asyncTaskResult[0] = directCoords;
            asyncTaskResult[1] = CODE_OK;


            /*
			 * Формируем URL
             */
            String googleMapUrl = "http://maps.googleapis.com/maps/api/directions/json?origin=" +
                    coords[0] + "," + coords[1] + "&destination="+ coords[2] + "," +
                    coords[3] + "&sensor=false";
            URL url = null;
            try {
                url = new URL(googleMapUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                asyncTaskResult[0] = directCoords;
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
                        String googleMapResponseCode = googleMapResponse.getString("status");
                        /**
                         * Если код ответа не ОК, выходим из метода
                         */
                        if (!googleMapResponseCode.equals("OK")){
                            asyncTaskResult[0] = directCoords;
                            asyncTaskResult[1] = DIRECTIONS_NOT_FOUND;
                            return asyncTaskResult;
                        }

                        if (googleMapResponse.has("routes")){
                            JSONArray routes = googleMapResponse.getJSONArray("routes");
                            if (routes.getJSONObject(0).has("overview_polyline")){
                                JSONObject pointPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline");
                                directCoords = decodePoly(pointPolyline.getString("points"));
                                asyncTaskResult[0] = directCoords;
                                return asyncTaskResult;
                            }


//                            if (routes.getJSONObject(0).has("legs")){
//                                JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");
//                                JSONObject nextLocation = legs.getJSONObject(0).getJSONObject("start_location");
//                                double lat = nextLocation.getDouble("lat");
//                                double lng = nextLocation.getDouble("lng");
//                                LatLng loc = new LatLng(lat,lng);
//                                directCoords = new ArrayList<LatLng>();
//                                directCoords.add(loc);
//                                for (int i = 0; i < legs.length(); i++){
//                                    JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
//                                    for (int j = 0; j < steps.length(); j++){
//                                        nextLocation = steps.getJSONObject(j).getJSONObject("end_location");
//                                        lat = nextLocation.getDouble("lat");
//                                        lng = nextLocation.getDouble("lng");
//                                        loc = new LatLng(lat,lng);
//                                        directCoords.add(loc);
//                                    }
//                                }
//                                asyncTaskResult[0] = directCoords;
//                                return asyncTaskResult;
//                            }
                        }



                    } catch (JSONException e) {
                        e.printStackTrace();
                        asyncTaskResult[0] = directCoords;
                        return asyncTaskResult;
                    }

                 /*
				 * Если мы в печали, так как что-то пошло не так
                 */
                } else {
                    asyncTaskResult[0] = directCoords;
                    asyncTaskResult[1] = CODE_HTTP_ERROR;
                    return asyncTaskResult;
                }
			/*
			* Если мы в еще большей печали (нет интернетика?)
            */
            } catch (IOException e) {
                Log.e(TAG_LOG, "IOException from doInBackground");
                e.printStackTrace();
                asyncTaskResult[0] = directCoords;
                asyncTaskResult[1] = CODE_IO_EXCEPTION;
                return asyncTaskResult;
            } finally {
                if (httpconn != null) {
                    httpconn.disconnect();
                }
            }

            asyncTaskResult[0] = directCoords;
            return asyncTaskResult;
        }
    }


    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        double lat = .0, lng = .0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            double dlat = ((double)((result & 1) != 0 ? ~(result >> 1) : (result >> 1)))/1E5;
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            double dlng = ((double)((result & 1) != 0 ? ~(result >> 1) : (result >> 1)))/1E5;
            lng += dlng;
            LatLng p = new LatLng(lat,lng);
            poly.add(p);
        }
        return poly;
    }


    private void drawPath(List<LatLng> directionCoords) {
        final int lineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());
        Polyline line = mMap.addPolyline(new PolylineOptions().addAll(directionCoords)
                .width(lineWidth).color(Color.BLACK));

        /**
         * Рассчитываем границы, к которым необходимо подвинуть экран. Для этого находим самую
         * северо-восточную точку и юго-западную
         */
        double northLimit = directionCoords.get(0).latitude;
        double eastLimit = directionCoords.get(0).longitude;
        double southLimit = directionCoords.get(0).latitude;
        double westLimit = directionCoords.get(0).longitude;

        List<LatLng> tempCoords = directionCoords;
//        if (mCurLocation!=null) {
//            tempCoords.add(mCurLocation);
//        }
        for (LatLng point : tempCoords) {
            if (northLimit<point.latitude)
                northLimit = point.latitude;
            if (southLimit>point.latitude)
                southLimit = point.latitude;
            if (eastLimit<point.longitude)
                eastLimit = point.longitude;
            if (westLimit>point.longitude)
                westLimit = point.longitude;
        }

        LatLng northEastPoint = new LatLng(northLimit,eastLimit);
        LatLng southWestPoint = new LatLng(southLimit,westLimit);
        LatLngBounds cameraPosBounds = new LatLngBounds(
                southWestPoint, northEastPoint);
        final int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                getResources().getDisplayMetrics());
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cameraPosBounds, padding));
    }
}

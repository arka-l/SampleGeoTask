package com.example.vall.geotask;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class MyActivity extends ActionBarActivity {

    private static final int REQUEST_CODE = 1;
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mButton = (Button) findViewById(R.id.main_activity_button);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Resources r = getResources();


        /**
         * Проверка на доступность службы Google Service
         * При недоступности вызываем встроенный диалог
         */
        if (isGoogleServicesOk()) {
            mButton.setEnabled(true);
            mButton.setText(r.getText(R.string.splash_screen_button_text));
        } else {
            mButton.setEnabled(false);
            mButton.setText(r.getText(R.string.google_play_services_missing_text));
        }

    }

    public void onSplashButtonClick(View v) {
        Intent intent = new Intent(this, DirectionActivity.class);
        startActivity(intent);
    }

    private boolean isGoogleServicesOk() {
        int flag = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        switch (flag) {
            case (ConnectionResult.SUCCESS): {
                return true;
            }
            case (ConnectionResult.SERVICE_MISSING): {

            }

            case (ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED): {

            }

            case (ConnectionResult.SERVICE_DISABLED): {

            }
        }
        Toast.makeText(this, "" + flag, Toast.LENGTH_SHORT).show();
        GooglePlayServicesUtil.getErrorDialog(flag, this, REQUEST_CODE);
        return false;
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//        switch (requestCode) {
//            case(REQUEST_CODE):{
//                if (resultCode== Activity.RESULT_OK){
//
//                }
//            }
//        }
//    }


    //
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.my, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}

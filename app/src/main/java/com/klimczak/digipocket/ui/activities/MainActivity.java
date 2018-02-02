package com.klimczak.digipocket.ui.activities;

import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.klimczak.digipocket.R;
import com.klimczak.digipocket.ui.helpers.BottomNavigationViewHelper;
import com.klimczak.digipocket.ui.widget.ControllableAppBarLayout;
import com.klimczak.digipocket.utils.Constants;

public class MainActivity extends AppCompatActivity {

    private static final int TITLE_ANIMATE_DURATION = 600;
    private static final int TITLE_ANIMATE_DURATION2 = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ControllableAppBarLayout oAppBar = (ControllableAppBarLayout) findViewById(R.id.app_bar);
        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);

        final LinearLayout titleContainer = (LinearLayout) findViewById(R.id.title_container);


        oAppBar.setOnStateChangeListener(new ControllableAppBarLayout.OnStateChangeListener() {
            @Override
            public void onStateChange(ControllableAppBarLayout.State toolbarChange) {
                switch(toolbarChange){
                    case COLLAPSED:  collapsingToolbarLayout.setTitle(Constants.APP_NAME);
                                     collapsingToolbarLayout.animate().alpha(1).setDuration(TITLE_ANIMATE_DURATION);
                                     titleContainer.animate().alpha(0).setDuration(TITLE_ANIMATE_DURATION2);
                                     break;
                    case EXPANDED:   collapsingToolbarLayout.setTitle(Constants.APP_NAME_NO_VISIBLE);//space between double quote otherwise it wont work
                                     collapsingToolbarLayout.animate().alpha(1).setDuration(TITLE_ANIMATE_DURATION);
                                     titleContainer.animate().alpha(1).setDuration(TITLE_ANIMATE_DURATION);
                                     break;
                    case IDLE:       collapsingToolbarLayout.setTitle(Constants.APP_NAME_NO_VISIBLE);
                                     break;

                }
            }
        });
        BottomNavigationView navigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        BottomNavigationViewHelper.removeShiftMode(navigationView);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

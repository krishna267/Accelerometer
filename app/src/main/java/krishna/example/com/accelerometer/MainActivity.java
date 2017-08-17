package krishna.example.com.accelerometer;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import static android.R.transition.fade;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Spinner datapoints,degree,alpha;
    private TextView x,y,z;
    private SensorManager sensorManager;
    private Sensor sensor;
    private Button start,reset;
    private Boolean record =false,sgolay = true,ema =false;
    private GraphView graphView;
    private Handler handler;
    private double  i=0,x0 = 0,alp = 0.2;
    private int n=10,d=2;
    private LineGraphSeries<DataPoint> series,filtered;
    private double[] coeff;
    private double[] values = new double[2*n+1];
    private SGFilter sgFilter = new SGFilter(n,n);
    private DrawerLayout drawerLayout;
    private String[] filterslist;
    private ArrayAdapter<String> adapter;
    private ListView listView;
    private CharSequence mTitle,mDrawerTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private Scene mScene,mAnother;
    private ViewGroup scene_root;
    private ArrayAdapter<CharSequence> alpha_values,dp,deg;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize views
        scene_root = (ViewGroup)findViewById(R.id.scene_root);
        datapoints = (Spinner)findViewById(R.id.spinner);
        degree = (Spinner)findViewById(R.id.spinner2);

        x = (TextView)findViewById(R.id.xdata);
        y = (TextView)findViewById(R.id.ydata);
        z = (TextView)findViewById(R.id.zdata);
        start = (Button)findViewById(R.id.start);
        reset = (Button)findViewById(R.id.reset);
        graphView = (GraphView)findViewById(R.id.graph);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer);
        listView = (ListView)findViewById(R.id.left_drawer);
        mScene = Scene.getSceneForLayout(scene_root,R.layout.scene,this);
        mAnother = Scene.getSceneForLayout(scene_root,R.layout.another_scene,this);

        //transition
        final Transition mTransition = new Fade();

        //toolbar
        final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        myToolbar.setTitleTextColor(Color.WHITE);
        mTitle = mDrawerTitle = "Accellog";
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,myToolbar, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                myToolbar.setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                myToolbar.setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(mDrawerToggle);

        //Drawerlist
        filterslist = getResources().getStringArray(R.array.filters);
        adapter = new ArrayAdapter<String>(this,R.layout.listitem,filterslist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0: {
                        sgolay = true;
                        refreshgraph();
                        ema = false;
                        record = false;
                        TransitionManager.go(mScene,mTransition);
                        datapoints = (Spinner)findViewById(R.id.spinner);
                        datapoints.setAdapter(dp);
                        datapoints.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                                n = position + 10;
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                        degree = (Spinner)findViewById(R.id.spinner2);
                        degree.setAdapter(deg);
                        degree.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                                d = position + 2;
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                        break;
                    }
                    case 1: {
                        refreshgraph();
                        ema = true;
                        sgolay = false;
                        record = false;
                        TransitionManager.go(mAnother,mTransition);
                        alpha = (Spinner)findViewById(R.id.alpha_spinner);
                        alpha.setAdapter(alpha_values);
                        alpha.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                record = false;
                                refreshgraph();
                                alp = 0.2*(position+1);
                                x0 = 0;
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                        break;
                    }
                }
            }
        });

        //handling
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 5){
                    series.appendData(new DataPoint(i,(double)msg.obj),true,480);
                    if(sgolay){
                        shift(values,(double)msg.obj);
                        if(i>(0.1*n)){
                            filtered.appendData(new DataPoint(i-0.1*n,output(values,coeff)),true,490);
                        }
                    }
                    if(ema){
                        double dp = (alp)*(double)msg.obj + (1-alp)*(x0);
                        filtered.appendData(new DataPoint(i,dp),true,490);
                        x0 = dp;
                    }
                    i = i +0.1;
                }
            }
        };

        //spinners
        dp = ArrayAdapter.createFromResource(this,R.array.data_points,R.layout.support_simple_spinner_dropdown_item);
        datapoints.setAdapter(dp);
        deg = ArrayAdapter.createFromResource(this,R.array.degree,R.layout.support_simple_spinner_dropdown_item);
        degree.setAdapter(deg);
        alpha_values = ArrayAdapter.createFromResource(this,R.array.alpha,R.layout.support_simple_spinner_dropdown_item);


        datapoints.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                n = position + 10;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        degree.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                d = position + 2;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        //Customize graphview
        Viewport viewport = graphView.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(50);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0);
        viewport.setMaxY(20);

        //START BUTTON
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record = !record;
                if(record){
                    refreshgraph();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record = false;
                refreshgraph();
                x0 = 0;
            }
        });

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float xd = event.values[0];
        float yd = event.values[1];
        float zd = event.values[2];
        x.setText(""+xd);
        y.setText(""+yd);
        z.setText(""+zd);
        if(record) {
            double mag = Math.sqrt(xd*xd+yd*yd+zd*zd);
            handler.obtainMessage(5,mag).sendToTarget();
        }
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(listView);
        //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    public void refreshgraph(){
        handler.removeMessages(5);
        graphView.removeAllSeries();
        series = new LineGraphSeries<DataPoint>();
        filtered =  new LineGraphSeries<DataPoint>();
        filtered.setColor(Color.GREEN);
        graphView.addSeries(series);
        series.setTitle("Sampled Data");
        graphView.addSeries(filtered);
        filtered.setTitle("Filtered Data");
        graphView.getLegendRenderer().setVisible(true);
        graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        i = 0;
        if(sgolay){
            coeff = sgFilter.computeSGCoefficients(n,n,d);
            values = new double[2*n+1];
        }
    }

    public double[] shift(double val[],double next){
        for(int j=0;j<(val.length-1);j++){
            val[j] = val[j+1];
        }
        val[val.length-1] = next;
        return val;
    }

    public double output(double[] val,double[] cfs){
        double out=0;
        for(int k=0;k<val.length;k++){
            out = out+val[k]*cfs[k];
        }
        return out;
    }
}

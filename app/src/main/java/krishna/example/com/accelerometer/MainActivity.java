package krishna.example.com.accelerometer;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Spinner datapoints,degree;
    private TextView x,y,z;
    private SensorManager sensorManager;
    private Sensor sensor;
    private Button start;
    private List<Double> arrayList;
    private Boolean record =false;
    private GraphView graphView;
    private double values[];
    private double well[];
    private double filt[];
    private Handler handler;
    private  int nl=3,nr=3,d=2;
    private LineGraphSeries<DataPoint> series,filtered;
    private double[] xyz = new double[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize views
        datapoints = (Spinner)findViewById(R.id.spinner);
        degree = (Spinner)findViewById(R.id.spinner2);
        x = (TextView)findViewById(R.id.xdata);
        y = (TextView)findViewById(R.id.ydata);
        z = (TextView)findViewById(R.id.zdata);
        start = (Button)findViewById(R.id.start);
        graphView = (GraphView)findViewById(R.id.graph);

        //handling
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_WHAT) {
                    double mag = (double)msg.obj;
                    series.appendData(new DataPoint(i, mag), true, 1000);

                    //update
                    int k=0;
                    for(;k<(2*nl);k++){
                        xyz[k] = xyz[k+1];
                    }
                    xyz[k] = mag;

                    double dp =0;
                    for(int j=0;j< (2*nl+1);j++){
                        dp = dp + filt[j]*xyz[j];
                    }
                    if (i > nl) {
                        filtered.appendData(new DataPoint(i - nl, dp), true, 1000);
                    }
                    i++;
                }
            }
        };

        //spinners
        ArrayAdapter<CharSequence> dp = ArrayAdapter.createFromResource(this,R.array.data_points,android.R.layout.simple_spinner_item);
        dp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        datapoints.setAdapter(dp);
        ArrayAdapter<CharSequence> deg = ArrayAdapter.createFromResource(this,R.array.degree,android.R.layout.simple_spinner_item);
        deg.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        degree.setAdapter(deg);

        datapoints.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                record = false;
                Toast.makeText(MainActivity.this,"Please wait!",Toast.LENGTH_LONG).show();
                new CountDownTimer(2000, 1000) {

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        refresh();
                        nl=nr=position+3;
                        xyz = new double[2*nl+1];
                    }
                }.start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        degree.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                record = false;
                Toast.makeText(MainActivity.this,"Please wait!",Toast.LENGTH_LONG).show();
                new CountDownTimer(2000, 1000) {

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        refresh();
                        d = position +2;
                        xyz = new double[2*nl+1];
                    }
                }.start();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        //Customize graphview
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMaxX(200);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMaxY(20);
        graphView.setTitle("Graph");
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScrollable(true);

        //START BUTTON
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(record) {
                    refresh();
                    Toast.makeText(MainActivity.this,"Please wait!",Toast.LENGTH_LONG).show();
                    new CountDownTimer(2000, 1000) {

                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                        }
                    }.start();
                }

                if(!record){
                    SGFilter sgFilter = new SGFilter(nl,nr);
                    filt = sgFilter.computeSGCoefficients(nl,nr,d);

                    series = new LineGraphSeries<DataPoint>();
                    filtered = new LineGraphSeries<DataPoint>();
                    filtered.setColor(Color.GREEN);

                    graphView.addSeries(series);
                    graphView.addSeries(filtered);
                    series.setTitle("Sampled Data");
                    filtered.setTitle("Filtered Data");
                    graphView.getLegendRenderer().setVisible(true);
                    graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                }

                record = !record;
            }
        });

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI);
    }

    private void refresh() {
        graphView.removeAllSeries();
        i = 0;
        graphView.getLegendRenderer().setVisible(false);
    }

    private int i =0,MSG_WHAT = 1;

    @Override
    public void onSensorChanged(SensorEvent event) {
        float xd = event.values[0];
        float yd = event.values[1];
        float zd = event.values[2];
        x.setText(""+xd);
        y.setText(""+yd);
        z.setText(""+zd);
        if(record) {
            double mag = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
            handler.obtainMessage(MSG_WHAT,0,0,mag).sendToTarget();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}

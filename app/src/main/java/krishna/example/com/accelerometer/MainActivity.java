package krishna.example.com.accelerometer;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Spinner datapoints,degree;
    private TextView x,y,z;
    private SensorManager sensorManager;
    private Sensor sensor;
    private Button start,reset;
    private Boolean record =false;
    private GraphView graphView;
    private Handler handler;
    private double  i=0;
    private int n=10,d=2;
    private LineGraphSeries<DataPoint> series,filtered;
    private double[] coeff;
    private double[] values = new double[2*n+1];
    private SGFilter sgFilter = new SGFilter(n,n);

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
        reset = (Button)findViewById(R.id.reset);
        graphView = (GraphView)findViewById(R.id.graph);

        //handling
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 5){
                    series.appendData(new DataPoint(i,(double)msg.obj),true,480);
                    shift(values,(double)msg.obj);
                    if(i>(0.1*n)){
                        filtered.appendData(new DataPoint(i-0.1*n,output(values,coeff)),true,490);
                    }
                    i = i +0.1;
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
        coeff = sgFilter.computeSGCoefficients(n,n,d);
        values = new double[2*n+1];
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

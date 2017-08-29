package krishna.example.com.accelerometer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

/**
 * Created by Krishna Vemuri on 8/19/2017.
 */

public class Infoactivity extends AppCompatActivity {
    private ListView listView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_layout);

        listView = (ListView)findViewById(R.id.info_list);
    }
}



package com.hamnetwork.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("HAM Wallet");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText("\nWallet HAM siap.\n\nBelum terhubung ke blockchain.");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(status);

        setContentView(layout);
    }
}


package com.hamnetwork.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    TextView address;
    TextView balance;
    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("HAM Wallet");
        title.setTextSize(30);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("Wallet HAM siap");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);

        address = new TextView(this);
        address.setText("\nAlamat HAM:\nBelum dibuat");
        address.setTextSize(16);
        address.setGravity(Gravity.CENTER);

        balance = new TextView(this);
        balance.setText("\nSaldo HAM: 0 HAM");
        balance.setTextSize(20);
        balance.setGravity(Gravity.CENTER);

        Button createWallet = new Button(this);
        createWallet.setText("BUAT WALLET");

        Button mining = new Button(this);
        mining.setText("MULAI MINING");

        createWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                address.setText(
                    "\nAlamat HAM:\nHAM-WALLET-ANDROID"
                );
                status.setText("Wallet HAM berhasil dibuat");
            }
        });

        mining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText(
                    "Mining HAM:\nBelum terhubung ke node"
                );
            }
        });

        layout.addView(title);
        layout.addView(status);
        layout.addView(address);
        layout.addView(balance);
        layout.addView(createWallet);
        layout.addView(mining);

        setContentView(layout);
    }
}

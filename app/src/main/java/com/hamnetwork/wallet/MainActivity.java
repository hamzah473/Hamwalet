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
package com.hamnetwork.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class MainActivity extends Activity {

    private static final int DIFFICULTY = 5;
    private static final long REWARD = 50;

    private TextView addressView;
    private TextView balanceView;
    private TextView statusView;
    private TextView miningView;

    private SharedPreferences prefs;

    private String privateKey;
    private String publicKey;
    private String address;

    private long balance;
    private int blockIndex;
    private String previousHash;

    private volatile boolean mining = false;
    private Thread miningThread;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("ham_wallet", MODE_PRIVATE);

        loadWallet();
        buildInterface();
        updateScreen();
    }

    private void loadWallet() {
        privateKey = prefs.getString("private_key", null);

        if (privateKey == null) {
            privateKey = randomHex(32);
            publicKey = sha256(privateKey);
            address = "HAM1" + sha256(publicKey).substring(0, 40);

            balance = 0;
            blockIndex = -1;
            previousHash = "";

            prefs.edit()
                    .putString("private_key", privateKey)
                    .putString("public_key", publicKey)
                    .putString("address", address)
                    .putLong("balance", balance)
                    .putInt("block_index", blockIndex)
                    .putString("previous_hash", previousHash)
                    .apply();
        } else {
            publicKey = prefs.getString("public_key", sha256(privateKey));
            address = prefs.getString(
                    "address",
                    "HAM1" + sha256(publicKey).substring(0, 40)
            );

            balance = prefs.getLong("balance", 0);
            blockIndex = prefs.getInt("block_index", -1);
            previousHash = prefs.getString("previous_hash", "");
        }
    }

    private void buildInterface() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("HAM WALLET");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);

        statusView = new TextView(this);
        statusView.setTextSize(18);
        statusView.setGravity(Gravity.CENTER);

        addressView = new TextView(this);
        addressView.setTextSize(14);
        addressView.setGravity(Gravity.CENTER);

        balanceView = new TextView(this);
        balanceView.setTextSize(22);
        balanceView.setGravity(Gravity.CENTER);

        Button copy = new Button(this);
        copy.setText("SALIN ALAMAT");

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                cm.setPrimaryClip(
                        ClipData.newPlainText("HAM Address", address)
                );

                statusView.setText("Alamat HAM disalin");
            }
        });

        Button mining = new Button(this);
        mining.setText("MULAI MINING");

        mining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mining) {
                    startMining();
                    mining.setText("BERHENTI MINING");
                } else {
                    stopMining();
                    mining.setText("MULAI MINING");
                }
            }
        });

        miningView = new TextView(this);
        miningView.setTextSize(15);
        miningView.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(statusView);
        layout.addView(addressView);
        layout.addView(balanceView);
        layout.addView(copy);
        layout.addView(mining);
        layout.addView(miningView);

        setContentView(layout);
    }

    private void updateScreen() {

        addressView.setText(
                "\nAlamat HAM:\n" + address + "\n"
        );

        balanceView.setText(
                "Saldo: " + balance + " HAM\n"
        );

        if (mining) {
            statusView.setText("MINING HAM AKTIF");
        } else {
            statusView.setText("Wallet HAM siap");
        }

        miningView.setText(
                "Block: " +
                (blockIndex < 0 ? "Belum ada" : blockIndex)
        );
    }

    private void startMining() {

        if (mining) {
            return;
        }

        mining = true;
        updateScreen();

        miningThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (mining) {
                    mineBlock();
                }
            }
        });

        miningThread.start();
    }

    private void stopMining() {

        mining = false;

        if (miningThread != null) {
            miningThread.interrupt();
        }

        statusView.setText("Mining dihentikan");
    }

    private void mineBlock() {

        int index = blockIndex + 1;

        String prev;

        if (blockIndex < 0) {
            prev = repeat("0", 64);
        } else {
            prev = previousHash;
        }

        long timestamp = System.currentTimeMillis() / 1000L;

        String data =
                "HAM mining reward " +
                REWARD +
                " to " +
                address;

        long nonce = 0;

        while (mining) {

            String hash = sha256(
                    "" +
                    index +
                    prev +
                    timestamp +
                    data +
                    nonce
            );

            if (hash.startsWith(repeat("0", DIFFICULTY))) {

                blockIndex = index;
                previousHash = hash;
                balance += REWARD;

                prefs.edit()
                        .putLong("balance", balance)
                        .putInt("block_index", blockIndex)
                        .putString("previous_hash", previousHash)
                        .apply();

                final String resultHash = hash;

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        balanceView.setText(
                                "Saldo: " + balance + " HAM\n"
                        );

                        statusView.setText(
                                "BLOCK HAM BERHASIL!"
                        );

                        miningView.setText(
                                "Block: " + blockIndex +
                                "\nReward: " + REWARD +
                                " HAM\n\nHash:\n" +
                                resultHash
                        );
                    }
                });

                return;
            }

            nonce++;
        }
    }

    private String randomHex(int size) {

        SecureRandom random = new SecureRandom();

        byte[] data = new byte[size];
        random.nextBytes(data);

        StringBuilder result = new StringBuilder();

        for (byte b : data) {
            result.append(String.format("%02x", b & 255));
        }

        return result.toString();
    }

    private String sha256(String input) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes =
                    md.digest(
                            input.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(
                        String.format("%02x", b & 255)
                );
            }

            return result.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String repeat(String text, int count) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < count; i++) {
            result.append(text);
        }

        return result.toString();
    }

    @Override
    protected void onDestroy() {

        mining = false;

        if (miningThread != null) {
            miningThread.interrupt();
        }

        super.onDestroy();
    }
}

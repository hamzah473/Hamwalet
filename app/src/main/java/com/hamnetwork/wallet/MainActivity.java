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
    private Button miningButton;

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

            publicKey = prefs.getString(
                    "public_key",
                    sha256(privateKey)
            );

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

        Button copyButton = new Button(this);
        copyButton.setText("SALIN ALAMAT");

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager cm =
                        (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);

                cm.setPrimaryClip(
                        ClipData.newPlainText(
                                "HAM Address",
                                address
                        )
                );

                statusView.setText("Alamat HAM disalin");
            }
        });

        miningButton = new Button(this);
        miningButton.setText("MULAI MINING");

        miningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mining) {
                    startMining();
                } else {
                    stopMining();
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
        layout.addView(copyButton);
        layout.addView(miningButton);
        layout.addView(miningView);

        setContentView(layout);
    }

    private void updateScreen() {

        addressView.setText(
                "\nAlamat HAM:\n" +
                address +
                "\n"
        );

        balanceView.setText(
                "Saldo: " +
                balance +
                " HAM\n"
        );

        if (mining) {

            statusView.setText("MINING HAM AKTIF");
            miningButton.setText("BERHENTI MINING");

        } else {

            statusView.setText("Wallet HAM siap");
            miningButton.setText("MULAI MINING");
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

        handler.post(new Runnable() {
            @Override
            public void run() {

                statusView.setText("Mining dihentikan");
                miningButton.setText("MULAI MINING");
            }
        });
    }

    private void mineBlock() {

        int index = blockIndex + 1;

        String prev;

        if (blockIndex < 0) {
            prev = repeat("0", 64);
        } else {
            prev = previousHash;
        }

        long timestamp =
                System.currentTimeMillis() / 1000L;

        String data =
                "HAM mining reward " +
                REWARD +
                " to " +
                address;

        long nonce = 0;

        String target = repeat("0", DIFFICULTY);

        while (mining) {

            String hash = sha256(
                    "" +
                    index +
                    prev +
                    timestamp +
                    data +
                    nonce
            );

            if (hash.startsWith(target)) {

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
                                "Saldo: " +
                                balance +
                                " HAM\n"
                        );

                        statusView.setText(
                                "BLOCK HAM BERHASIL!"
                        );

                        miningView.setText(
                                "Block: " +
                                blockIndex +
                                "\nReward: " +
                                REWARD +
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

            result.append(
                    String.format(
                            "%02x",
                            b & 255
                    )
            );
        }

        return result.toString();
    }

    private String sha256(String input) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes =
                    md.digest(
                            input.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            for (byte b : bytes) {

                result.append(
                        String.format(
                                "%02x",
                                b & 255
                        )
                );
            }

            return result.toString();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private String repeat(String text, int count) {

        StringBuilder result =
                new StringBuilder();

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

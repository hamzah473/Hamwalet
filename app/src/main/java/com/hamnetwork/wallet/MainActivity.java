
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

import org.json.JSONArray;
import org.json.JSONObject;

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
    private TextView hashRateView;
    private TextView blockchainView;
    private TextView lastBlockView;
    private Button miningButton;

    private SharedPreferences prefs;

    private String privateKey;
    private String publicKey;
    private String address;

    private long balance;
    private int blockIndex;
    private String previousHash;

    private long totalHashes;

    private volatile boolean mining = false;
    private Thread miningThread;

    private final Handler handler = new Handler();

    private long currentHashes = 0;
    private long currentNonce = 0;
    private long miningStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "ham_wallet",
                MODE_PRIVATE
        );

        loadWallet();
        buildInterface();
        updateScreen();
    }

    private void loadWallet() {

        privateKey =
                prefs.getString(
                        "private_key",
                        null
                );

        if (privateKey == null) {

            privateKey = randomHex(32);

            publicKey =
                    sha256(privateKey);

            address =
                    "HAM1" +
                    sha256(publicKey)
                            .substring(0, 40);

            balance = 0;
            blockIndex = -1;
            previousHash = "";
            totalHashes = 0;

            prefs.edit()
                    .putString(
                            "private_key",
                            privateKey
                    )
                    .putString(
                            "public_key",
                            publicKey
                    )
                    .putString(
                            "address",
                            address
                    )
                    .putLong(
                            "balance",
                            balance
                    )
                    .putInt(
                            "block_index",
                            blockIndex
                    )
                    .putString(
                            "previous_hash",
                            previousHash
                    )
                    .putLong(
                            "total_hashes",
                            totalHashes
                    )
                    .putString(
                            "blockchain",
                            "[]"
                    )
                    .apply();

        } else {

            publicKey =
                    prefs.getString(
                            "public_key",
                            sha256(privateKey)
                    );

            address =
                    prefs.getString(
                            "address",
                            "HAM1" +
                            sha256(publicKey)
                                    .substring(0, 40)
                    );

            balance =
                    prefs.getLong(
                            "balance",
                            0
                    );

            blockIndex =
                    prefs.getInt(
                            "block_index",
                            -1
                    );

            previousHash =
                    prefs.getString(
                            "previous_hash",
                            ""
                    );

            totalHashes =
                    prefs.getLong(
                            "total_hashes",
                            0
                    );
        }
    }

    private void buildInterface() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setGravity(
                Gravity.CENTER
        );

        layout.setPadding(
                25,
                25,
                25,
                25
        );

        TextView title =
                new TextView(this);

        title.setText(
                "HAM WALLET"
        );

        title.setTextSize(28);

        title.setGravity(
                Gravity.CENTER
        );

        statusView =
                new TextView(this);

        statusView.setTextSize(18);

        statusView.setGravity(
                Gravity.CENTER
        );

        addressView =
                new TextView(this);

        addressView.setTextSize(13);

        addressView.setGravity(
                Gravity.CENTER
        );

        balanceView =
                new TextView(this);

        balanceView.setTextSize(22);

        balanceView.setGravity(
                Gravity.CENTER
        );

        hashRateView =
                new TextView(this);

        hashRateView.setTextSize(17);

        hashRateView.setGravity(
                Gravity.CENTER
        );

        miningView =
                new TextView(this);

        miningView.setTextSize(15);

        miningView.setGravity(
                Gravity.CENTER
        );

        blockchainView =
                new TextView(this);

        blockchainView.setTextSize(15);

        blockchainView.setGravity(
                Gravity.CENTER
        );

        lastBlockView =
                new TextView(this);

        lastBlockView.setTextSize(13);

        lastBlockView.setGravity(
                Gravity.CENTER
        );

        Button copyButton =
                new Button(this);

        copyButton.setText(
                "SALIN ALAMAT"
        );

        copyButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        ClipboardManager cm =
                                (ClipboardManager)
                                getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                );

                        cm.setPrimaryClip(
                                ClipData.newPlainText(
                                        "HAM Address",
                                        address
                                )
                        );

                        statusView.setText(
                                "Alamat HAM disalin"
                        );
                    }
                }
        );

        miningButton =
                new Button(this);

        miningButton.setText(
                "MULAI MINING"
        );

        miningButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (mining) {

                            stopMining();

                        } else {

                            startMining();
                        }
                    }
                }
        );

        layout.addView(title);
        layout.addView(statusView);
        layout.addView(addressView);
        layout.addView(balanceView);
        layout.addView(hashRateView);
        layout.addView(miningView);
        layout.addView(blockchainView);
        layout.addView(lastBlockView);
        layout.addView(copyButton);
        layout.addView(miningButton);

        setContentView(layout);
    }

    private void updateScreen() {

        addressView.setText(
                "Alamat HAM:\n" +
                address
        );

        balanceView.setText(
                "Saldo: " +
                balance +
                " HAM"
        );

        long elapsed =
                System.currentTimeMillis()
                - miningStartTime;

        double hashRate = 0;

        if (mining && elapsed > 0) {

            hashRate =
                    currentHashes /
                    (elapsed / 1000.0);
        }

        hashRateView.setText(
                "Hashrate: " +
                formatHashrate(hashRate) +
                "\nHash dihitung: " +
                totalHashes +
                "\nNonce: " +
                currentNonce
        );

        int nextBlock =
                blockIndex + 1;

        miningView.setText(
                "Block sedang ditambang: #" +
                nextBlock +
                "\nKesulitan: " +
                DIFFICULTY +
                "\nReward: " +
                REWARD +
                " HAM"
        );

        int blocks =
                getBlockchainCount();

        blockchainView.setText(
                "Blockchain lokal: " +
                blocks +
                " block"
        );

        if (blockIndex >= 0) {

            lastBlockView.setText(
                    "Block terakhir: #" +
                    blockIndex +
                    "\nHash:\n" +
                    previousHash
            );

        } else {

            lastBlockView.setText(
                    "Belum ada block HAM"
            );
        }

        if (mining) {

            statusView.setText(
                    "⛏ MINING HAM AKTIF"
            );

            miningButton.setText(
                    "BERHENTI MINING"
            );

        } else {

            statusView.setText(
                    "Wallet HAM siap"
            );

            miningButton.setText(
                    "MULAI MINING"
            );
        }
    }

    private void startMining() {

        if (mining) {
            return;
        }

        mining = true;

        currentHashes = 0;
        currentNonce = 0;

        miningStartTime =
                System.currentTimeMillis();

        updateScreen();

        miningThread =
                new Thread(
                        new Runnable() {

                            @Override
                            public void run() {

                                mineLoop();
                            }
                        }
                );

        miningThread.start();

        startScreenUpdater();
    }

    private void stopMining() {

        mining = false;

        if (miningThread != null) {

            miningThread.interrupt();
        }

        handler.post(
                new Runnable() {

                    @Override
                    public void run() {

                        updateScreen();
                    }
                }
        );
    }

    private void mineLoop() {

        while (mining) {

            mineBlock();
        }
    }

    private void mineBlock() {

        int index =
                blockIndex + 1;

        String prev;

        if (blockIndex < 0) {

            prev =
                    repeat("0", 64);

        } else {

            prev =
                    previousHash;
        }

        long timestamp =
                System.currentTimeMillis()
                / 1000L;

        String data =
                "HAM mining reward " +
                REWARD +
                " to " +
                address;

        long nonce = 0;

        String target =
                repeat(
                        "0",
                        DIFFICULTY
                );

        currentHashes = 0;

        miningStartTime =
                System.currentTimeMillis();

        while (mining) {

            currentNonce =
                    nonce;

            String hash =
                    sha256(
                            "" +
                            index +
                            prev +
                            timestamp +
                            data +
                            nonce
                    );

            nonce++;

            currentHashes++;
            totalHashes++;

            if (hash.startsWith(target)) {

                saveBlock(
                        index,
                        prev,
                        timestamp,
                        data,
                        nonce - 1,
                        hash
                );

                return;
            }
        }
    }

    private synchronized void saveBlock(
            int index,
            String prev,
            long timestamp,
            String data,
            long nonce,
            String hash
    ) {

        try {

            String chainText =
                    prefs.getString(
                            "blockchain",
                            "[]"
                    );

            JSONArray chain =
                    new JSONArray(chainText);

            JSONObject block =
                    new JSONObject();

            block.put(
                    "index",
                    index
            );

            block.put(
                    "previous_hash",
                    prev
            );

            block.put(
                    "timestamp",
                    timestamp
            );

            block.put(
                    "data",
                    data
            );

            block.put(
                    "nonce",
                    nonce
            );

            block.put(
                    "hash",
                    hash
            );

            block.put(
                    "reward",
                    REWARD
            );

            block.put(
                    "miner",
                    address
            );

            chain.put(block);

            blockIndex =
                    index;

            previousHash =
                    hash;

            balance += REWARD;

            prefs.edit()
                    .putLong(
                            "balance",
                            balance
                    )
                    .putInt(
                            "block_index",
                            blockIndex
                    )
                    .putString(
                            "previous_hash",
                            previousHash
                    )
                    .putLong(
                            "total_hashes",
                            totalHashes
                    )
                    .putString(
                            "blockchain",
                            chain.toString()
                    )
                    .apply();

            final long foundNonce =
                    nonce;

            handler.post(
                    new Runnable() {

                        @Override
                        public void run() {

                            statusView.setText(
                                    "✅ BLOCK BERHASIL!"
                            );

                            miningView.setText(
                                    "Block #" +
                                    blockIndex +
                                    " berhasil\n" +
                                    "Reward: " +
                                    REWARD +
                                    " HAM\n" +
                                    "Nonce: " +
                                    foundNonce
                            );

                            updateScreen();
                        }
                    }
            );

        } catch (Exception e) {

            handler.post(
                    new Runnable() {

                        @Override
                        public void run() {

                            statusView.setText(
                                    "Error blockchain: " +
                                    e.getMessage()
                            );
                        }
                    }
            );
        }
    }

    private int getBlockchainCount() {

        try {

            String text =
                    prefs.getString(
                            "blockchain",
                            "[]"
                    );

            JSONArray chain =
                    new JSONArray(text);

            return chain.length();

        } catch (Exception e) {

            return 0;
        }
    }

    private String formatHashrate(
            double rate
    ) {

        if (rate >= 1000000) {

            return String.format(
                    "%.2f MH/s",
                    rate / 1000000.0
            );

        } else if (rate >= 1000) {

            return String.format(
                    "%.2f KH/s",
                    rate / 1000.0
            );

        } else {

            return String.format(
                    "%.2f H/s",
                    rate
            );
        }
    }

    private String randomHex(
            int size
    ) {

        SecureRandom random =
                new SecureRandom();

        byte[] data =
                new byte[size];

        random.nextBytes(data);

        StringBuilder result =
                new StringBuilder();

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

    private String sha256(
            String input
    ) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

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

    private String repeat(
            String text,
            int count
    ) {

        StringBuilder result =
                new StringBuilder();

        for (int i = 0; i < count; i++) {

            result.append(text);
        }

        return result.toString();
    }

    private void startScreenUpdater() {

        handler.post(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!mining) {
                            return;
                        }

                        updateScreen();

                        handler.postDelayed(
                                this,
                                500
                        );
                    }
                }
        );
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

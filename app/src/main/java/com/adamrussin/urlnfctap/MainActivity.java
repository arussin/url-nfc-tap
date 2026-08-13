package com.adamrussin.urlnfctap;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String SAVED_PRIMARY_SELECTED = "primary_selected";
    private static final String SAVED_SHARING_ENABLED = "sharing_enabled";

    private LinearLayout statusPanel;
    private TextView statusTitle;
    private TextView statusDetail;
    private TextView currentLabel;
    private TextView currentUrl;
    private Button primaryButton;
    private Button secondaryButton;
    private Button stopSharingButton;
    private Button nfcSettingsButton;

    private NfcAdapter nfcAdapter;
    private CardEmulation cardEmulation;
    private boolean primarySelected = true;
    private boolean sharingEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        bindViews();
        configureNfc();
        configureActions();

        primarySelected = savedInstanceState == null
                || savedInstanceState.getBoolean(SAVED_PRIMARY_SELECTED, true);
        sharingEnabled = savedInstanceState == null
                || savedInstanceState.getBoolean(SAVED_SHARING_ENABLED, true);
        selectDestination(primarySelected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearLegacyForegroundNfcPreference();
        applySharingState();
        refreshNfcStatus();
    }

    @Override
    protected void onPause() {
        ShareState.setActive(false);
        clearLegacyForegroundNfcPreference();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            selectDestination(true);
            sharingEnabled = true;
            applySharingState();
            refreshNfcStatus();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVED_PRIMARY_SELECTED, primarySelected);
        outState.putBoolean(SAVED_SHARING_ENABLED, sharingEnabled);
        super.onSaveInstanceState(outState);
    }

    private void bindViews() {
        statusPanel = findViewById(R.id.status_panel);
        statusTitle = findViewById(R.id.status_title);
        statusDetail = findViewById(R.id.status_detail);
        currentLabel = findViewById(R.id.current_label);
        currentUrl = findViewById(R.id.current_url);
        primaryButton = findViewById(R.id.primary_button);
        secondaryButton = findViewById(R.id.secondary_button);
        stopSharingButton = findViewById(R.id.stop_sharing_button);
        nfcSettingsButton = findViewById(R.id.nfc_settings_button);

        primaryButton.setText(BuildConfig.PRIMARY_LABEL);
        secondaryButton.setText(BuildConfig.SECONDARY_LABEL);
    }

    private void configureNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            cardEmulation = CardEmulation.getInstance(nfcAdapter);
        }
    }

    private void configureActions() {
        primaryButton.setOnClickListener(view -> selectDestination(true));
        secondaryButton.setOnClickListener(view -> selectDestination(false));
        stopSharingButton.setOnClickListener(view -> {
            sharingEnabled = !sharingEnabled;
            applySharingState();
            refreshNfcStatus();
        });
        nfcSettingsButton.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            } catch (RuntimeException error) {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
    }

    private void selectDestination(boolean selectPrimary) {
        primarySelected = selectPrimary;
        String label = selectPrimary ? BuildConfig.PRIMARY_LABEL : BuildConfig.SECONDARY_LABEL;
        String url = selectPrimary ? BuildConfig.PRIMARY_URL : BuildConfig.SECONDARY_URL;

        ShareState.select(this, url);
        currentLabel.setText(label);
        currentUrl.setText(url);
        primaryButton.setSelected(selectPrimary);
        secondaryButton.setSelected(!selectPrimary);
        primaryButton.setContentDescription(BuildConfig.PRIMARY_LABEL
                + (selectPrimary ? ", selected" : ", tap to select"));
        secondaryButton.setContentDescription(BuildConfig.SECONDARY_LABEL
                + (!selectPrimary ? ", selected" : ", tap to select"));
    }

    private void applySharingState() {
        ShareState.setActive(sharingEnabled);
        stopSharingButton.setText(sharingEnabled
                ? R.string.stop_sharing
                : R.string.start_sharing);
        if (!sharingEnabled) {
            clearLegacyForegroundNfcPreference();
        }
    }

    private void clearLegacyForegroundNfcPreference() {
        if (cardEmulation == null) return;
        try {
            // Version 1.0.0 requested foreground HCE priority. Clear any preference it left
            // behind, then rely on normal AID routing so Google Wallet remains the default.
            cardEmulation.unsetPreferredService(this);
        } catch (RuntimeException ignored) {
            // There may be no legacy preference to clear.
        }
    }

    private void refreshNfcStatus() {
        boolean hceSupported = nfcAdapter != null
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);

        if (!hceSupported) {
            showWarning(R.string.unsupported_title, R.string.unsupported_detail, false);
        } else if (!nfcAdapter.isEnabled()) {
            showWarning(R.string.nfc_off_title, R.string.nfc_off_detail, true);
        } else if (!sharingEnabled) {
            statusPanel.setBackgroundResource(R.drawable.panel_background);
            statusTitle.setText(R.string.paused_title);
            statusTitle.setTextColor(getColor(R.color.text_secondary));
            statusDetail.setText(R.string.paused_detail);
            nfcSettingsButton.setVisibility(View.GONE);
        } else {
            statusPanel.setBackgroundResource(R.drawable.status_active);
            statusTitle.setText(R.string.ready_title);
            statusTitle.setTextColor(getColor(R.color.success_text));
            statusDetail.setText(R.string.ready_detail);
            nfcSettingsButton.setVisibility(View.GONE);
        }
    }

    private void showWarning(int title, int detail, boolean showSettings) {
        statusPanel.setBackgroundResource(R.drawable.status_warning);
        statusTitle.setText(title);
        statusTitle.setTextColor(getColor(R.color.warning_text));
        statusDetail.setText(detail);
        nfcSettingsButton.setVisibility(showSettings ? View.VISIBLE : View.GONE);
    }
}

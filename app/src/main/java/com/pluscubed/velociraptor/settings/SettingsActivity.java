package com.pluscubed.velociraptor.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.pluscubed.velociraptor.AppDetectionService;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.settings.appselection.AppSelectionActivity;
import com.pluscubed.velociraptor.ui.LimitService;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {
    public static final int PENDING_SERVICE = 4;
    public static final int PENDING_SERVICE_CLOSE = 3;
    public static final int PENDING_SETTINGS = 2;
    public static final int NOTIFICATION_CONTROLS = 42;
    public static final String[] SUBSCRIPTIONS = new String[]{"sub_1", "sub_2"};
    public static final String[] PURCHASES = new String[]{"badge_1", "badge_2", "badge_3", "badge_4", "badge_5"};
    public static final String OSM_COVERAGE_URL = "http://product.itoworld.com/map/124";
    private static final int REQUEST_LOCATION = 105;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    //M Permissions
    @BindView(R.id.card_m_permissions)
    View mPermCard;
    @BindView(R.id.button_floating_enabled)
    Button enableFloatingButton;
    @BindView(R.id.image_floating_enabled)
    ImageView enabledFloatingImage;
    @BindView(R.id.button_location_enabled)
    Button enableLocationButton;
    @BindView(R.id.image_location_enabled)
    ImageView enabledLocationImage;

    //Activation
    @BindView(R.id.linear_auto_display)
    View appDetectionContainer;
    @BindView(R.id.switch_auto_display)
    SwitchCompat appDetectionSwitch;


    @BindView(R.id.linear_auto_display_options)
    ViewGroup appDetectionOptionsContainer;

    @BindView(R.id.image_service_enabled)
    ImageView enabledServiceImage;
    @BindView(R.id.button_enable_service)
    Button enableServiceButton;

    @BindView(R.id.linear_app_selection)
    ViewGroup appSelectionContainer;
    @BindView(R.id.button_app_selection)
    Button appSelectionButton;

    @BindView(R.id.linear_android_auto)
    ViewGroup androidAutoContainer;
    @BindView(R.id.switch_android_auto)
    SwitchCompat androidAutoSwitch;

    @BindView(R.id.linear_gmaps_navigation)
    ViewGroup gmapsOnlyNavigationContainer;
    @BindView(R.id.switch_gmaps_navigation)
    SwitchCompat gmapsOnlyNavigationSwitch;

    @BindViews({R.id.image_app_selection, R.id.image_android_auto, R.id.image_gmaps_navigation})
    List<ImageView> autoDisplayIcons;

    @BindView(R.id.switch_notif_controls)
    View notifControlsContainer;

    @BindView(R.id.linear_tip_qs)
    View qsTipView;

    //General
    @BindView(R.id.switch_speedometer)
    SwitchCompat showSpeedometerSwitch;

    @BindView(R.id.switch_beep)
    SwitchCompat beepSwitch;
    @BindView(R.id.button_test_beep)
    Button testBeepButton;

    @BindView(R.id.linear_tolerance)
    LinearLayout toleranceView;
    @BindView(R.id.text_overview_tolerance)
    TextView toleranceOverview;

    @BindView(R.id.linear_size)
    LinearLayout sizeView;
    @BindView(R.id.text_overview_size)
    TextView sizeOverview;

    @BindView(R.id.linear_opacity)
    LinearLayout opacityView;
    @BindView(R.id.text_overview_opacity)
    TextView opacityOverview;

    @BindView(R.id.spinner_unit)
    Spinner unitSpinner;
    @BindView(R.id.spinner_style)
    Spinner styleSpinner;

    //OpenStreetMap edit
    @BindView(R.id.check_coverage)
    LinearLayout checkCoverageView;

    //Advanced
    @BindView(R.id.switch_debugging)
    SwitchCompat debuggingSwitch;


    private NotificationManager notificationManager;
    private BillingProcessor billingProcessor;

    @Override
    protected void onDestroy() {
        if (billingProcessor != null) {
            billingProcessor.release();
        }
        super.onDestroy();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermCard.setVisibility(View.GONE);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            qsTipView.setVisibility(View.GONE);
        }

        checkCoverageView.setOnClickListener(v -> {
            if (Utils.isLocationPermissionGranted(SettingsActivity.this)) {
                FusedLocationProviderClient fusedLocationProvider =
                        LocationServices.getFusedLocationProviderClient(SettingsActivity.this);
                fusedLocationProvider
                        .getLastLocation()
                        .addOnCompleteListener(SettingsActivity.this, task -> {
                            String uriString = OSM_COVERAGE_URL;
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location lastLocation = task.getResult();
                                uriString += "?lon=" + lastLocation.getLongitude() + "&lat=" + lastLocation.getLatitude() + "&zoom=12";
                            }
                            openCoverageMap(uriString);
                        });
            } else {
                openCoverageMap(OSM_COVERAGE_URL);
            }
        });

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifControlsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, LimitService.class);
                intent.putExtra(LimitService.EXTRA_NOTIF_START, true);
                PendingIntent pending = PendingIntent.getService(SettingsActivity.this,
                        PENDING_SERVICE, intent, PendingIntent.FLAG_CANCEL_CURRENT);

                Intent intentClose = new Intent(SettingsActivity.this, LimitService.class);
                intentClose.putExtra(LimitService.EXTRA_NOTIF_CLOSE, true);
                PendingIntent pendingClose = PendingIntent.getService(SettingsActivity.this,
                        PENDING_SERVICE_CLOSE, intentClose, PendingIntent.FLAG_CANCEL_CURRENT);

                Intent settings = new Intent(SettingsActivity.this, SettingsActivity.class);
                PendingIntent settingsIntent = PendingIntent.getActivity(SettingsActivity.this,
                        PENDING_SETTINGS, settings, PendingIntent.FLAG_CANCEL_CURRENT);

                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(SettingsActivity.this)
                                .setSmallIcon(R.drawable.ic_speedometer_notif)
                                .setContentTitle(getString(R.string.controls_notif_title))
                                .setContentText(getString(R.string.controls_notif_desc))
                                .addAction(0, getString(R.string.show), pending)
                                .addAction(0, getString(R.string.hide), pendingClose)
                                .setDeleteIntent(pendingClose)
                                .setContentIntent(settingsIntent);
                Notification notification = builder.build();
                notificationManager.notify(NOTIFICATION_CONTROLS, notification);
            }
        });


        appSelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AppSelectionActivity.class));
            }
        });

        appDetectionSwitch.setChecked(PrefUtils.isAutoDisplayEnabled(this));
        appDetectionContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appDetectionSwitch.toggle();
                boolean autoDisplayEnabled = appDetectionSwitch.isChecked();
                PrefUtils.setAutoDisplay(SettingsActivity.this, autoDisplayEnabled);
                updateAppDetectionOptionStates();
            }
        });

        enableServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(enableServiceButton, R.string.open_settings_failed_accessibility, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        enableFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //Open the current default browswer App Info page
                    openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID);
                } catch (ActivityNotFoundException ignored) {
                    Snackbar.make(enableFloatingButton, R.string.open_settings_failed_overlay, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        enableLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            }
        });

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_text, new String[]{"mph", "km/h"});
        unitAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (PrefUtils.getUseMetric(SettingsActivity.this) != (position == 1)) {
                    PrefUtils.setUseMetric(SettingsActivity.this, position == 1);
                    unitSpinner.setDropDownVerticalOffset(
                            Utils.convertDpToPx(SettingsActivity.this, unitSpinner.getSelectedItemPosition() * -48));

                    Utils.updateFloatingServicePrefs(SettingsActivity.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        unitSpinner.setSelection(PrefUtils.getUseMetric(this) ? 1 : 0);
        unitSpinner.setDropDownVerticalOffset(Utils.convertDpToPx(this, unitSpinner.getSelectedItemPosition() * -48));

        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_text,
                new String[]{getString(R.string.united_states), getString(R.string.international)});
        styleAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        styleSpinner.setAdapter(styleAdapter);
        styleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != PrefUtils.getSignStyle(SettingsActivity.this)) {
                    PrefUtils.setSignStyle(SettingsActivity.this, position);
                    styleSpinner.setDropDownVerticalOffset(
                            Utils.convertDpToPx(SettingsActivity.this, styleSpinner.getSelectedItemPosition() * -48));

                    Utils.updateFloatingServicePrefs(SettingsActivity.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        styleSpinner.setSelection(PrefUtils.getSignStyle(this));
        styleSpinner.setDropDownVerticalOffset(Utils.convertDpToPx(this, styleSpinner.getSelectedItemPosition() * -48));

        toleranceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ToleranceDialogFragment().show(getFragmentManager(), "dialog_tolerance");
            }
        });

        sizeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SizeDialogFragment().show(getFragmentManager(), "dialog_size");
            }
        });

        opacityView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new OpacityDialogFragment().show(getFragmentManager(), "dialog_opacity");
            }
        });

        showSpeedometerSwitch.setChecked(PrefUtils.getShowSpeedometer(this));
        ((View) showSpeedometerSwitch.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeedometerSwitch.setChecked(!showSpeedometerSwitch.isChecked());

                PrefUtils.setShowSpeedometer(SettingsActivity.this, showSpeedometerSwitch.isChecked());

                Utils.updateFloatingServicePrefs(SettingsActivity.this);
            }
        });

        debuggingSwitch.setChecked(PrefUtils.isDebuggingEnabled(this));
        ((View) debuggingSwitch.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debuggingSwitch.setChecked(!debuggingSwitch.isChecked());

                PrefUtils.setDebugging(SettingsActivity.this, debuggingSwitch.isChecked());

                Utils.updateFloatingServicePrefs(SettingsActivity.this);
            }
        });

        beepSwitch.setChecked(PrefUtils.isBeepAlertEnabled(this));
        beepSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefUtils.setBeepAlertEnabled(SettingsActivity.this, beepSwitch.isChecked());
            }
        });
        testBeepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.playBeeps();
            }
        });

        androidAutoSwitch.setChecked(PrefUtils.isAutoIntegrationEnabled(this));
        androidAutoContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.FLAVOR.equals("play")) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.auto_not_available, Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (!androidAutoSwitch.isEnabled()) {
                    return;
                }
                androidAutoSwitch.toggle();
                boolean checked = androidAutoSwitch.isChecked();
                if (checked) {
                    new MaterialDialog.Builder(SettingsActivity.this)
                            .content(R.string.android_auto_instruction_dialog)
                            .positiveText(android.R.string.ok)
                            .show();
                }

                PrefUtils.setAutoIntegrationEnabled(SettingsActivity.this, checked);
            }
        });

        gmapsOnlyNavigationSwitch.setChecked(isNotificationAccessGranted() && PrefUtils.isGmapsOnlyInNavigation(this));
        gmapsOnlyNavigationContainer.setOnClickListener(v -> {
            if (!gmapsOnlyNavigationSwitch.isEnabled()) {
                return;
            }

            boolean accessGranted = isNotificationAccessGranted();
            if (accessGranted) {
                gmapsOnlyNavigationSwitch.toggle();
                PrefUtils.setGmapsOnlyInNavigation(SettingsActivity.this, gmapsOnlyNavigationSwitch.isChecked());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                new MaterialDialog.Builder(SettingsActivity.this)
                        .content(R.string.gmaps_only_nav_notif_access)
                        .positiveText(R.string.grant)
                        .onPositive((dialog, which) -> {
                            try {
                                String settingsAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ?
                                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS : "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
                                Intent intent = new Intent(settingsAction);
                                startActivity(intent);
                            } catch (ActivityNotFoundException ignored) {
                                Snackbar.make(enableFloatingButton, R.string.open_settings_failed_notif_access, Snackbar.LENGTH_LONG).show();
                            }
                        })
                        .show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.gmaps_only_nav_jellybean, Snackbar.LENGTH_LONG).show();
            }
        });

        invalidateStates();

        if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this) &&
                !PrefUtils.isFirstRun(this)) {
            showChangelog();
        }

        billingProcessor = new BillingProcessor(this, getString(R.string.play_license_key), new BillingProcessor.IBillingHandler() {
            @Override
            public void onProductPurchased(String productId, TransactionDetails details) {
                PrefUtils.setSupported(SettingsActivity.this, true);
                if (Arrays.asList(PURCHASES).contains(productId))
                    billingProcessor.consumePurchase(productId);
            }

            @Override
            public void onPurchaseHistoryRestored() {

            }

            @Override
            public void onBillingError(int errorCode, Throwable error) {
                if (errorCode != 110) {
                    Snackbar.make(findViewById(android.R.id.content), "Billing error: code = " + errorCode + ", error: " +
                            (error != null ? error.getMessage() : "?"), Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onBillingInitialized() {
                billingProcessor.loadOwnedPurchasesFromGoogle();
            }
        });

        PrefUtils.setFirstRun(this, false);
        PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE);
    }

    private void openCoverageMap(String uriString) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(uriString));
        intent.setAction(Intent.ACTION_VIEW);
        try {
            SettingsActivity.this.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(enableFloatingButton, R.string.open_coverage_map_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean isNotificationAccessGranted() {
        return NotificationManagerCompat.getEnabledListenerPackages(SettingsActivity.this).contains(BuildConfig.APPLICATION_ID);
    }

    public void showSupportDialog() {
        String content = getString(BuildConfig.FLAVOR.equals("play") ? R.string.support_dev_dialog : R.string.support_dev_dialog_notplay);
        if (PrefUtils.hasSupported(this) || !billingProcessor.listOwnedSubscriptions().isEmpty()) {
            content += "\n\n\uD83C\uDF89 " + getString(R.string.support_dev_dialog_badge) + " \uD83C\uDF89";
        }
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .icon(Utils.getVectorDrawableCompat(this, R.drawable.ic_favorite_black_24dp))
                .title(R.string.support_development)
                .content(Html.fromHtml(content));

        if (BuildConfig.FLAVOR.equals("play")) {
            if (!billingProcessor.isInitialized()) {
                Snackbar.make(findViewById(android.R.id.content), R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show();
                return;
            }
            final List<SkuDetails> purchaseListingDetails = billingProcessor.getPurchaseListingDetails(new ArrayList<>(Arrays.asList(PURCHASES)));
            final List<SkuDetails> subscriptionListingDetails = billingProcessor.getSubscriptionListingDetails(new ArrayList<>(Arrays.asList(SUBSCRIPTIONS)));

            if (purchaseListingDetails == null || purchaseListingDetails.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show();
                return;
            }

            purchaseListingDetails.addAll(0, subscriptionListingDetails);

            List<String> purchaseDisplay = new ArrayList<>();
            for (SkuDetails details : purchaseListingDetails) {
                NumberFormat format = NumberFormat.getCurrencyInstance();
                format.setCurrency(Currency.getInstance(details.currency));
                String amount = format.format(details.priceValue);
                if (details.isSubscription)
                    amount = String.format(getString(R.string.per_month), amount);
                else {
                    amount = String.format(getString(R.string.one_time), amount);
                }
                purchaseDisplay.add(amount);
            }

            builder.items(purchaseDisplay)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            SkuDetails skuDetails = purchaseListingDetails.get(which);
                            if (skuDetails.isSubscription) {
                                billingProcessor.subscribe(SettingsActivity.this, skuDetails.productId);
                            } else {
                                billingProcessor.purchase(SettingsActivity.this, skuDetails.productId);
                            }
                        }
                    });
        }

        if (BuildConfig.FLAVOR.equals("full")) {
            builder.positiveText(R.string.dismiss);
        }

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        enableService(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings_support:
                showSupportDialog();
                return true;
            case R.id.menu_settings_about:
                showAboutDialog();
                return true;
            case R.id.menu_settings_changelog:
                showChangelog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.about_dialog_title, BuildConfig.VERSION_NAME))
                .positiveText(R.string.dismiss)
                .content(Html.fromHtml(getString(R.string.about_body)))
                .neutralText(R.string.licenses)
                .onNeutral((dialog, which) -> startActivity(new Intent(SettingsActivity.this, OssLicensesMenuActivity.class)))
                .iconRes(R.mipmap.ic_launcher)
                .show();
    }

    private void showChangelog() {
        ChangelogDialogFragment.newInstance().show(getFragmentManager(), "CHANGELOG_DIALOG");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            invalidateStates();
        }
    }

    private void invalidateStates() {
        boolean permissionGranted = Utils.isLocationPermissionGranted(this);
        enabledLocationImage.setImageResource(permissionGranted ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        enableLocationButton.setEnabled(!permissionGranted);

        @SuppressLint({"NewApi", "LocalSuppress"}) boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        enabledFloatingImage.setImageResource(overlayEnabled ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        enableFloatingButton.setEnabled(!overlayEnabled);

        updateAppDetectionOptionStates();

        String constant = getString(PrefUtils.getUseMetric(this) ? R.string.kmph : R.string.mph,
                String.valueOf(PrefUtils.getSpeedingConstant(this)));
        String percent = getString(R.string.percent, String.valueOf(PrefUtils.getSpeedingPercent(this)));
        String mode = PrefUtils.getToleranceMode(this) ? "+" : getString(R.string.or);
        String overview = getString(R.string.tolerance_desc, percent, mode, constant);
        toleranceOverview.setText(overview);

        String limitSizePercent = getString(R.string.percent, String.valueOf((int) (PrefUtils.getSpeedLimitSize(this) * 100)));
        String speedLimitSize = getString(R.string.size_limit_overview, limitSizePercent);
        String speedometerSizePercent = getString(R.string.percent, String.valueOf((int) (PrefUtils.getSpeedometerSize(this) * 100)));
        String speedometerSize = getString(R.string.size_speedometer_overview, speedometerSizePercent);
        sizeOverview.setText(speedLimitSize + "\n" + speedometerSize);

        opacityOverview.setText(getString(R.string.percent, String.valueOf(PrefUtils.getOpacity(this))));

        if (permissionGranted && overlayEnabled) {
            enableService(true);
        }
    }

    private void updateAppDetectionOptionStates() {
        boolean autoDisplayEnabled = PrefUtils.isAutoDisplayEnabled(this);
        enableDisableAllChildren(autoDisplayEnabled, appDetectionOptionsContainer);

        boolean serviceEnabled = Utils.isAccessibilityServiceEnabled(this, AppDetectionService.class);
        enabledServiceImage.setImageResource(serviceEnabled ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        enabledServiceImage.setAlpha(autoDisplayEnabled ? 1f : 0.7f);
        enableServiceButton.setEnabled(autoDisplayEnabled && !serviceEnabled);

        ButterKnife.apply(autoDisplayIcons, View.ALPHA, (autoDisplayEnabled && serviceEnabled ? 1f : 0.7f));
        enableDisableAllChildren(autoDisplayEnabled && serviceEnabled, appSelectionContainer);
        enableDisableAllChildren(autoDisplayEnabled && serviceEnabled, androidAutoContainer);
        enableDisableAllChildren(autoDisplayEnabled && serviceEnabled, gmapsOnlyNavigationContainer);
    }

    private void enableDisableAllChildren(boolean enable, ViewGroup viewgroup) {
        for (int i = 0; i < viewgroup.getChildCount(); i++) {
            View child = viewgroup.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                enableDisableAllChildren(enable, (ViewGroup) child);
            }
        }
    }

    private void enableService(boolean start) {
        Intent intent = new Intent(this, LimitService.class);
        if (!start) {
            intent.putExtra(LimitService.EXTRA_CLOSE, true);
        }
        startService(intent);
    }

    private void openSettings(String settingsAction, String packageName) {
        Intent intent = new Intent(settingsAction);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }
}

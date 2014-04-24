package com.cardiomood.heartrate.android.ads;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.util.Random;

/**
 * Created by danon on 23.04.2014.
 */
public class AdMobController extends AdListener implements AdsControllerBase {

    private static final int REQUEST_TIMEOUT = 30000;

    private String interstitialId;
    private final String bannerId;
    private final Activity activity;
    private final RelativeLayout container;
    private long lastBannerUpdate;
    private long lastInterstitialUpdate;
    private PreferenceHelper prefHelper;
    private AdView adView;
    private InterstitialAd interstitial;
    private final Handler mHandler;
    private boolean stopped;


    public AdMobController(Activity activity, RelativeLayout container, String bannerId) {
        this(activity, container, bannerId, null);
    }

    public AdMobController(Activity activity, RelativeLayout container, String bannerId, String interstitialId) {
        this.bannerId = bannerId;
        this.activity = activity;
        this.container = container;
        this.prefHelper = new PreferenceHelper(activity.getApplicationContext(), true);
        this.mHandler = new Handler();

        createView(container);
        setInterstitialId(interstitialId);
        lastBannerUpdate = System.currentTimeMillis();
        lastInterstitialUpdate = System.currentTimeMillis();
    }

    @Override
    public void createView(RelativeLayout layout) {
        if(isAddsDisabled())
            return;
        final AdRequest adRequest = getAdRequest();

        adView = new AdView(activity);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(bannerId);
        RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        adParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        adParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        adView.setAdListener(this);

        layout.addView(adView, adParams);
        adView.loadAd(adRequest);
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("BA419EB2EB88131929985C0BB86319CC")
                .build();
    }

    @Override
    public void showBanner(boolean show) {
        if (adView == null)
            return;
        adView.setVisibility((show) ? View.VISIBLE : View.GONE);
        if (show && (System.currentTimeMillis() - lastBannerUpdate > REQUEST_TIMEOUT)) {
            lastBannerUpdate = System.currentTimeMillis();
            adView.loadAd(getAdRequest());
        }
    }

    @Override
    public void onStart() {
        stopped = false;
    }

    @Override
    public void onDestroy() {
        if (adView != null)
            adView.destroy();
        interstitial = null;
    }

    @Override
    public void onResume() {
        if (adView != null)
            adView.resume();
    }

    @Override
    public void onStop() {
        stopped = true;
    }

    @Override
    public void onPause() {
        if (adView != null)
            adView.pause();
    }

    @Override
    public void onAdClosed() {
        super.onAdClosed();
    }

    @Override
    public void onAdFailedToLoad(int errorCode) {
        super.onAdFailedToLoad(errorCode);
    }

    @Override
    public void onAdLeftApplication() {
        super.onAdLeftApplication();
    }

    @Override
    public void onAdOpened() {
        super.onAdOpened();
    }

    @Override
    public void onAdLoaded() {
        super.onAdLoaded();
    }

    public void setInterstitialId(String interstitialId) {
        if (interstitialId == null) {
            this.interstitialId = interstitialId;
            interstitial = null;
            return;
        }
        if (!interstitialId.equals(this.interstitialId)) {
            this.interstitialId = interstitialId;
            if (isAddsDisabled())
                return;
            // Create the interstitial.
            interstitial = new InterstitialAd(activity);
            interstitial.setAdUnitId(interstitialId);
            interstitial.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            reloadInterstitial();
                        }
                    }, 2*1000*60);
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showInterstitial();
                        }
                    }, new Random().nextInt(1000*120));
                }
            });
            // Begin loading your interstitial.
            interstitial.loadAd(getAdRequest());
        }
    }

    public boolean isAddsDisabled() {
        return prefHelper.getBoolean(ConfigurationConstants.DISABLE_ADS);
    }

    private void reloadInterstitial() {
        if (interstitial != null) {
            lastInterstitialUpdate = System.currentTimeMillis();
            interstitial.loadAd(getAdRequest());
        }
    }

    public void showInterstitial() {
        if (interstitial == null)
            return;
        if (!stopped && interstitial.isLoaded()) {
            interstitial.show();
        }
        if (System.currentTimeMillis() - lastInterstitialUpdate > REQUEST_TIMEOUT) {
            lastInterstitialUpdate = System.currentTimeMillis();
            interstitial.loadAd(getAdRequest());
        }
    }
}

package io.invertase.googlemobileads;

/*
 * Copyright (c) 2016-present Invertase Limited & Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this library except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.BaseAdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.admanager.AppEventListener;
import io.invertase.googlemobileads.common.ReactNativeAdView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReactNativeGoogleMobileAdsBannerAdViewManager
    extends SimpleViewManager<ReactNativeAdView> {
  private static final String REACT_CLASS = "RNGoogleMobileAdsBannerView";
  private final String EVENT_AD_LOADED = "onAdLoaded";
  private final String EVENT_AD_FAILED_TO_LOAD = "onAdFailedToLoad";
  private final String EVENT_AD_OPENED = "onAdOpened";
  private final String EVENT_AD_CLOSED = "onAdClosed";
  private final String EVENT_SIZE_CHANGE = "onSizeChange";
  private final String EVENT_APP_EVENT = "onAppEvent";
  private final int COMMAND_ID_RECORD_MANUAL_IMPRESSION = 1;

  @Nonnull
  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Nonnull
  @Override
  public ReactNativeAdView createViewInstance(@Nonnull ThemedReactContext themedReactContext) {
    return new ReactNativeAdView(themedReactContext);
  }

  @Override
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    builder.put("onNativeEvent", MapBuilder.of("registrationName", "onNativeEvent"));
    return builder.build();
  }

  @Nullable
  @Override
  public Map<String, Integer> getCommandsMap() {
    return MapBuilder.of("recordManualImpression", COMMAND_ID_RECORD_MANUAL_IMPRESSION);
  }

  @Override
  public void receiveCommand(
      @NonNull ReactNativeAdView reactViewGroup, String commandId, @Nullable ReadableArray args) {
    super.receiveCommand(reactViewGroup, commandId, args);
    int commandIdInt = Integer.parseInt(commandId);

    if (commandIdInt == COMMAND_ID_RECORD_MANUAL_IMPRESSION) {
      BaseAdView adView = reactViewGroup.getContentView();
      if (adView instanceof AdManagerAdView) {
        ((AdManagerAdView) adView).recordManualImpression();
      }
    }
  }

  @ReactProp(name = "unitId")
  public void setUnitId(ReactNativeAdView reactViewGroup, String value) {
    reactViewGroup.setUnitId(value);
    reactViewGroup.setPropsChanged(true);
  }

  @ReactProp(name = "request")
  public void setRequest(ReactNativeAdView reactViewGroup, ReadableMap value) {
    reactViewGroup.setRequest(ReactNativeGoogleMobileAdsCommon.buildAdRequest(value));
    reactViewGroup.setPropsChanged(true);
  }

  @ReactProp(name = "sizes")
  public void setSizes(ReactNativeAdView reactViewGroup, ReadableArray value) {
    List<AdSize> sizeList = new ArrayList<>();
    for (Object size : value.toArrayList()) {
      if (size instanceof String) {
        String sizeString = (String) size;
        sizeList.add(ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, reactViewGroup));
      }
    }

    if (sizeList.size() > 0 && !sizeList.contains(AdSize.FLUID)) {
      AdSize adSize = sizeList.get(0);
      WritableMap payload = Arguments.createMap();
      payload.putDouble("width", adSize.getWidth());
      payload.putDouble("height", adSize.getHeight());
      sendEvent(reactViewGroup, EVENT_SIZE_CHANGE, payload);
    }

    reactViewGroup.setSizes(sizeList);
    reactViewGroup.setPropsChanged(true);
  }

  @ReactProp(name = "manualImpressionsEnabled")
  public void setManualImpressionsEnabled(ReactNativeAdView reactViewGroup, boolean value) {
    reactViewGroup.setManualImpressionsEnabled(value);
    reactViewGroup.setPropsChanged(true);
  }

  @Override
  public void onAfterUpdateTransaction(@NonNull ReactNativeAdView reactViewGroup) {
    super.onAfterUpdateTransaction(reactViewGroup);
    if (reactViewGroup.getPropsChanged()) {
      requestAd(reactViewGroup);
    }
    reactViewGroup.setPropsChanged(false);
  }

  private BaseAdView initAdView(ReactNativeAdView reactViewGroup) {
    BaseAdView oldAdView = reactViewGroup.getContentView();
    if (oldAdView != null) {
      oldAdView.setAdListener(null);
      if (oldAdView instanceof AdManagerAdView) {
        ((AdManagerAdView) oldAdView).setAppEventListener(null);
      }
      oldAdView.destroy();
      reactViewGroup.removeView(oldAdView);
    }
    BaseAdView adView;
    if (ReactNativeGoogleMobileAdsCommon.isAdManagerUnit(reactViewGroup.getUnitId())) {
      // in order to display the debug menu for GAM ads we need the activity context
      // https://github.com/invertase/react-native-google-mobile-ads/issues/188
      adView =
          new AdManagerAdView(
              Objects.requireNonNull(
                  ((ReactContext) reactViewGroup.getContext()).getCurrentActivity()));
    } else {
      adView = new AdView(reactViewGroup.getContext());
    }
    adView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    adView.setAdListener(
      new AdListener() {
        @Override
        public void onAdLoaded() {
          reactViewGroup.requestLayout();

          if(reactViewGroup.getIsFluid()) return;

          AdSize adSize = adView.getAdSize();
          WritableMap payload = Arguments.createMap();
          payload.putDouble("width", adSize.getWidth());
          payload.putDouble("height", adSize.getHeight());

          sendEvent(reactViewGroup, EVENT_AD_LOADED, payload);
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          int errorCode = loadAdError.getCode();
          WritableMap payload = ReactNativeGoogleMobileAdsCommon.errorCodeToMap(errorCode);
          sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
        }

        @Override
        public void onAdOpened() {
          sendEvent(reactViewGroup, EVENT_AD_OPENED, null);
        }

        @Override
        public void onAdClosed() {
          sendEvent(reactViewGroup, EVENT_AD_CLOSED, null);
        }
      }
    );
    if (adView instanceof AdManagerAdView) {
      ((AdManagerAdView) adView)
          .setAppEventListener(
              new AppEventListener() {
                @Override
                public void onAppEvent(@NonNull String name, @Nullable String data) {
                  WritableMap payload = Arguments.createMap();
                  payload.putString("name", name);
                  payload.putString("data", data);
                  sendEvent(reactViewGroup, EVENT_APP_EVENT, payload);
                }
              });
    }
    reactViewGroup.addView(adView);
    return adView;
  }

  private void requestAd(ReactNativeAdView reactViewGroup) {
    String unitId = reactViewGroup.getUnitId();
    List<AdSize> sizes = reactViewGroup.getSizes();
    AdRequest request = reactViewGroup.getRequest();
    Boolean manualImpressionsEnabled = reactViewGroup.getManualImpressionsEnabled();

    if (sizes == null || unitId == null || request == null || manualImpressionsEnabled == null) {
      return;
    }

    BaseAdView adView = initAdView(reactViewGroup);
    adView.setAdUnitId(unitId);

    reactViewGroup.setIsFluid(false);
    if (adView instanceof AdManagerAdView) {
      if (sizes.contains(AdSize.FLUID)) {
        reactViewGroup.setIsFluid(true);
        ((AdManagerAdView) adView).setAdSizes(AdSize.FLUID);
      } else {
        ((AdManagerAdView) adView).setAdSizes(sizes.toArray(new AdSize[0]));
      }
      if (manualImpressionsEnabled) {
        ((AdManagerAdView) adView).setManualImpressionsEnabled(true);
      }
    } else {
      adView.setAdSize(sizes.get(0));
    }

    adView.loadAd(request);
  }

  private void sendEvent(ReactNativeAdView reactViewGroup, String type, WritableMap payload) {
    WritableMap event = Arguments.createMap();
    event.putString("type", type);

    if (payload != null) {
      event.merge(payload);
    }

    ((ThemedReactContext) reactViewGroup.getContext())
        .getJSModule(RCTEventEmitter.class)
        .receiveEvent(reactViewGroup.getId(), "onNativeEvent", event);
  }
}
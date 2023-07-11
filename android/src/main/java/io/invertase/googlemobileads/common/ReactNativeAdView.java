package io.invertase.googlemobileads.common;

import android.content.Context;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import java.util.List;

package io.invertase.googlemobileads;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.BaseAdView;

public class ReactNativeAdView extends FrameLayout {
  private BaseAdView contentView;

  private AdRequest request;
  private List<AdSize> sizes;
  private String unitId;
  private boolean manualImpressionsEnabled;
  private boolean propsChanged;
  private boolean isFluid;

  public LayoutWrapper(Context context) {
    super(context);
  }

  @Override
  public void requestLayout() {
    super.requestLayout();
    post(measureAndLayout);
  }

  public ReactNativeAdView(final Context context) {
    super(context);
  }

  public void setRequest(AdRequest request) {
    this.request = request;
  }

  public AdRequest getRequest() {
    return this.request;
  }

  public void setSizes(List<AdSize> sizes) {
    this.sizes = sizes;
  }

  public List<AdSize> getSizes() {
    return this.sizes;
  }

  public void setUnitId(String unitId) {
    this.unitId = unitId;
  }

  public String getUnitId() {
    return this.unitId;
  }

  public void setManualImpressionsEnabled(boolean manualImpressionsEnabled) {
    this.manualImpressionsEnabled = manualImpressionsEnabled;
  }

  public boolean getManualImpressionsEnabled() {
    return this.manualImpressionsEnabled;
  }

  public void setPropsChanged(boolean propsChanged) {
    this.propsChanged = propsChanged;
  }

  public boolean getPropsChanged() {
    return this.propsChanged;
  }

  public void setIsFluid(boolean isFluid) {
    this.isFluid = isFluid;
  }

  public boolean getIsFluid() {
    return this.isFluid;
  }
  private final Runnable measureAndLayout = () -> {

    BaseAdView adView = getContentView();

    if(adView != null) {
      AdSize adSize = adView.getAdSize();
      int left = adView.getLeft();
      int top = adView.getTop();
      int width = adSize.getWidthInPixels(getContext());
      int height = adSize.getHeightInPixels(getContext());

      measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
      layout(left, top, left + width, top + height);
    } else {
      measure(
        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
      layout(getLeft(), getTop(), getRight(), getBottom());
    }
  };

  public void setContentView(BaseAdView view) {
    contentView = view;
    addView(contentView);
  }

  public BaseAdView getContentView() {
    return contentView;
  }
}

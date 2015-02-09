package com.github.ksoichiro.eclipse.aar.samples.observablescrollview;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class ExampleActivity extends ActionBarActivity implements
		ObservableScrollViewCallbacks {

	private static final float MAX_TEXT_SCALE_DELTA = 0.3f;
	private static final boolean TOOLBAR_IS_STICKY = false;
	private static final int NUM_OF_ITEMS = 100;

	private View mToolbar;
	private View mImageView;
	private View mOverlayView;
	private View mListBackgroundView;
	private TextView mTitleView;
	private View mFab;
	private int mActionBarSize;
	private int mFlexibleSpaceShowFabOffset;
	private int mFlexibleSpaceImageHeight;
	private int mFabMargin;
	private int mToolbarColor;
	private boolean mFabIsShown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_example);

		setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

		mFlexibleSpaceImageHeight = getResources().getDimensionPixelSize(
				R.dimen.flexible_space_image_height);
		mFlexibleSpaceShowFabOffset = getResources().getDimensionPixelSize(
				R.dimen.flexible_space_show_fab_offset);
		mActionBarSize = getActionBarSize();
		mToolbarColor = getResources().getColor(R.color.primary);

		mToolbar = findViewById(R.id.toolbar);
		if (!TOOLBAR_IS_STICKY) {
			mToolbar.setBackgroundColor(Color.TRANSPARENT);
		}
		mImageView = findViewById(R.id.image);
		mOverlayView = findViewById(R.id.overlay);
		ObservableListView listView = (ObservableListView) findViewById(R.id.list);
		listView.setScrollViewCallbacks(this);

		// Set padding view for ListView. This is the flexible space.
		View paddingView = new View(this);
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
				AbsListView.LayoutParams.MATCH_PARENT,
				mFlexibleSpaceImageHeight);
		paddingView.setLayoutParams(lp);

		// This is required to disable header's list selector effect
		paddingView.setClickable(true);

		listView.addHeaderView(paddingView);
		setDummyData(listView);
		mTitleView = (TextView) findViewById(R.id.title);
		mTitleView.setText(getTitle());
		setTitle(null);
		mFab = findViewById(R.id.fab);
		mFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(ExampleActivity.this, "FAB is clicked",
						Toast.LENGTH_SHORT).show();
			}
		});
		mFabMargin = getResources().getDimensionPixelSize(
				R.dimen.margin_standard);
		ViewHelper.setScaleX(mFab, 0);
		ViewHelper.setScaleY(mFab, 0);

		// mListBackgroundView makes ListView's background except header view.
		mListBackgroundView = findViewById(R.id.list_background);
		final View contentView = getWindow().getDecorView().findViewById(
				android.R.id.content);
		contentView.post(new Runnable() {
			@Override
			public void run() {
				// mListBackgroundView's should fill its parent vertically
				// but the height of the content view is 0 on 'onCreate'.
				// So we should get it with post().
				mListBackgroundView.getLayoutParams().height = contentView
						.getHeight();
			}
		});
	}

	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll,
			boolean dragging) {
		// Translate overlay and image
		float flexibleRange = mFlexibleSpaceImageHeight - mActionBarSize;
		int minOverlayTransitionY = mActionBarSize - mOverlayView.getHeight();
		ViewHelper.setTranslationY(mOverlayView,
				ScrollUtils.getFloat(-scrollY, minOverlayTransitionY, 0));
		ViewHelper.setTranslationY(mImageView,
				ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0));

		// Translate list background
		ViewHelper.setTranslationY(mListBackgroundView,
				Math.max(0, -scrollY + mFlexibleSpaceImageHeight));

		// Change alpha of overlay
		ViewHelper.setAlpha(mOverlayView,
				ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));

		// Scale title text
		float scale = 1 + ScrollUtils.getFloat((flexibleRange - scrollY)
				/ flexibleRange, 0, MAX_TEXT_SCALE_DELTA);
		setPivotXToTitle();
		ViewHelper.setPivotY(mTitleView, 0);
		ViewHelper.setScaleX(mTitleView, scale);
		ViewHelper.setScaleY(mTitleView, scale);

		// Translate title text
		int maxTitleTranslationY = (int) (mFlexibleSpaceImageHeight - mTitleView
				.getHeight() * scale);
		int titleTranslationY = maxTitleTranslationY - scrollY;
		if (TOOLBAR_IS_STICKY) {
			titleTranslationY = Math.max(0, titleTranslationY);
		}
		ViewHelper.setTranslationY(mTitleView, titleTranslationY);

		// Translate FAB
		int maxFabTranslationY = mFlexibleSpaceImageHeight - mFab.getHeight()
				/ 2;
		float fabTranslationY = ScrollUtils.getFloat(-scrollY
				+ mFlexibleSpaceImageHeight - mFab.getHeight() / 2,
				mActionBarSize - mFab.getHeight() / 2, maxFabTranslationY);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			// On pre-honeycomb, ViewHelper.setTranslationX/Y does not set
			// margin,
			// which causes FAB's OnClickListener not working.
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFab
					.getLayoutParams();
			lp.leftMargin = mOverlayView.getWidth() - mFabMargin
					- mFab.getWidth();
			lp.topMargin = (int) fabTranslationY;
			mFab.requestLayout();
		} else {
			ViewHelper.setTranslationX(mFab, mOverlayView.getWidth()
					- mFabMargin - mFab.getWidth());
			ViewHelper.setTranslationY(mFab, fabTranslationY);
		}

		// Show/hide FAB
		if (fabTranslationY < mFlexibleSpaceShowFabOffset) {
			hideFab();
		} else {
			showFab();
		}

		if (TOOLBAR_IS_STICKY) {
			// Change alpha of toolbar background
			if (-scrollY + mFlexibleSpaceImageHeight <= mActionBarSize) {
				mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(1,
						mToolbarColor));
			} else {
				mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(0,
						mToolbarColor));
			}
		} else {
			// Translate Toolbar
			if (scrollY < mFlexibleSpaceImageHeight) {
				ViewHelper.setTranslationY(mToolbar, 0);
			} else {
				ViewHelper.setTranslationY(mToolbar, -scrollY);
			}
		}
	}

	@Override
	public void onDownMotionEvent() {
	}

	@Override
	public void onUpOrCancelMotionEvent(ScrollState scrollState) {
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private void setPivotXToTitle() {
		Configuration config = getResources().getConfiguration();
		if (Build.VERSION_CODES.JELLY_BEAN_MR1 <= Build.VERSION.SDK_INT
				&& config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
			ViewHelper.setPivotX(mTitleView, findViewById(android.R.id.content)
					.getWidth());
		} else {
			ViewHelper.setPivotX(mTitleView, 0);
		}
	}

	private void showFab() {
		if (!mFabIsShown) {
			ViewPropertyAnimator.animate(mFab).cancel();
			ViewPropertyAnimator.animate(mFab).scaleX(1).scaleY(1)
					.setDuration(200).start();
			mFabIsShown = true;
		}
	}

	private void hideFab() {
		if (mFabIsShown) {
			ViewPropertyAnimator.animate(mFab).cancel();
			ViewPropertyAnimator.animate(mFab).scaleX(0).scaleY(0)
					.setDuration(200).start();
			mFabIsShown = false;
		}
	}

	private int getActionBarSize() {
		TypedValue typedValue = new TypedValue();
		int[] textSizeAttr = new int[] { R.attr.actionBarSize };
		int indexOfAttrTextSize = 0;
		TypedArray a = obtainStyledAttributes(typedValue.data, textSizeAttr);
		int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
		a.recycle();
		return actionBarSize;
	}

	private void setDummyData(ListView listView) {
		setDummyData(listView, NUM_OF_ITEMS);
	}

	private void setDummyData(ListView listView, int num) {
		listView.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, getDummyData(num)));
	}

	private static ArrayList<String> getDummyData(int num) {
		ArrayList<String> items = new ArrayList<String>();
		for (int i = 1; i <= num; i++) {
			items.add("Item " + i);
		}
		return items;
	}
}

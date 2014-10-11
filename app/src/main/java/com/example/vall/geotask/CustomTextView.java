package com.example.vall.geotask;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/*
 * CustomTextView. Единстенное отличие от TextView -
 * рамка вокруг View. Рисуем рамку прямоугольником
 */
public class CustomTextView extends TextView {

	int mScrollX;
	Paint mRectColor = new Paint(Paint.ANTI_ALIAS_FLAG);
	Rect mRect = new Rect();

	public CustomTextView(Context context) {
		super(context);
		init();
	}

	public CustomTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	protected void init() {
		Resources resources = getResources();
		mRectColor.setColor(resources.getColor(R.color.black_transparency_80));
		mRectColor.setStyle(Paint.Style.STROKE);
		int strokeDimension = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				resources.getDimension(R.dimen.rect_stroke_dimension), resources.getDisplayMetrics());
		mRectColor.setStrokeWidth(strokeDimension);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		mScrollX = getScrollX();
		mRect.set(mScrollX, 0, getMeasuredWidth() + mScrollX, getMeasuredHeight());
		canvas.drawRect(mRect, mRectColor);
		super.onDraw(canvas);
	}
}

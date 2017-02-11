package tk.giesecke.myhomecontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class BrightnessControl extends SeekBar {

//	public BrightnessControl(Context context) {
//		super(context);
//	}

	public BrightnessControl(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public BrightnessControl(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//　Swapping height and width makes it vertical!
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	protected void onDraw(Canvas c) {
		c.rotate(-90);
		c.translate(-getHeight(), 0);

		super.onDraw(c);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);

		if (!isEnabled()) {
			return false;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
				onSizeChanged(getWidth(), getHeight(), 0, 0);
				break;
			case MotionEvent.ACTION_UP:
				setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
				onSizeChanged(getWidth(), getHeight(), 0, 0);
				break;
			case MotionEvent.ACTION_CANCEL:
			default:
				break;
		}
		return true;
	}
}

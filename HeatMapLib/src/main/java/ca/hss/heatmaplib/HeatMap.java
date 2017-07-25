/**
 * HeatMap.java
 *
 * Copyright 2016 Heartland Software Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the license at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the LIcense is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.hss.heatmaplib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Travis Redpath on 10/2/2016.
 *
 * A class for rendering a heat map in an Android view.
 */
public class HeatMap extends View implements View.OnTouchListener {

    /**
     * The data that will be displayed in the heat map.
     */
    private List<DataPoint> data;

    /**
     * A buffer for new data that hasn't been displayed yet.
     */
    private List<DataPoint> dataBuffer;

    /**
     * Whether the information stored in dataBuffer has changed.
     */
    private boolean dataModified = false;

    /**
     * The value that corresponds to the minimum of the gradient scale.
     */
    private double min = Double.NEGATIVE_INFINITY;
    /**
     * The value that corresponds to the maximum of the gradient scale.
     */
    private double max = Double.POSITIVE_INFINITY;

    /**
     * The amount of blur to use.
     */
    private double mBlur = 0.85;
    /**
     * The radius (px) of the circle each data point takes up.
     */
    private double mRadius = 200;

    /**
     * If greater than 0 this will be used as the transparency for the entire map.
     */
    private int opacity = 0;
    /**
     * The minimum opacity to use in the map. Only used when {@link HeatMap#opacity} is 0.
     */
    private int minOpacity = 0;
    /**
     * The maximum opacity to use in the map. Only used when {@link HeatMap#opacity} is 0.
     */
    private int maxOpacity = 255;

    /**
     * The bounds of actual data. For the sake of efficiency this stops us updating outside
     * of where data is present.
     */
    private double mRenderBoundaries[] = new double[4];

    /**
     * Colors to be used in building the gradient.
     */
    private int colors[] = new int[] { 0xffff0000, 0xff00ff00 };

    /**
     * The stops to position the colors at.
     */
    private float positions[] = new float[] { 0.0f, 1.0f };

    /**
     * A paint for solid black.
     */
    private Paint mBlack;

    /**
     * A paint to be used to fill objects.
     */
    private Paint mFill;

    /**
     * The color palette being used to create the radial gradients.
     */
    private int palette[] = null;

    /**
     * Whether the palette needs refreshed.
     */
    private boolean needsRefresh = true;

    /**
     * Update the shadow layer when the size changes.
     */
    private boolean sizeChange = false;

    private OnMapClickListener mListener;

    private Bitmap mShadow = null;

    private Canvas mShadowCanvas = null;

    /**
     * Set the blur factor for the heat map. Must be between 0 and 1.
     * @param blur The blur factor
     */
    public void setBlur(double blur) {
        if (blur > 1.0 || blur < 0.0)
            throw new IllegalArgumentException("Blur must be between 0 and 1.");
        mBlur = blur;
    }
    /**
     * Get the heat map's blur factor.
     */
    public double getBlur() { return mBlur; }

    /**
     * Sets the value associated with the maximum on the gradient scale.
     *
     * This should be greater than the minimum value.
     * @param max The maximum value.
     */
    public void setMaximum(double max) { this.max = max; }

    /**
     * Sets the value associated with the minimum on the gradient scale.
     *
     * This should be less than the maximum value.
     * @param min The minimum value.
     */
    public void setMinimum(double min) { this.min = min; }

    /**
     * Set the opacity to be used in the heat map. This opacity will be used for the entire map.
     * @param opacity The opacity in the range [0,255].
     */
    public void setOpacity(int opacity) { this.opacity = opacity; }

    /**
     * Set the minimum opacity to be used in the map. Only used when {@link HeatMap#opacity} is 0.
     * @param min The minimum opacity in the range [0,255].
     */
    public void setMinimumOpactity(int min) { this.minOpacity = min; }

    /**
     * Set the maximum opacity to be used in the map. Only used when {@link HeatMap#opacity} is 0.
     * @param max The maximum opacity in the range [0,255].
     */
    public void setMaximumOpactity(int max) { this.maxOpacity = max; }

    /**
     * Set the circles radius when drawing data points.
     * @param radius The radius in pixels.
     */
    public void setRadius(double radius) { this.mRadius = radius; }

    /**
     * Set the color stops used for the heat map's gradient. There needs to be at least 2 stops
     * and there should be one at a position of 0 and one at a position of 1.
     * @param stops A map from stop positions (as fractions of the width in [0,1]) to ARGB colors.
     */
    public void setColorStops(Map<Float, Integer> stops) {
        if (stops.size() < 2)
            throw new IllegalArgumentException("There must be at least 2 color stops");
        colors = new int[stops.size()];
        positions = new float[stops.size()];
        int i = 0;
        for (Float key : stops.keySet()) {
            colors[i] = stops.get(key);
            positions[i] = key;
            i++;
        }
    }

    /**
     * Add a new data point to the heat map.
     *
     * Does not refresh the display. See {@link HeatMap#forceRefresh()} in order to redraw the heat map.
     * @param point A new data point.
     */
    public void addData(DataPoint point) {
        dataBuffer.add(point);
        dataModified = true;
    }

	public void addDataSet(ArrayList<DataPoint> data) {
		dataBuffer = data;
		dataModified = true;
	}

    /**
     * Clears the data that is being displayed in the heat map.
     *
     * Does not refresh the display. See {@link HeatMap#forceRefresh()} in order to redraw the heat map.
     */
    public void clearData() {
        dataBuffer.clear();
        dataModified = true;
    }

    /**
     * Register a callback to be invoked when this view is clicked. It will return the closest
     * data point as well as the clicked location.
     * @param listener The callback that will run
     */
    public void setOnMapClickListener(OnMapClickListener listener) { this.mListener = listener; }

    /**
     * Register a callback to be invoked when this view is touched.
     * @param listener The callback that will run
     * @deprecated Use {@link #setOnMapClickListener(OnMapClickListener)} instead.
     */
    @Override
    @Deprecated
    public void setOnTouchListener(OnTouchListener listener) {
        mListener = null;
        super.setOnTouchListener(listener);
    }

    /**
     * Simple constructor to use when creating a view from code.
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public HeatMap(Context context) {
        super(context);
        initialize();
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called when a view is
     * being constructed from an XML file, supplying attributes that were specified in the XML file.
     * This version uses a default style of 0, so the only attribute values applied are those in the
     * Context's Theme and the given AttributeSet.
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public HeatMap(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.HeatMap, 0, 0);
        try {
            opacity = a.getInt(R.styleable.HeatMap_opacity, -1);
            if (opacity < 0)
                opacity = 0;
            minOpacity = a.getInt(R.styleable.HeatMap_minOpacity, -1);
            if (minOpacity < 0)
                minOpacity = 0;
            maxOpacity = a.getInt(R.styleable.HeatMap_maxOpacity, -1);
            if (maxOpacity < 0)
                maxOpacity = 255;
            mBlur = a.getFloat(R.styleable.HeatMap_blur, -1);
            if (mBlur < 0)
                mBlur = 0.85;
            mRadius = a.getDimension(R.styleable.HeatMap_radius, -1);
            if (mRadius < 0)
                mRadius = 200;
        } finally {
            a.recycle();
        }
    }

    /**
     * Force a refresh of the heat map.
     *
     * Use this instead of {@link View#invalidate()}.
     */
    public void forceRefresh() {
        needsRefresh = true;
        invalidate();
    }

    /**
     * Initialize all of the paints that we're cable of before drawing.
     */
    private void initialize() {
        mBlack = new Paint();
        mBlack.setColor(0xff000000);
        mFill = new Paint();
        mFill.setStyle(Paint.Style.FILL);
        data = new ArrayList<>();
        dataBuffer = new ArrayList<>();
        super.setOnTouchListener(this);
        this.setDrawingCacheEnabled(true);
        this.setDrawingCacheBackgroundColor(Color.TRANSPARENT);
    }

    private void redrawShadow() {
        mRenderBoundaries[0] = 10000;
        mRenderBoundaries[1] = 10000;
        mRenderBoundaries[2] = 0;
        mRenderBoundaries[3] = 0;

        mShadow = getDrawingCache();
        mShadowCanvas = new Canvas(mShadow);

        drawTransparent(mShadowCanvas);
    }

    /**
     * If needed, refresh the palette.
     */
    private void tryRefresh() {
        if (needsRefresh) {
            Bitmap bit = Bitmap.createBitmap(256, 1, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bit);
            LinearGradient grad;
            grad = new LinearGradient(0, 0, 256, 1, colors, positions, Shader.TileMode.CLAMP);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(grad);
            canvas.drawLine(0, 0, 256, 1, paint);
            palette = new int[256];
            bit.getPixels(palette, 0, 256, 0, 0, 256, 1);

            if (dataModified) {
                data.clear();
                data.addAll(dataBuffer);
                dataBuffer.clear();
                dataModified = false;
            }

            redrawShadow();
        }
        else if (sizeChange) {
            redrawShadow();
        }
        needsRefresh = false;
        sizeChange = false;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        sizeChange = true;
    }

    /**
     * Draw the heatp map.
     *
     * @param canvas Canvas to draw into.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        tryRefresh();

        drawColour(canvas);
    }

    /**
     * Draw a radial gradient at a given location. Only draws in black with the gradient being only
     * in transparency.
     *
     * @param canvas Canvas to draw into.
     * @param x The x location to draw the point.
     * @param y The y location to draw the point.
     * @param radius The radius (in pixels) of the point.
     * @param blurFactor A factor to scale the circles width by.
     * @param alpha The transparency of the gradient.
     */
    private void drawDataPoint(Canvas canvas, float x, float y, double radius, double blurFactor, double alpha) {
        if (blurFactor == 1) {
            canvas.drawCircle(x, y, (float)radius, mBlack);
        }
        else {
            //create a radial gradient at the requested position with the requested size
            RadialGradient gradient = new RadialGradient(x, y, (float)(radius * blurFactor),
                    new int[] { Color.argb((int)(alpha * 255), 0, 0, 0), Color.argb(0, 0, 0, 0) },
                    null, Shader.TileMode.CLAMP);
            mFill.setShader(gradient);
            canvas.drawCircle(x, y, (float)(2 * radius), mFill);
        }
    }

    /**
     * Draw a heat map in only black and transparency to be used as the blended base of the coloured
     * version.
     *
     * @param canvas Canvas to draw into.
     */
    private void drawTransparent(Canvas canvas) {
        //invert the blur factor
        double blur = 1 - mBlur;

        //clear the canvas
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        //loop through the data points
        for (DataPoint point : data) {
            float x = point.x * getWidth();
            float y = point.y * getHeight();
            double value = Math.max(min, Math.min(point.value, max));
            //the edge of the bounding rectangle for the circle
            double rectX = x - mRadius;
            double rectY = y - mRadius;

            //calculate the transparency of the circle from its percentage between the max and
            //min values
            double alpha = (value - min)/(max - min);

            //draw the point into the canvas
            drawDataPoint(canvas, x, y, mRadius, blur, alpha);

            //update the modified bounds of the image if necessary
            if (rectX < mRenderBoundaries[0])
                mRenderBoundaries[0] = rectX;
            if (rectY < mRenderBoundaries[1])
                mRenderBoundaries[1] = rectY;
            if ((rectX + (2*mRadius)) > mRenderBoundaries[2])
                mRenderBoundaries[2] = rectX + (2*mRadius);
            if ((rectY + (2*mRadius)) > mRenderBoundaries[3])
                mRenderBoundaries[3] = rectY + (2*mRadius);
        }
    }

    /**
     * Convert the black/transparent heat map into a full colour one.
     *
     * @param canvas The canvas to draw into.
     */
    private void drawColour(Canvas canvas) {
        if (data.size() == 0)
            return;

        //calculate the bounds of shadow layer that have modified pixels
        int x = (int)mRenderBoundaries[0];
        int y = (int)mRenderBoundaries[1];
        int width = (int)mRenderBoundaries[2];
        int height = (int)mRenderBoundaries[3];
        int maxWidth = getWidth();
        int maxHeight = getHeight();

        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        if (x + width > maxWidth)
            width = maxWidth - x;
        if (y + height > maxHeight)
            height = maxHeight - y;

        //retrieve the modified pixels from the shadow layer
        int pixels[] = new int[width * height];
        mShadow.getPixels(pixels, 0, width, x, y, width, height);
	    long startTime = System.nanoTime();
        //loop over each retrieved pixel
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int pixel = pixels[i + (j * width)];
                //the pixels alpha value (0-255)
                int alpha = 0xff & (pixel >> 24);

                //clamp the alpha value to user specified bounds
                int clampAlpha;
                if (opacity > 0)
                    clampAlpha = opacity;
                else {
                    if (alpha < maxOpacity) {
                        if (alpha < minOpacity) {
                            clampAlpha = minOpacity;
                        }
                        else {
                            clampAlpha = alpha;
                        }
                    }
                    else {
                        clampAlpha = maxOpacity;
                    }
                }

                //set the pixels colour to its corresponding colour in the palette
                pixels[i + (j * width)] = ((0xff & clampAlpha) << 24) | (0xffffff & palette[alpha]);
            }
        }

	    long endTime = System.nanoTime();

	    System.out.println("time: " + (endTime - startTime));

	    //set the modified pixels back into the bitmap
        mShadow.setPixels(pixels, 0, width, x, y, width, height);
        //render the bitmap onto the heat map
        canvas.drawBitmap(mShadow, 0, 0, null);
    }

    private float touchX;
    private float touchY;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mListener != null) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                double d = Math.sqrt(Math.pow(touchX - x, 2.0f) + Math.pow(touchY - y, 2.0f));
                if (d < 10) {
                    x = x / (float) getWidth();
                    y = y / (float) getHeight();
                    double minDist = Double.MAX_VALUE;
                    DataPoint minPoint = null;
                    for (DataPoint point : data) {
                        double dist = point.distanceTo(x, y);
                        if (dist < minDist) {
                            minDist = dist;
                            minPoint = point;
                        }
                    }
                    mListener.onMapClicked((int) x, (int) y, minPoint);
                    return true;
                }
            }
            else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                touchX = motionEvent.getX();
                touchY = motionEvent.getY();
                return true;
            }
        }
        return false;
    }

    /**
     * Stores data points to display in the heat map.
     */
    public static class DataPoint {
        /**
         * The data points x value as a decimal percent of the views width.
         */
        public float x;
        /**
         * The data points y value as a decimal percent of the views height.
         */
        public float y;
        /**
         * The intensity value of the data point.
         */
        public double value;
        /**
         * Any user specific data that may need to be associated with a point.
         */
        public Object userData = null;

        /**
         * Construct a new data point to be displayed in the heat map.
         *
         * @param x The data points x location as a decimal percent of the views width
         * @param y The data points y location as a decimal percent of the views height
         * @param value The intensity value of the data point
         */
        public DataPoint(float x, float y, double value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

        double distanceTo(float x, float y) {
            return Math.sqrt(Math.pow(x - this.x, 2.0) + Math.pow(y - this.y, 2.0));
        }


	    public float getX() {
		    return x;
	    }

	    public void setX(float x) {
		    this.x = x;
	    }

	    public float getY() {
		    return y;
	    }

	    public void setY(float y) {
		    this.y = y;
	    }

	    public double getValue() {
		    return value;
	    }

	    public void setValue(double value) {
		    this.value = value;
	    }
    }

    public interface OnMapClickListener {
        void onMapClicked(int x, int y, DataPoint closest);
    }
}

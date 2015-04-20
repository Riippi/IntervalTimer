package org.leeko.intervaltimer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Mikko on 24.1.2015.
 */
public class CustomButton extends LinearLayout {


    TextView left, right;

    public CustomButton(Context context)
    {
        super(context);
        init();

    }
    public CustomButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();

    }
    public CustomButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();

    }

    private void init() {
        //left = (TextView)findViewById(R.id.textViewXLeft);
// right = (TextView)findViewById(R.id.textViewXRight);


       // LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
       // inflater.inflate(R.layout.custom_button, this, true);

       // left = (TextView)getChildAt(0);
    }


    // method for setting texts for the text views
    public void setTitleText(CharSequence text)
    {
        left = (TextView)getChildAt(0);

        if (null != left)
        {
            left.setText(text);
        }
    }

    public void setValueText(CharSequence text)
    {

        right = (TextView)getChildAt(1);

        if (null != right)
        {
            right.setText(text);
        }
    }


}

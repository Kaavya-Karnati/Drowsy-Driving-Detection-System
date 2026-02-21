package com.example.drowsydrivingdetection;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class FaqActivity extends NavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.faq_page);

        // FAQ textviews
        TextView faq1 = findViewById(R.id.FAQA1);
        TextView faq2 = findViewById(R.id.FAQA2);
        TextView faq3 = findViewById(R.id.FAQA3);
        TextView faq4 = findViewById(R.id.FAQA4);

        // FAQ buttons
        ImageView button1 = findViewById(R.id.FAQQ1Arrow);
        ImageView button2 = findViewById(R.id.FAQQ2Arrow);
        ImageView button3 = findViewById(R.id.FAQQ3Arrow);
        ImageView button4 = findViewById(R.id.FAQQ4Arrow);

        // FAQ Question 1 button
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (faq1.getVisibility() == View.GONE) {
                    faq1.setVisibility(View.VISIBLE);
                    button1.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    faq1.setVisibility(View.GONE);
                    button1.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });

        // FAQ Question 2 button
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (faq2.getVisibility() == View.GONE) {
                    faq2.setVisibility(View.VISIBLE);
                    button2.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    faq2.setVisibility(View.GONE);
                    button2.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });

        // FAQ Question 3 button
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (faq3.getVisibility() == View.GONE) {
                    faq3.setVisibility(View.VISIBLE);
                    button3.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    faq3.setVisibility(View.GONE);
                    button3.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });

        // FAQ question 4 button
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (faq4.getVisibility() == View.GONE) {
                    faq4.setVisibility(View.VISIBLE);
                    button4.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    faq4.setVisibility(View.GONE);
                    button4.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });

        setupBottomNavigation();
    }
}
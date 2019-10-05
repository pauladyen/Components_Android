package com.adyen.components;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultsPage extends AppCompatActivity {


    TextView componentResult;
    Button newPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_page);

        newPayment = findViewById(R.id.newPayment);

        componentResult = findViewById(R.id.componentresults);


        Intent intent = getIntent();

        componentResult.setText(intent.getStringExtra("ComponentResult"));

        newPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restart();
            }
        });




    }


    public void restart(){
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
        this.finishAffinity();
    }

}

package com.adyen.components;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.adyen.checkout.adyen3ds2.Adyen3DS2Component;
import com.adyen.checkout.base.ComponentAvailableCallback;
import com.adyen.checkout.googlepay.GooglePayComponent;
import com.adyen.checkout.googlepay.GooglePayConfiguration;
import com.adyen.checkout.sepa.SepaComponent;
import com.adyen.checkout.sepa.SepaConfiguration;
import com.adyen.checkout.sepa.SepaView;
import com.adyen.components.Network.ApiConfig;
import com.adyen.components.Network.AppConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.lifecycle.Observer;
import com.adyen.checkout.base.ActionComponentData;
import com.adyen.checkout.base.PaymentComponentState;
import com.adyen.checkout.base.model.PaymentMethodsApiResponse;
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod;
import com.adyen.checkout.base.model.payments.response.Action;
import com.adyen.checkout.card.CardComponent;
import com.adyen.checkout.card.CardComponentProvider;
import com.adyen.checkout.card.CardConfiguration;
import com.adyen.checkout.card.CardView;
import com.adyen.checkout.ideal.IdealComponent;
import com.adyen.checkout.ideal.IdealConfiguration;
import com.adyen.checkout.ideal.IdealSpinnerView;
import com.adyen.checkout.redirect.RedirectComponent;
import com.adyen.checkout.redirect.RedirectUtil;
import com.adyen.components.Network.EncodingUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    EditText currency,country,amount;
    Button getMethods, pay;

    Spinner methodSpinner;

    public static String curr, amo, cc;
    Intent resultsIntent;

    CardView cardView;
    IdealSpinnerView idealSpinnerView;
    SepaView sepaView;
    LinearLayoutCompat layout;


    JSONObject jsonPayMethodsResponse;

    RedirectComponent redirectComponent;

    public static int GOOGLEPAY = 000;

    GooglePayComponent googlePayComponent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardView = findViewById(R.id.cardComp);
        idealSpinnerView = findViewById(R.id.idealComp);
        sepaView = findViewById(R.id.sepaComp);

        layout = findViewById(R.id.layout);

        currency = findViewById(R.id.currency);
        country = findViewById(R.id.countryCode);
        amount = findViewById(R.id.amount);

        getMethods = findViewById(R.id.getMethods);
        methodSpinner = findViewById(R.id.methodSpinner);
        pay = findViewById(R.id.pay);

        methodSpinner.setEnabled(false);


        reset();

        resultsIntent = new Intent(MainActivity.this, ResultsPage.class);

        Random rand = new Random();

        amount.setText(String.valueOf(1000 + rand.nextInt(9999)));



        getMethods.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                curr = currency.getText().toString().toUpperCase();
                amo = amount.getText().toString();
                cc = country.getText().toString();

                getPaymentMethods(
                        country.getText().toString().toUpperCase(),
                        curr,
                        amount.getText().toString()
                );
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getPaymentMethods(String country, String Currency, String Amount){

        ApiConfig getResponse = AppConfig.getRetrofit().create(ApiConfig.class);

        Call<JsonObject> call = getResponse.getPaymentMethods(country, Currency, Amount, "Android");

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()&&response.body()!=null){

                        try{
                            Gson gson = new Gson();
                            String json = gson.toJson(response.body());


                            jsonPayMethodsResponse = new JSONObject(json);

                            Log.i("GetPaymentMethods",jsonPayMethodsResponse.toString(4));

                            PaymentMethodsApiResponse paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonPayMethodsResponse);

                            final List<PaymentMethod> paymentMethods = paymentMethodsApiResponse.getPaymentMethods();


                            if(!paymentMethods.isEmpty()){

                                methodSpinner.setEnabled(true);

                                List<String> methodNames = new ArrayList<String>();


                                for (PaymentMethod pm : paymentMethods){
                                    methodNames.add(pm.getName());
                                }


                                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, methodNames);

                                methodSpinner.setAdapter(dataAdapter);

                                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                                methodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        onMethodSelected(paymentMethods.get(position));
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {

                                    }
                                });

                            }
                        }catch (Exception e){
                            Log.e("GetPaymentMethods", e.toString());
                        }

                }else {
                    Log.e("GetPaymentMethods", response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("GetPaymentMethods", t.toString());
            }
        });


    }

    private void onMethodSelected(final PaymentMethod paymentMethod){

        Log.v("Selected_Type:",paymentMethod.getType());

        switch (paymentMethod.getType()){

            case "scheme":
                loadCardComponent();
                break;

            case "ideal":
                loadIdealComponent(paymentMethod);
                break;


            case "paypal":
                break;
            case "klarna_account":
            case "klarna_paynow":
            case "klarna":
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try{
                            makePaymentsCall("{\"type\": "+paymentMethod.getType()+"}");
                        }catch (Exception e){
                        }
                    }
                }).start();

                break;
            case "sepadirectdebit":
                loadSepaComponent(paymentMethod);
                break;
            case "cup":
                break;
            case "paywithgoogle":
                loadGooglePayComponent(paymentMethod);
                break;
            case "wechatpayQR":
                break;
            case "wechatpayWeb":
                break;
            case "safetypay":
                break;
            case "boletobancario_santander":
                break;

        }

    }

    private void loadCardComponent(){

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setType("scheme");

        CardConfiguration cardConfiguration = new CardConfiguration.Builder(MainActivity.this, "10001|BD67BF5FDCD84F2CD99DD452A76AFC999CA060A187A9A527BA8F72743BAC53BA0362EA7A01AF4612A8FE7B736D9F5B02C65414707A4344D9A666AFCFF33DAB7C387B997D4FE1FABA3502BD604E8793F5CD657C19D822F133E09C48C360E302E5865641FB70DA304E4C067C95361F4181095E61DDF943518FD4811C85C81D7424AF5A0CB66C25E8F1285075F92C2EA4C143E8B1DF6DB2F1CF0B992B5DFBF7FD7419FCBA1421E87A245F42A52DE253E85FB2BE446911214A02A9B40168F354BAC7D742C77B3F99B093FDDD576896B53B32B35419AE9BF2F795D2A3CF2B27C8F466D8D77998B526842FFAF9F188DB5471079EC222590BA298E992C315AEEA44BCCF")
                .build();

        CardComponent cardComponent = new CardComponentProvider().get(MainActivity.this,paymentMethod ,cardConfiguration);

        cardView.attach(cardComponent, MainActivity.this);

        reset();

        layout.addView(cardView);

        layout.addView(pay);
        pay.setEnabled(false);


        cardComponent.observe(MainActivity.this, new Observer<PaymentComponentState>() {
            @Override
            public void onChanged(final PaymentComponentState paymentComponentState) {

                if(paymentComponentState.isValid()){

                    pay.setEnabled(true);

                    pay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try{
                                        JSONObject paymentComponentData = new JSONObject(new Gson().toJson(paymentComponentState));
                                        Log.v("PaymentComponentData", paymentComponentData.toString(4));
                                        makePaymentsCall(paymentComponentData.getJSONObject("mPaymentComponentData").getJSONObject("paymentMethod").toString());
                                    }catch (Exception e){
                                    }
                                }
                            }).start();
                        }
                    });

                }else{
                    pay.setEnabled(false);
                }

            }
        });
    }

    private void loadIdealComponent(PaymentMethod paymentMethod) {

        IdealConfiguration idealConfiguration = new IdealConfiguration.Builder(MainActivity.this).build();

           IdealComponent idealComponent = IdealComponent.PROVIDER.get(MainActivity.this,
                    paymentMethod,
                    idealConfiguration);

            idealSpinnerView.attach(idealComponent, MainActivity.this);

            reset();

            layout.addView(idealSpinnerView);

            layout.addView(pay);
            pay.setEnabled(false);


            idealComponent.observe(MainActivity.this, new Observer<PaymentComponentState>() {
                @Override
                public void onChanged(final PaymentComponentState paymentComponentState) {

                    if(paymentComponentState.isValid()){
                        pay.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try{
                                            JSONObject paymentComponentData = new JSONObject(new Gson().toJson(paymentComponentState));
                                            Log.v("PaymentComponentData", paymentComponentData.toString(4));
                                            makePaymentsCall(paymentComponentData.getJSONObject("mPaymentComponentData").getJSONObject("paymentMethod").toString());
                                        }catch (Exception e){
                                        }

                                    }
                                }).start();
                            }
                        });

                        pay.setEnabled(true);

                    }else{
                        pay.setEnabled(false);
                    }

                }
            });
    }

    private void loadSepaComponent(PaymentMethod paymentMethod){

        SepaConfiguration sepaConfiguration = new SepaConfiguration.Builder(MainActivity.this).build();
        SepaComponent sepaComponent = SepaComponent.PROVIDER.get(MainActivity.this,
                paymentMethod,
                sepaConfiguration);

        sepaView.attach(sepaComponent, MainActivity.this);

        reset();

        layout.addView(sepaView);

        layout.addView(pay);
        pay.setEnabled(false);


        sepaComponent.observe(MainActivity.this, new Observer<PaymentComponentState>() {
            @Override
            public void onChanged(final PaymentComponentState paymentComponentState) {

                if(paymentComponentState.isValid()){
                    pay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        JSONObject paymentComponentData = new JSONObject(new Gson().toJson(paymentComponentState));
                                        Log.v("PaymentComponentData", paymentComponentData.toString(4));
                                        makePaymentsCall(paymentComponentData.getJSONObject("mPaymentComponentData").getJSONObject("paymentMethod").toString());
                                    }catch (Exception e){
                                    }

                                }
                            }).start();
                        }
                    });

                    pay.setEnabled(true);

                }else{
                    pay.setEnabled(false);
                }

            }
        });


    }

    private void loadGooglePayComponent(PaymentMethod paymentMethod){

        reset();

        final GooglePayConfiguration googlePayConfiguration = new GooglePayConfiguration.Builder(MainActivity.this, "PaulAsiimwe").build();


        GooglePayComponent.PROVIDER.isAvailable(getApplication(), paymentMethod, googlePayConfiguration,
                new ComponentAvailableCallback<GooglePayConfiguration>() {
                    @Override
                    public void onAvailabilityResult(boolean isAvailable, @NonNull PaymentMethod paymentMethod, @Nullable GooglePayConfiguration config) {

                        googlePayComponent = new GooglePayComponent(paymentMethod, googlePayConfiguration);

                        googlePayComponent.startGooglePayScreen(MainActivity.this, GOOGLEPAY);

                        layout.addView(pay);
                        pay.setEnabled(false);


                    }
                });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode , Intent data){

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GOOGLEPAY){
            googlePayComponent.observe(MainActivity.this, new Observer<PaymentComponentState>() {
                @Override
                public void onChanged(final PaymentComponentState paymentComponentState) {
                    if(paymentComponentState.isValid()){
                        pay.setEnabled(true);

                        pay.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        try{
                                            JSONObject paymentComponentData = new JSONObject(new Gson().toJson(paymentComponentState));
                                            Log.v("PaymentComponentData", paymentComponentData.toString(4));
                                            makePaymentsCall(paymentComponentData.getJSONObject("mPaymentComponentData").getJSONObject("paymentMethod").toString());
                                        }catch (Exception e){
                                        }
                                    }
                                }).start();
                            }
                        });
                    }
                }
            });

            googlePayComponent.handleActivityResult(resultCode, data);
        }

    }





    private void reset(){
        layout.removeView(cardView);
        layout.removeView(idealSpinnerView);
        layout.removeView(sepaView);
        layout.removeView(pay);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);



        if (intent.getData() != null && intent.getData().toString().startsWith(RedirectUtil.REDIRECT_RESULT_SCHEME)) {
            Log.v("Redirect_Intent","Intent");
            redirectComponent.handleRedirectResponse(intent.getData());
        }

    }

    private void makePaymentsCall(String paymentMethod){
        try  {
            ApiConfig getResponse = AppConfig.getRetrofit().create(ApiConfig.class);

            String data = EncodingUtil.encodeURIComponent(paymentMethod);

            Call<JsonObject> call = getResponse.makePayment(data, MainActivity.curr,MainActivity.cc, MainActivity.amo, EncodingUtil.encodeURIComponent("adyencheckout://com.adyen.components"),"Android");

            Response<JsonObject> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {

                String json = new Gson().toJson(response.body());

                final JSONObject paymentsResponse = new JSONObject(json);

                Log.v("PayCallResponse", paymentsResponse.toString(4));

                if(paymentsResponse.getString("resultCode").equalsIgnoreCase("IdentifyShopper")){
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            threeDSAction(paymentsResponse);
                        }
                    });
                }else if(paymentsResponse.getString("resultCode").equalsIgnoreCase("RedirectShopper")){
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            launchRedirectComponent(paymentsResponse);
                        }
                    });
                }else{
                    resultsIntent.putExtra("ComponentResult", new Gson().toJson(paymentsResponse));

                    startActivity(resultsIntent);
                }


            }else {
                Log.e("Unsuccessful_PayResult", response.message());
            }



        } catch (Exception e) {
            Log.e("Exception_PayResult",e.toString());
        }
    }

    private void makePaymentsDetailsCall(final JSONObject paymentsResponse, ActionComponentData actionComponentData){
        ApiConfig getResponse = AppConfig.getRetrofit().create(ApiConfig.class);

        try{
            Call<JsonObject> call;

            String type = paymentsResponse.getJSONObject("action").getString("type")
                    + paymentsResponse.getJSONObject("action").getString("paymentMethodType");

            if(type.equalsIgnoreCase("redirectscheme")){
                call = getResponse.paymentDetailsScheme(
                        type,
                        actionComponentData.getDetails().getString("MD"),
                        actionComponentData.getDetails().getString("PaRes"),
                        paymentsResponse.getString("paymentData")
                );
            }else if(type.equalsIgnoreCase("redirectideal")){
                call = getResponse.paymentDetailsIdeal(
                        type,
                        actionComponentData.getDetails().getString("payload")
                );
            }else if (type.equalsIgnoreCase("threeDS2Fingerprintscheme")) {

                call = getResponse.paymentDetailsFingerPrint(
                        type,
                        actionComponentData.getDetails().getString("threeds2.fingerprint"),
                        paymentsResponse.getString("paymentData")
                );

            }else if (type.equalsIgnoreCase("threeDS2Challengescheme")){

                    call = getResponse.paymentDetailsChallenge(
                            type,
                            actionComponentData.getDetails().getString("threeds2.challengeResult"),
                            paymentsResponse.getString("paymentData")
                    );

            }else if (type.equalsIgnoreCase("redirectklarna_account")
            ||  type.equalsIgnoreCase("redirectklarna_paynow")
            ||  type.equalsIgnoreCase("redirectklarna")){

                call = getResponse.paymentDetailsKlarnaAccount(
                        type,
                        actionComponentData.getDetails().getString("redirectResult"),
                        paymentsResponse.getString("paymentData")
                );
            }else {
               call = null;
            }



            //String data = EncodingUtil.encodeURIComponent(paymentComponentData.toString());

            Response<JsonObject> response = call.execute();

            if (response.isSuccessful() && response.body() != null){

                String json = new Gson().toJson(response.body());

                final JSONObject paymentsDetailsResponse = new JSONObject(json);

                Log.v("PaymentsDetailsResponse", paymentsDetailsResponse.toString(4));

                if(paymentsDetailsResponse.getString("resultCode").equalsIgnoreCase("ChallengeShopper")){

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            threeDSAction(paymentsDetailsResponse);
                        }
                    });
                }else{
                    resultsIntent.putExtra("ComponentResult", json);

                    startActivity(resultsIntent);
                }


            }else {
                Log.e("PaymentsDetailsResponse", response.message());
            }



        }catch (Exception e){
            Log.e("PaymentsDetailsResponse",e.toString());
        }
    }

    private void launchRedirectComponent(final JSONObject paymentsResponse) {

        try{
            final JSONObject actionResponse = paymentsResponse.getJSONObject("action");
            final Action action = Action.SERIALIZER.deserialize(actionResponse);

            Log.v("Redirect_Action_Data",actionResponse.toString(4));

            redirectComponent = RedirectComponent.PROVIDER.get(this);

            redirectComponent.handleAction(this, action);

            redirectComponent.observe(this, new Observer<ActionComponentData>() {
                @Override
                public void onChanged(final ActionComponentData actionComponentData) {

                    try{
                        Log.v("Redirect_Data",actionComponentData.getDetails().toString(4));

                        final String paymentMethodType = actionResponse.getString("paymentMethodType");


                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                makePaymentsDetailsCall(paymentsResponse, actionComponentData);
                            }
                        }).start();

                    }catch (Exception e){
                        Log.e("Redirect_Pay_Response",e.toString());
                    }
                }
            });


        }catch (JSONException e){

        }




    }

    private void threeDSAction(final JSONObject paymentsResponse){

        try{
            Adyen3DS2Component threedsComponent = Adyen3DS2Component.PROVIDER.get(this);

            final JSONObject actionResponse = paymentsResponse.getJSONObject("action");
            final Action action = Action.SERIALIZER.deserialize(actionResponse);

            threedsComponent.handleAction(this, action);

            threedsComponent.observe(this, new Observer<ActionComponentData>() {
                @Override
                public void onChanged(final ActionComponentData actionComponentData) {


                    try{
                        JSONObject actionComponent = new JSONObject(
                                new Gson().toJson(
                                        actionComponentData
                                )
                        );

                        Log.v("3DS", actionComponent.toString(4));

                    }catch (Exception e){

                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            makePaymentsDetailsCall(paymentsResponse, actionComponentData);
                        }
                    }).start();

                }
            });


        }catch (Exception e){

        }

    }

    public static String generateString() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

//    private class idealPay extends AsyncTask<String, PaymentMethodsResponse, PaymentMethodsResponse> {
//
//        protected PaymentMethodsResponse doInBackground(String... params) {
//
//        }
//
//        protected void onPostExecute(PaymentMethodsResponse response) {
//
//
//
//        }
//    }


}

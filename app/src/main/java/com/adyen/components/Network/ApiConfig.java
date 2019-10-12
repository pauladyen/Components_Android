package com.adyen.components.Network;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;


public interface ApiConfig {

//    @Multipart
//    @POST("android_upload.php")
//    Call<ServerResponse> upload(
//            @PartMap Map<String, RequestBody> map
//    );
//
//
//    @Multipart
//    @POST("android_convert.php")
//    Call<ServerResponse> convert_request(
//            @Part("serverpath") RequestBody serverpath,
//            @Part("format") RequestBody format,
//            @Part("resize") RequestBody resize
//    );

    @Streaming
    @GET
    Call<ResponseBody> downloadFile(@Url String fileUrl);


    @POST("GetPaymentMethods")
    Call<JsonObject> getPaymentMethods(

            @Query("countryCode") String country,
            @Query("currency") String currency,
            @Query("value") String amount,
            @Query("channel") String channel
    );


    @POST("MakePayment")
    Call<JsonObject> makePayment(
            @Query("data") String data,
            @Query("currency") String currency,
            @Query("value") String amount,
            @Query("returnurl") String returnURL,
            @Query("channel") String channel
    );

    @POST("PaymentDetails")
    Call<JsonObject> paymentDetailsScheme(
            @Query("type") String actionType,
            @Query("MD") String MD,
            @Query("PaRes") String PaRes,
            @Query("paymentData") String paymentData
    );

    @POST("PaymentDetails")
    Call<JsonObject> paymentDetailsIdeal(
            @Query("type") String actionType,
            @Query("payload") String payload
    );

    @POST("PaymentDetails")
    Call<JsonObject> paymentDetailsFingerPrint(
            @Query("type") String actionType,
            @Query("fingerprint") String payload,
            @Query("paymentData") String paymentData
    );

    @POST("PaymentDetails")
    Call<JsonObject> paymentDetailsChallenge(
            @Query("type") String actionType,
            @Query("challengeResult") String challengeResult,
            @Query("paymentData") String paymentData
    );

}

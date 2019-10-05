package com.adyen.components.Network;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
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


    @GET("GetPaymentMethods")
    Call<JsonObject> getPaymentMethods(

            @Query("countryCode") String country,
            @Query("currency") String currency,
            @Query("value") String amount,
            @Query("channel") String channel
    );


    @GET("MakePayment")
    Call<JsonObject> makePayment(
            @Query("data") String data,
            @Query("currency") String currency,
            @Query("value") String amount,
            @Query("returnurl") String returnURL
    );

    @GET("PaymentDetails")
    Call<JsonObject> paymentDetails(
            @Query("paymentMethodType") String paymentMethodType,
            @Query("MD") String MD,
            @Query("PaRes") String PaRes,
            @Query("paymentData") String paymentData
    );

    @GET("PaymentDetails")
    Call<JsonObject> paymentDetails(
            @Query("paymentMethodType") String paymentMethodType,
            @Query("payload") String payload
    );

}

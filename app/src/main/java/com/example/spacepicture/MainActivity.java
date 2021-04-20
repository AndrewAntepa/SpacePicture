package com.example.spacepicture;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    TextView desc;
    ImageView image;
    OkHttpClient spaceClient;
    private static final String ADDRESS = "https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY";
    Request spaceRequest;
    Response spaceResponse;
    SpaceInfo spaceInfo = new SpaceInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        desc = findViewById(R.id.descr);
        image = findViewById(R.id.space);

        SpaceTask spaceTask = new SpaceTask();
        spaceTask.execute();
    }


    class SpaceTask extends AsyncTask<String, Void, Response> {
        @Override
        protected Response doInBackground(String... strings) {
            spaceClient = new OkHttpClient();
            HttpUrl.Builder hub = HttpUrl.parse(ADDRESS).newBuilder();
            String url = hub.toString();
            spaceRequest = new Request.Builder().url(url).build();
            try {
                Gson gson = new Gson();
                spaceResponse = spaceClient.newCall(spaceRequest).execute();
                spaceInfo = gson.fromJson(spaceResponse.body().string(), (Type) SpaceInfo.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return spaceResponse;
        }


        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);
            //запросим перевод перед показом текста
            //1 настроить класс - тело и записать в него англ текст
            BodyTranslate[] bodyTranslates = new BodyTranslate[1];
            bodyTranslates[0] = new BodyTranslate();
            bodyTranslates[0].Text = spaceInfo.explanation;
            //построить объект retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(AzureTranslateAPI.address)
                    .build();
            //создать объект интерфейса
            AzureTranslateAPI api = retrofit.create(AzureTranslateAPI.class);
            //отправить запрос
            Call<ResponseTranslate[]> call = api.requestTranslate(bodyTranslates);
            call.enqueue(new ResponseCallBack());

//            desc.setText(spaceInfo.explanation);
            if(spaceInfo.media_type.equals("image")) {
                Picasso.get()
                        .load(spaceInfo.url)
                        .placeholder(R.drawable.spacee)
                        .into(image);
            } else {
                desc.append("Сегодня видео, вот ссылка: " + spaceInfo.url + "\n\n");
            }
        }
    }

    private class ResponseCallBack implements retrofit2.Callback<ResponseTranslate[]> {
        @Override
        public void onResponse(Call<ResponseTranslate[]> call, retrofit2.Response<ResponseTranslate[]> response) {
            if(response.isSuccessful() && response.body()[0].translations.size() != 0){
                String  s = response.body()[0].toString();
                desc.append(s);
            } else
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(Call<ResponseTranslate[]> call, Throwable t) {

        }
    }
}
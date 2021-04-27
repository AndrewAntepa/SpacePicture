package com.example.spacepicture;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class MainActivity extends AppCompatActivity {
    Button speechButton;
    TextView desc;
    ImageView image;
    OkHttpClient spaceClient;
    private static final String ADDRESS = "https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY";
    Request spaceRequest;
    Response spaceResponse;
    SpaceInfo spaceInfo = new SpaceInfo();
    String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        desc = findViewById(R.id.descr);
        image = findViewById(R.id.space);
        speechButton = findViewById(R.id.speechButton);

        SpaceTask spaceTask = new SpaceTask();
        spaceTask.execute();
    }

    public void getVoices(View view) {
        String text = desc.getText().toString();
        Retrofit tokenRet = new Retrofit.Builder()
                .baseUrl(AzureTokenApi.tokenURI)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        AzureTokenApi azureTokenApi = tokenRet.create(AzureTokenApi.class);
        Call<String> callToken = azureTokenApi.getToken();
        callToken.enqueue(new TokenCallBack());
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

    private class TokenCallBack implements retrofit2.Callback<String> {
        @Override
        public void onResponse(Call<String> call, retrofit2.Response<String> response) {
            if(response.isSuccessful()) {
                token = "Bearer " + response.body();
                //следующий объектр ретрофит
                Retrofit voicesRet = new Retrofit.Builder()
                        .baseUrl(AzureVoicesApi.voiceURI)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                AzureVoicesApi azureVoicesApi = voicesRet.create(AzureVoicesApi.class);

                ArrayList<Dictor> dictors = new ArrayList<>();
                Call<ArrayList<Dictor>> callDictors = azureVoicesApi.getDictorsList(token);
                callDictors.enqueue(new DictorsCallBack());
                //Toast.makeText(getApplicationContext(), token, Toast.LENGTH_SHORT).show();
            } else Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(Call<String> call, Throwable t) {
            Toast.makeText(getApplicationContext(), "Что то в программе", Toast.LENGTH_SHORT).show();
        }
    }

    private class DictorsCallBack implements retrofit2.Callback<ArrayList<Dictor>> {
        @Override
        public void onResponse(Call<ArrayList<Dictor>> call, retrofit2.Response<ArrayList<Dictor>> response) {
            ArrayList<Dictor> dictors = new ArrayList<>();
            if(response.isSuccessful()) {
                dictors = response.body();
                for (int i = 0; i < dictors.size(); i++) {
                    desc.append(dictors.get(i).toString());
                    if(dictors.get(i).Locate.equals("ru-RU") && dictors.get(i).ShortName.equals("ru-RU-Pavel")){
                        //запрос на озвучку
                        VoiceChoice voiceChoice = new VoiceChoice();
                        voiceChoice.lang = dictors.get(i).Locate;
                        voiceChoice.voice.lang = dictors.get(i).Locate;
                        voiceChoice.voice.gender = dictors.get(i).Gender;
                        voiceChoice.voice.name = dictors.get(i).ShortName;
                        voiceChoice.voice.text = desc.getText().toString();

                        Retrofit voiceRetrofit = new Retrofit.Builder()
                                .baseUrl(AzureVoicesApi.voiceURI)
                                .addConverterFactory(SimpleXmlConverterFactory.create())
                                .build();
                        AzureVoicesApi azureVoicesApi = voiceRetrofit.create(AzureVoicesApi.class);
                        Call<ResponseBody> callVoice = azureVoicesApi.getVoice(token, voiceChoice);
                        callVoice.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                                //TODO скачать байтовый поток в файл
                                //TODO 1 открыть файл для записи в песочнице
                                //TODO 2 создать байтовый массив на 4096
                                //TODO 3 читать кусок из потока в response.body()
                                //TODO 4 записать прочитанное в файл
                                //TODO 5 после скачивания запустить файл на проигрование
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {

                            }
                        });
                    }
                }
            } else Toast.makeText(getApplicationContext(), Integer.toString(response.code()), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(Call<ArrayList<Dictor>> call, Throwable t) {
            Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
package com.example.weathertest.view;
//@HK KRKY
//https://www.linkedin.com/in/hamza-karakaya-684a101b6/

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.weathertest.R;
import com.example.weathertest.adapter.CustomPagerAdapter;
import com.example.weathertest.adapter.RecyclerViewAdapter;
import com.example.weathertest.anim.ZoomOutPageTransformer;
import com.example.weathertest.database.PlaceNamesDataBase;
import com.example.weathertest.database.PlacesDao;
import com.example.weathertest.databinding.ActivityMainBinding;
import com.example.weathertest.model.WeatherModel;
import com.example.weathertest.service.WeatherAPI;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends FragmentActivity implements SwipeRefreshLayout.OnRefreshListener {

        public static final String BASE_URL ="https://api.openweathermap.org/data/2.5/";
        public static final String AppId ="61e8b0259c092b1b9a15474cd800ee25";
        public static final String FRAGMENT_TAG_ARG = "tag";
        private WeatherAPI weatherAPI;
        private CompositeDisposable compositeDisposable;
        private CompositeDisposable compositeDisposable2;
        private PlaceNamesDataBase dataBase;
        private PlacesDao placesDao;
        private Gson gson;
        private ActivityMainBinding binding;
        private boolean viewPagerState=false;
        private CustomPagerAdapter mCustomPagerAdapter;
        private HashSet<String> CitiesHashSet = new HashSet<>();
        private SharedPreferences preferences;
        private boolean longClicked=false;
        RecyclerViewAdapter recyclerViewAdapter;
        ArrayList<String> sadsa=new ArrayList<>();

        private Subscription subscription;

    @SuppressLint({"CheckResult", "ClickableViewAccessibility"})
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //API initialize
        gson = new GsonBuilder().setLenient().create();
        weatherAPI = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(WeatherAPI.class);

        //SharedPreferences initialize
        preferences = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        CitiesHashSet = (HashSet<String>) preferences.getStringSet("cities",new HashSet<>());



        //DATABASE initialize
        dataBase = Room.databaseBuilder(getApplicationContext(),PlaceNamesDataBase.class,"Places")
                .allowMainThreadQueries()
                .build();
        placesDao = dataBase.placesDao();

        //CompositeDisposible initialize
        compositeDisposable = new CompositeDisposable();
        compositeDisposable2 = new CompositeDisposable();

        //Viewpager initialize
        mCustomPagerAdapter = new CustomPagerAdapter(getSupportFragmentManager());
        binding.mViewPager.setAdapter(mCustomPagerAdapter);
        binding.mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        //RefreshLayout initialize
        binding.refresh.setOnRefreshListener(this);



        if (!internetIsConnected()){
            final AlertDialog dialog = showSomePopup(this, "Internet does not exist.");
            dialog.show();
            initialize().subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                    .doOnSuccess(new Consumer<Boolean>() {
                        @Override
                        public void accept(@NonNull Boolean aBoolean) throws Exception {
                            //dialog.dismiss();
                        }
                    })
                    .subscribe();
        }



        //Get all data in room database
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(placesDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<WeatherModel>>() {
                    @Override
                    public void onSuccess(@NonNull List<WeatherModel> weatherModels) {
                        for (WeatherModel as : weatherModels){
                            mCustomPagerAdapter.addPage(MainFragment.newInstance(as.city.name,as));
                        }
                    }
                    @Override
                    public void onError(@NonNull Throwable e) {
                    }
                }));
    }
    Single<Boolean> initialize() {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<Boolean> e) throws Exception {
                e.onSuccess(true);
            }
        });
    }

    public AlertDialog showSomePopup(Context context, String msg) {
        return new AlertDialog.Builder(context)
                .setTitle("INTERNET")
                .setMessage(msg)
                .setPositiveButton("Close", null).create();

    }
    private boolean internetIsConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public void addButton(View view){
       AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
       LayoutInflater layoutInflater = MainActivity.this.getLayoutInflater();
       View dialogView= layoutInflater.inflate(R.layout.alerdialog_design,null);
       final EditText editText = dialogView.findViewById(R.id.placeName);
       alertDialog.setView(dialogView);
       Button button = dialogView.findViewById(R.id.button);
       AlertDialog dialog = alertDialog.create();
       dialog.show();
       dialog.getWindow().setBackgroundDrawable(null);

       button.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               if (binding.mViewPager.getAdapter().getCount()<10){
                   String cityname = editText.getText().toString().toUpperCase();

                   if (!(CitiesHashSet.contains(cityname))){
                       addNewPlace(cityname);
                       dialog.dismiss();
                       CitiesHashSet.add(cityname.toUpperCase());
                       preferences.edit().putStringSet("cities",CitiesHashSet).apply();
                       viewPagerState = true;
                   }
                   else
                       Toast.makeText(MainActivity.this,"This place already exists.",Toast.LENGTH_LONG).show();
               }
              else
              Toast.makeText(MainActivity.this,"Size exceeded. Please delete one of the registered places.",Toast.LENGTH_LONG).show();
           }
       });
   }


    public void addNewPlace(String cityname){
        //WeatherAPI service = retrofit.create(WeatherAPI.class);
        compositeDisposable2 = new CompositeDisposable();
        compositeDisposable2.add(weatherAPI.getData(cityname,AppId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<WeatherModel>(){
                    @Override
                    public void onComplete() {
                        Toast.makeText(MainActivity.this,"Succes",Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onNext(@NonNull WeatherModel weatherModel) {
                        addToDatabase(weatherModel);
                        String cityName = weatherModel.city.name.toUpperCase();
                        if (cityName.contains("PROVINCE")){
                            String target= String.copyValueOf("PROVINCE".toCharArray());
                            cityName=cityName.replace(target, "");
                        }
                        mCustomPagerAdapter.addPage(MainFragment.newInstance(cityName));
                        if (viewPagerState){
                            binding.mViewPager.setCurrentItem(mCustomPagerAdapter.getCount());
                            mCustomPagerAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this,"Invalid place name",Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void addToDatabase(WeatherModel weatherModel){
        compositeDisposable.add(placesDao.insert(weatherModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @SuppressLint("CheckResult")
            @Override
            public void run() {
               String cityNameArg = mCustomPagerAdapter.getPages().
                       get(binding.mViewPager.getCurrentItem()).
                       getArguments().getString("cityname");

               weatherAPI.getData(cityNameArg,MainActivity.AppId)
                       .subscribeOn(Schedulers.io())
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(MainActivity.this::upDateData);

                binding.refresh.setRefreshing(false);
            }
        }, 150);
    }

    @SuppressLint("CheckResult")
    public void upDateData(WeatherModel weatherModel){
        String cityNameArg = mCustomPagerAdapter.getPages().
                get(binding.mViewPager.getCurrentItem()).
                getArguments().getString("cityname");

        placesDao.upDate(weatherModel.city,weatherModel.list,cityNameArg)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        updateView(cityNameArg);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

    @SuppressLint("CheckResult")
    public void updateView(String ctyName){
        mCustomPagerAdapter.upDatePage(MainFragment.newInstance(ctyName),binding.mViewPager.getCurrentItem());
        placesDao.getByCityName(ctyName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<WeatherModel>() {
                    @Override
                    public void onSuccess(@NonNull WeatherModel weatherModel) {
                        System.out.println("Successful");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}

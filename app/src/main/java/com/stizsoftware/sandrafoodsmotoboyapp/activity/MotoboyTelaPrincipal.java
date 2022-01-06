package com.stizsoftware.sandrafoodsmotoboyapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;
import com.stizsoftware.sandrafoodsmotoboyapp.R;
import com.stizsoftware.sandrafoodsmotoboyapp.adapter.AdapterCardapio;
import com.stizsoftware.sandrafoodsmotoboyapp.adapter.AdapterMotoboyEntregas;
import com.stizsoftware.sandrafoodsmotoboyapp.helper.LocalizacaoMotoboyService;
import com.stizsoftware.sandrafoodsmotoboyapp.helper.RecyclerItemClickListener;
import com.stizsoftware.sandrafoodsmotoboyapp.model.Andamento;
import com.stizsoftware.sandrafoodsmotoboyapp.model.ItemCardapio;
import com.stizsoftware.sandrafoodsmotoboyapp.model.PosicaoMotoboy;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.List;

import static com.thekhaeng.pushdownanim.PushDownAnim.MODE_STATIC_DP;

public class MotoboyTelaPrincipal extends AppCompatActivity {

    /*
    ESSA TELA É A TELA PRINCIPAL PARA O MOTOBOY GERENCIAR AS ENTREGAS.
    AQUI TAMBÉM ACONTECERÁ O COMPARTILHAMENTO DE LOCALIZAÇÃO ATUAL
    PARA SER EXIBIDO NO APP DO CLIENTE.
     */

    private LocationManager locationManager;
    private LocationListener locationListener;
    DatabaseReference firebaseBanco = FirebaseDatabase.getInstance().getReference();
    DatabaseReference posicaoDB;
    DatabaseReference andamentoDB;
    Button btAguardando, btEntregando;
    TextView tvTitulo;

    RecyclerView recyclerView;
    private List<Andamento> listaPedidos = new ArrayList<>(); //em preparacao
    private List<Andamento> listaPedidosEmEntrega = new ArrayList<>(); //em rota de entrega
    private AdapterMotoboyEntregas adapter = new AdapterMotoboyEntregas(listaPedidos, 0);
    private LatLng meuLocal;

    private int parametroQualLista = 0; // 0 - esperando entrega | 1 - entregando

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_motoboy_tela_principal);

        if (checkLocationPermission()) {
            if(checkLocationPermission1()){
                if(checkLocationPermission2()){
                    recuperarLocalizacaoDoUsuario();
                }
            }
        }



        recuperaListaAndamento();
        btAguardando = findViewById(R.id.btMotoboyMostrarEntregar);
        btEntregando = findViewById(R.id.btMotoboyMostrarEntregando);
        tvTitulo = findViewById(R.id.textView23);

        recyclerView = findViewById(R.id.rvMotoboy);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        PushDownAnim.setPushDownAnimTo(btAguardando)
                .setScale( MODE_STATIC_DP, 8  )
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        parametroQualLista = 0;
                        tvTitulo.setBackground(ContextCompat.getDrawable(MotoboyTelaPrincipal.this, R.drawable.background_titulo_fragment));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Window window = getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            window.setStatusBarColor(getResources().getColor(R.color.laranja));
                        }
                        recuperaListaAndamento();
                    }
                });
        PushDownAnim.setPushDownAnimTo(btEntregando)
                .setScale( MODE_STATIC_DP, 8  )
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        parametroQualLista = 1;
                        tvTitulo.setBackground(ContextCompat.getDrawable(MotoboyTelaPrincipal.this, R.drawable.background_titulo_fragment_marrom));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Window window = getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            window.setStatusBarColor(getResources().getColor(R.color.marrom));
                        }
                        recuperaListaAndamento();
                    }
                });
    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION_1 = 199;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION_2 = 299;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Acessar sua Localização")
                        .setMessage("Para acompanhar a sua entrega, é necessário que forneça sua localização. Permitir acesso à sua localização?")
                        .setPositiveButton("Permitir", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MotoboyTelaPrincipal.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    public boolean checkLocationPermission1() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Acessar sua Localização")
                        .setMessage("Para acompanhar a sua entrega, é necessário que forneça sua localização. Permitir acesso à sua localização?")
                        .setPositiveButton("Permitir", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MotoboyTelaPrincipal.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION_1);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION_1);
            }
            return false;
        } else {
            return true;
        }
    }

    public boolean checkLocationPermission2() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Acessar sua Localização")
                        .setMessage("Para acompanhar a sua entrega, é necessário que forneça sua localização. Permitir acesso à sua localização?")
                        .setPositiveButton("Permitir", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MotoboyTelaPrincipal.this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION_2);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION_2);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        recuperarLocalizacaoDoUsuario();
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_LOCATION_1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        recuperarLocalizacaoDoUsuario();
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_LOCATION_2: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        recuperarLocalizacaoDoUsuario();
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }
        }
    }
    public void recuperarLocalizacaoDoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                PosicaoMotoboy nova = new PosicaoMotoboy();
                nova.setLatitude(location.getLatitude());
                nova.setLongitude(location.getLongitude());
                //double latitude = location.getLatitude();
                //double longitude = location.getLongitude();
                meuLocal = new LatLng(nova.getLatitude(), nova.getLongitude());
                posicaoDB = firebaseBanco.child("posicao-motoboy");
                posicaoDB.setValue(nova);
                //posicaoDB.child("latitude").setValue(latitude);
                //posicaoDB.child("longitude").setValue(longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2500,
                    0,
                    locationListener
            );
        }
        /*Intent servIntent = new Intent(this, LocationService.class);
        startService(servIntent);*/
    }

    public void recuperaListaAndamento()
    {
        andamentoDB = firebaseBanco.child("andamento");
        andamentoDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                listaPedidos.clear();
                listaPedidosEmEntrega.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    if(!ds.getValue(Andamento.class).getEnderecoPesquisa().equals("-"))
                    {
                        if(ds.getValue(Andamento.class).getStatus().equals("EM PREPARACAO"))
                        {
                            listaPedidos.add(ds.getValue(Andamento.class));
                        }
                        if(ds.getValue(Andamento.class).getStatus().equals("SAIU"))
                        {
                            listaPedidosEmEntrega.add(ds.getValue(Andamento.class));
                        }
                    }
                }
                configuraRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    public void configuraRecyclerView()
    {
        if(parametroQualLista == 0)
        {
            adapter = new AdapterMotoboyEntregas(listaPedidos, 0);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            adapter = new AdapterMotoboyEntregas(listaPedidosEmEntrega, 1);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        startService( new Intent( this, LocalizacaoMotoboyService.class )) ;
    }

    @Override
    protected void onResume() {
        super.onResume();
        recuperarLocalizacaoDoUsuario();
    }

    @Override
    protected void onStop() {
        super.onStop();
        startService( new Intent( this, LocalizacaoMotoboyService.class )) ;
    }
}
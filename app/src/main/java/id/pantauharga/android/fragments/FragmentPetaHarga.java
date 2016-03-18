package id.pantauharga.android.fragments;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import id.pantauharga.android.Konstan;
import id.pantauharga.android.R;
import id.pantauharga.android.messagebus.MessageAktFrag;
import id.pantauharga.android.modelgson.HargaKomoditasItem;
import id.pantauharga.android.modelgson.HargaKomoditasItemKomparator;
import id.pantauharga.android.parsers.Parseran;

/**
 * Created by Gulajava Ministudio on 11/5/15.
 */
public class FragmentPetaHarga extends Fragment {


    //GOOGLE MAPS
    private SupportMapFragment mapfragment;
    private GoogleMap map;
    private static final int paddingTop_dp = 0;
    private static final int paddingBottom_dp = 140;
    private static int paddingTop_px = 0;
    private static int paddingBottom_px = 0;

    @Bind(R.id.teks_namakomoditas)
    TextView teks_namakomoditas;
    @Bind(R.id.teks_keterangan)
    TextView teks_keterangan;
    @Bind(R.id.teks_lastupdate)
    TextView teks_lastupdate;
    @Bind(R.id.teks_hargakomoditas)
    TextView teks_hargakomoditas;
    @Bind(R.id.teks_alamatlokasi)
    TextView teks_alamatkomoditas;
    @Bind(R.id.teks_nomortelpon)
    TextView teks_telponkomoditas;
    @Bind(R.id.tombol_navigasi)
    FloatingActionButton btnNavigasi;
    @Bind(R.id.tombol_telepon)
    FloatingActionButton btnTelepon;
    @Bind(R.id.tombol_sms)
    FloatingActionButton btnSms;

    //untuk menampilkan marker posisi pengguna
    private LatLng kordinatsaya = null;
    private CameraPosition posisikamerasaya = null;
    private Marker markersaya = null;
    private double latitudesaya = 0;
    private double longitudesaya = 0;
    private Location lokasisaya = null;
    private boolean isMapSiap = false;
    private Date init_last_updated = new Date();
    private SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyy HH:mm");
    //DAFTAR HARGA KOMODITAS

    private List<HargaKomoditasItem> mListKomoditasHarga;
    private List<HargaKomoditasItemKomparator> mListKomoHargaKomparator;

    private Map<Marker, HargaKomoditasItemKomparator> hashmapListHarga;

    //JIKA PETA DIPILIH DARI HALAMAN SEBELAH
    private LatLng kordinatklik = null;
    private CameraPosition posisikameraklik = null;
    private Marker markerklik = null;


    //NAVIGASI KE GOOGLE MAPS
    private String latpeta = "0";
    private String longipeta = "0";

    private Parseran mParseran;
    private int modeUrutan = Konstan.MODE_TERDEKAT;


    //GEOCODER AMBIL LOKASI DAN POSISI PENGGUNA ALAMATNYA JIKA ADA
    private Geocoder geocoderPengguna;
    private List<Address> addressListPengguna = null;
    private String gecoder_alamat = "";
    private String gecoder_namakota = "";
    private String alamatgabungan = "";

    private String init_namakomoditas = "";
    private int init_hargakomoditas = 0;
    private int init_type = 0;
    private String init_keterangan = "";
    private String str_formathargakomoditas = "0";
    private String init_alamatkomoditas = "";
    private String init_telponkomoditas = "";
    private String init_latitudekomoditas = "0";
    private String init_longitudekomoditas = "0";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_petaharga, container, false);
        ButterKnife.bind(FragmentPetaHarga.this, view);

        mParseran = new Parseran(FragmentPetaHarga.this.getActivity());

        mapfragment = (SupportMapFragment) FragmentPetaHarga.this.getChildFragmentManager().findFragmentById(R.id.map);

        btnNavigasi.setOnClickListener(listenerNavigasi);
        btnTelepon.setOnClickListener(listenerTelepon);
        btnSms.setOnClickListener(listenerSms);

        teks_alamatkomoditas.setVisibility(View.GONE);
        teks_telponkomoditas.setVisibility(View.GONE);
        teks_keterangan.setVisibility(View.GONE);


        return view;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(FragmentPetaHarga.this);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(FragmentPetaHarga.this)) {
            EventBus.getDefault().register(FragmentPetaHarga.this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(FragmentPetaHarga.this)) {
            EventBus.getDefault().unregister(FragmentPetaHarga.this);
        }
    }

    public void displayInfo(HargaKomoditasItemKomparator itemloks) {

        init_namakomoditas = itemloks.getBarang();
        init_hargakomoditas = itemloks.getPrice();
        init_telponkomoditas = itemloks.getNohp();
        init_latitudekomoditas = itemloks.getLatitude();
        init_longitudekomoditas = itemloks.getLongitude();
        init_keterangan = itemloks.getKeterangan();
        init_last_updated = itemloks.getLastUpdated();
        init_type = itemloks.getType();
        String namakomoditastype = "";
        if (init_type == 2) {
            namakomoditastype = init_namakomoditas + "(Beli)";
        } else if (init_type == 1) {
            namakomoditastype = init_namakomoditas + "(Jual)";
        } else {
            namakomoditastype = init_namakomoditas + "(Pantau)";
        }

        teks_namakomoditas.setText(namakomoditastype);

        str_formathargakomoditas = "Rp " + mParseran.formatAngkaPisah(init_hargakomoditas) + ",-";
        teks_hargakomoditas.setText(str_formathargakomoditas);

        if (init_telponkomoditas.length() > 4) {
            String teksets = "Telp : " + init_telponkomoditas;
            teks_telponkomoditas.setText(teksets);
            teks_telponkomoditas.setVisibility(View.VISIBLE);
            btnTelepon.setVisibility(View.VISIBLE);
            btnSms.setVisibility(View.VISIBLE);
        } else {
            String teksets = "Telp : -";
            teks_telponkomoditas.setText(teksets);
            teks_telponkomoditas.setVisibility(View.GONE);
            btnTelepon.setVisibility(View.GONE);
            btnSms.setVisibility(View.GONE);
        }

        if (init_keterangan != null && !init_keterangan.isEmpty()) {
            teks_keterangan.setText("Keterangan : \n" + init_keterangan);
            teks_keterangan.setVisibility(View.VISIBLE);
        } else {
            teks_keterangan.setText("Keterangan : -");
            teks_keterangan.setVisibility(View.GONE);
        }
        teks_lastupdate.setText("Last Updated : " + formatDate.format(init_last_updated));
    }


    /**
     * LISTENER EVENTBUS PESAN DARI AKTIVITAS UTAMA
     ***/
    public void onEvent(MessageAktFrag messageAktFrag) {

        int kodepesan = messageAktFrag.getKode();

        switch (kodepesan) {

            case Konstan.KODE_LISTBARU:

                mListKomoditasHarga = messageAktFrag.getListHargaKomoditas();
                lokasisaya = messageAktFrag.getLocation();
                modeUrutan = messageAktFrag.getModelist();

                //segarkan daftars
                cekDaftarHasil();

                break;
        }
    }


    //CEK DAFTAR
    private void cekDaftarHasil() {

        if (mListKomoditasHarga != null && mListKomoditasHarga.size() > 0) {

            //ambil list dan tampilkan ke peta
            parseKomparatorPeta();
        }
    }


    //UBAH MENJADI BENTUK JARAK TERDEKAT DENGAN KOMPARATOR DKK
    private void parseKomparatorPeta() {

        //setel indikator memuat peta

        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                mListKomoHargaKomparator = mParseran.parseListKomparator(mListKomoditasHarga, lokasisaya,
                        modeUrutan);

                return null;
            }
        }).continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {

                if (mListKomoHargaKomparator != null && mListKomoHargaKomparator.size() > 0) {

                    Log.w("LOG PETA HARGA", "GET MAP ASYNC");
                    mapfragment.getMapAsync(mOnMapReadyCallback);

                } else {

                    //peta gagal dimuat
                    munculSnackbar(R.string.toastgagalpeta);

                }

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }


    OnMapReadyCallback mOnMapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {

            map = googleMap;
            isMapSiap = true;

            hitungSkalaAtasBawah();
            map.setPadding(0, paddingTop_px, 0, paddingBottom_px);
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);

            //setel posisi saya awal
            //setel peta marker
            setelPetaMarker();

            map.setOnMarkerClickListener(listenermarker);
        }
    };


    private void hitungSkalaAtasBawah() {

        final float scale = getResources().getDisplayMetrics().density;

        //padding atas
        paddingTop_px = (int) (paddingTop_dp * scale + 0.5f);
        //padding bawah
        paddingBottom_px = (int) (paddingBottom_dp * scale + 0.5f);

    }


    //SETEL POSISI SAYA
    public void setelPosisiSayaAwal() {

        try {

            if (markersaya != null) {
                markersaya.remove();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        Log.w("LOKASI", "lokasi saya peta " + latitudesaya + " , " + longitudesaya);

        kordinatsaya = new LatLng(latitudesaya, longitudesaya);
        posisikamerasaya = new CameraPosition.Builder().target(kordinatsaya)
                .zoom(16)
                .bearing(0).tilt(0).build();

        markersaya = map.addMarker(new MarkerOptions()
                .position(kordinatsaya)
                .title("Posisi Saya")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_lokasi_saya)));

        map.moveCamera(CameraUpdateFactory.newCameraPosition(posisikamerasaya));
        markersaya.showInfoWindow();
    }


    public void setelPosisiSayaMenu(Location location) {

        Log.w("LOG PETA HARGA", "SETEL POSISI SAYA MARKER");
        if (location != null) {

            lokasisaya = location;
            latitudesaya = lokasisaya.getLatitude();
            longitudesaya = lokasisaya.getLongitude();

            Log.w("LOKASI", "lokasi saya peta " + latitudesaya + " , " + longitudesaya);
            if (isMapSiap) {
                try {
                    setelPosisiSayaAwal();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }


    public void setelPetaMarker() {

        Log.w("LOG PETA HARGA", "SETEL PETA MARKER");
        if (lokasisaya != null) {

            latitudesaya = lokasisaya.getLatitude();
            longitudesaya = lokasisaya.getLongitude();

            Log.w("LOKASI", "lokasi saya peta " + latitudesaya + " , " + longitudesaya);

        }

        if (isMapSiap) {

            if (mListKomoHargaKomparator != null && mListKomoHargaKomparator.size() > 0) {

                //setel ke marker
                setelMarkerSemua();

            } else {
                setelPosisiSayaAwal();
                teks_namakomoditas.setText("Nama tidak tersedia");
                teks_hargakomoditas.setText("Harga tidak tersedia");
                teks_alamatkomoditas.setText("Alamat tidak tersedia");
                teks_telponkomoditas.setText("Telepon tidak tersedia");
                teks_keterangan.setText("Tidah ada keterangan");
            }
        }

    }


    //TAMPILKAN KE DALAM MARKER

    /**
     * SETEL DAN PASANG MARKER SEMUA KE PETA *
     */
    private void setelMarkerSemua() {

        try {

            int panjangarray = mListKomoHargaKomparator.size();
            HargaKomoditasItemKomparator lokitem;
            double dolatitude = 0;
            double dolongitude = 0;
            hashmapListHarga = new HashMap<>();

            String loops_namakomoditas = "";
            int loops_type = 0;
            map.clear();
            setelPosisiSayaAwal();


            //ambil data awal untuk inisialisasi keterangan
            displayInfo(mListKomoHargaKomparator.get(0));

            //task ambil geocoder
            taskAmbilGeocoder(init_latitudekomoditas, init_longitudekomoditas);


            //setel ke peta
            for (int i = 0; i < panjangarray; i++) {

                lokitem = mListKomoHargaKomparator.get(i);
                loops_namakomoditas = lokitem.getBarang();
                loops_type = lokitem.getType();
                String namakomo = "";
                int icon = 0;
                if (loops_type == 2) {
                    namakomo = loops_namakomoditas + "(B)";
                    icon = R.drawable.ic_buy;
                } else if (loops_type == 1) {
                    namakomo = loops_namakomoditas + "(J)";
                    icon = R.drawable.ic_sell;
                } else {
                    namakomo = loops_namakomoditas + "(P)";
                    icon = R.drawable.ic_pantau;
                }

                dolatitude = Double.valueOf(lokitem.getLatitude());
                dolongitude = Double.valueOf(lokitem.getLongitude());

                LatLng latlnglokasi = new LatLng(dolatitude, dolongitude);

                MarkerOptions markeropsi = new MarkerOptions();
                markeropsi.position(latlnglokasi);
                markeropsi.title(namakomo);
                markeropsi.icon(BitmapDescriptorFactory.fromResource(icon));

                Marker markeradd = map.addMarker(markeropsi);

                hashmapListHarga.put(markeradd, lokitem);
            }

            btnNavigasi.setVisibility(View.VISIBLE);


        } catch (Exception ex) {
            ex.printStackTrace();
            btnNavigasi.setVisibility(View.GONE);
        }
    }


    //SETEL PILIHAN DARI FRAGMENT SEBELAH KE DALAM PETA
    public void setelMarkerSemuaPilihanKlik(int posisiklik) {

        try {

            int panjangarray = mListKomoHargaKomparator.size();
            HargaKomoditasItemKomparator lokitem;
            double dolatitude = 0;
            double dolongitude = 0;
            hashmapListHarga = new HashMap<>();
            Marker markeradd;

            String loops_namakomoditas = "";
            int loops_type = 0;

            map.clear();
            setelPosisiSayaAwal();


            //ambil data awal untuk inisialisasi keterangan
            displayInfo(mListKomoHargaKomparator.get(posisiklik));

            //task ambil geocoder
            taskAmbilGeocoder(init_latitudekomoditas, init_longitudekomoditas);

            //setel ke peta
            for (int i = 0; i < panjangarray; i++) {

                lokitem = mListKomoHargaKomparator.get(i);

                loops_namakomoditas = lokitem.getBarang();
                loops_type = lokitem.getType();
                String namakomo = "";
                int icon2 = 0;
                if (loops_type == 2) {
                    namakomo = loops_namakomoditas + "(B)";
                    icon2 = R.drawable.ic_buy;
                } else if (loops_type == 1) {
                    namakomo = loops_namakomoditas + "(J)";
                    icon2 = R.drawable.ic_sell;
                } else {
                    namakomo = loops_namakomoditas + "(P)";
                    icon2 = R.drawable.ic_pantau;
                }
                dolatitude = Double.valueOf(lokitem.getLatitude());
                dolongitude = Double.valueOf(lokitem.getLongitude());

                LatLng latlnglokasi = new LatLng(dolatitude, dolongitude);

                MarkerOptions markeropsi = new MarkerOptions();
                markeropsi.position(latlnglokasi);
                markeropsi.title(namakomo);
                markeropsi.icon(BitmapDescriptorFactory.fromResource(icon2));


                if (posisiklik == i) {

                    latpeta = dolatitude + "";
                    longipeta = dolongitude + "";


                    posisikameraklik = new CameraPosition.Builder().target(latlnglokasi)
                            .zoom(16)
                            .bearing(0).tilt(0).build();

                    map.moveCamera(CameraUpdateFactory.newCameraPosition(posisikameraklik));

                    markeradd = map.addMarker(markeropsi);
                    markerklik = markeradd;
                } else {
                    markeradd = map.addMarker(markeropsi);
                }


                hashmapListHarga.put(markeradd, lokitem);
            }

            btnNavigasi.setVisibility(View.VISIBLE);

            markerklik.showInfoWindow();
        } catch (Exception ex) {
            ex.printStackTrace();
            btnNavigasi.setVisibility(View.GONE);
        }
    }


    //LISTENER KLO MARKER DI KLIK
    GoogleMap.OnMarkerClickListener listenermarker = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {

            marker.showInfoWindow();

            //tampilkan keterangan marker
            try {
                displayInfo(hashmapListHarga.get(marker));

                //task ambil geocoder
                taskAmbilGeocoder(latpeta, longipeta);

                btnNavigasi.setVisibility(View.VISIBLE);

            } catch (Exception ex) {
                ex.printStackTrace();

                btnNavigasi.setVisibility(View.GONE);
            }


            return true;
        }
    };


    View.OnClickListener listenerNavigasi = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            String kordinatpetakirim = init_latitudekomoditas + "," + init_longitudekomoditas;
            String kordinatsaya = latitudesaya + "," + longitudesaya;
            String alamatpeta = "http://maps.google.com/maps?saddr=" + kordinatsaya + "&daddr=" + kordinatpetakirim + "&mode=driving";

            Log.w("ALAMAT PETA BUKA", "" + alamatpeta);
            Toast.makeText(FragmentPetaHarga.this.getActivity(), "Membuka Google Maps", Toast.LENGTH_SHORT).show();

            try {

                Intent intentpeta = new Intent(Intent.ACTION_VIEW);
                intentpeta.setData(Uri.parse(alamatpeta));
                intentpeta.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                FragmentPetaHarga.this.startActivity(intentpeta);

            } catch (Exception ex) {
                ex.printStackTrace();

                Intent intentpeta = new Intent(Intent.ACTION_VIEW);
                intentpeta.setData(Uri.parse(alamatpeta));
                FragmentPetaHarga.this.startActivity(intentpeta);
            }

        }
    };

    View.OnClickListener listenerTelepon = new View.OnClickListener() {
        public void onClick(View view) {
            if (init_telponkomoditas.length() > 4) {
                Intent in = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + init_telponkomoditas));
                try {
                    startActivity(in);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(FragmentPetaHarga.this.getActivity(), "Call Activity is not founded", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    View.OnClickListener listenerSms = new View.OnClickListener() {
        public void onClick(View view) {
            if (init_telponkomoditas.length() > 4) {
                Intent in = new Intent(Intent.ACTION_VIEW);
                in.setType("vnd.android-dir/mms-sms");
                in.putExtra("address", init_telponkomoditas);
                in.putExtra("sms_body","#pantauharga.id");
                try {
                    startActivity(in);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(FragmentPetaHarga.this.getActivity(), "Call Activity is not founded", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    //AMBIL GEOCODER PENGGUNA
    private void ambilGeocoderPengguna(String latitude, String longitude) {

        geocoderPengguna = new Geocoder(FragmentPetaHarga.this.getActivity(), Locale.getDefault());
        double dolatitu = 0;
        double dolongi = 0;

        try {
            dolatitu = Double.valueOf(latitude);
            dolongi = Double.valueOf(longitude);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {


            addressListPengguna = geocoderPengguna.getFromLocation(dolatitu, dolongi, 1);
            if (addressListPengguna.size() > 0) {

                int panjangalamat = addressListPengguna.get(0).getMaxAddressLineIndex();

                if (panjangalamat > 0) {

                    gecoder_alamat = addressListPengguna.get(0).getAddressLine(0);
                    gecoder_namakota = addressListPengguna.get(0).getLocality();

                } else {
                    gecoder_alamat = "";
                    gecoder_namakota = "";
                    alamatgabungan = "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            gecoder_alamat = "";
            gecoder_namakota = "";
            alamatgabungan = "";
        }

        Log.w("NAMA KOTA", "NAMA KOTA " + gecoder_alamat + " " + gecoder_namakota);

        if (gecoder_namakota != null && gecoder_namakota.length() > 0) {

            alamatgabungan = gecoder_namakota;

            if (gecoder_alamat != null && gecoder_alamat.length() > 0) {

                alamatgabungan = gecoder_alamat + ", " + gecoder_namakota;

            }
        } else {
            alamatgabungan = "";
        }

    }


    //TASK AMBIL GEOCODER
    private void taskAmbilGeocoder(final String strlatitude, final String strlongitude) {

        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                ambilGeocoderPengguna(strlatitude, strlongitude);

                return null;
            }
        }).continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {

                if (alamatgabungan.length() > 4) {
                    Log.w("ALAMAT GABUNGAN TASK", "ALAMAT " + alamatgabungan);
                    teks_alamatkomoditas.setText(alamatgabungan);
                    teks_alamatkomoditas.setVisibility(View.VISIBLE);
                } else {
                    teks_alamatkomoditas.setText("-");
                    teks_alamatkomoditas.setVisibility(View.GONE);
                }

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);


    }


    //MUNCUL SNACKBAR
    private void munculSnackbar(int resPesan) {

        Snackbar.make(teks_namakomoditas, resPesan, Snackbar.LENGTH_LONG).setAction("OK", listenersnackbar)
                .setActionTextColor(FragmentPetaHarga.this.getResources().getColor(R.color.kuning_indikator)).show();
    }

    View.OnClickListener listenersnackbar = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };


}

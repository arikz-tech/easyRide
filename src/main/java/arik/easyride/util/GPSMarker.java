package arik.easyride.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import arik.easyride.R;

public class GPSMarker implements LocationListener {

    private GoogleMap mMap;
    private Context context;

    public GPSMarker(Context context, GoogleMap mMap) {
        this.mMap = mMap;
        this.context = context;
    }



    @Override
    public void onLocationChanged(@NonNull Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions marker = new MarkerOptions()
                .position(latLng)
                .icon(bitmapDescriptorFromVector(R.drawable.ic_marker_16));
        mMap.addMarker(marker);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    private BitmapDescriptor bitmapDescriptorFromVector(int resId) {
        Drawable vector = ContextCompat.getDrawable(context, resId);
        if (vector != null) {
            vector.setBounds(0, 0, vector.getIntrinsicWidth() * 2, vector.getIntrinsicHeight() * 2);
            Bitmap bitmap = Bitmap.createBitmap(vector.getIntrinsicWidth() * 2, vector.getIntrinsicHeight() * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vector.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }

        return null;
    }

}

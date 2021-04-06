package arik.easyride.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.List;

public class Navigate extends ContextWrapper {

    private Intent mIntent;
    private static final String MARKET_LINK = "market://details?id=%s";

    private static final Intent GOOGLE = new Intent(Intent.ACTION_VIEW)
            .setPackage("com.google.android.apps.maps");

    private static final Intent WAZE = new Intent(Intent.ACTION_VIEW)
            .setPackage("com.waze");

    public enum Apps {
        GOOGLE, WAZE;
    }

    private static final String GOOGLE_NAVIGATION = "google.navigation:ll=%s,%s";

    private static final String GOOGLE_MAP_NAVIGATION = "http://maps.google.com/maps?%s"; //saddr=%1$s&daddr=%2$s+to:%3$s

    //Waze api for delivery to weze next point coordinates
    private static final String WAZE_NAVIGATION = "waze://?ll=%s,%s&navigate=yes";

    public Navigate(Context base) {
        super(base);
    }

    public Navigate setDestination(final Apps flag, List<LatLng> points) {
        mIntent = new Intent();

        switch (flag) {
            case GOOGLE:
                if (points.size() == 1) {
                    mIntent = GOOGLE.setData(Uri.parse(String.format(GOOGLE_NAVIGATION, points.get(0).latitude, points.get(0).longitude)));
                    break;
                }
                StringBuilder google = new StringBuilder();
                int i = 0;
                for (LatLng point : points) {
                    switch (i) {
                        case 0:
                            google.append("&daddr=").append(point.toString());
                            break;
                        default:
                            google.append("+to:").append(point.toString());
                            break;
                    }
                    i++;
                }
                mIntent = GOOGLE.setData(Uri.parse(String.format(GOOGLE_MAP_NAVIGATION, google.toString())));
                break;

            case WAZE:
                mIntent = WAZE.setData(Uri.parse(String.format(WAZE_NAVIGATION, points.get(0).latitude, points.get(0).longitude)));
                break;
        }

        return this;
    }

    public void checkPackage(Apps flag) throws ActivityNotFoundException {
        Intent intent;
        switch (flag) {
            case GOOGLE:
                intent = GOOGLE;
                break;

            case WAZE:
                intent = WAZE;
                break;

            default:
                return;
        }

        if (!isPackageInstalled(intent)) {
            openMarket(intent);
        }
    }

    public void guideMe(boolean install) throws ActivityNotFoundException {
        if (isPackageInstalled(mIntent)) {
            startActivity(mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            if (install) {
                openMarket(mIntent);
            } else {
                throw new ActivityNotFoundException();
            }
        }
    }

    private boolean isPackageInstalled(Intent intent) {
        try {
            return getApplicationContext().getPackageManager().getPackageInfo(intent.getPackage(), 0).packageName.equalsIgnoreCase(intent.getPackage());
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openMarket(Intent intent) {
        startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(MARKET_LINK, intent.getPackage())))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        );
    }

    public static class LatLng {
        public double latitude;
        public double longitude;

        public LatLng(double var1, double var3) {
            if (-180.0D <= var3 && var3 < 180.0D) {
                this.longitude = var3;
            } else {
                this.longitude = ((var3 - 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D;
            }
            this.latitude = Math.max(-90.0D, Math.min(90.0D, var1));
        }


        public final int hashCode() {
            long var2 = Double.doubleToLongBits(this.latitude);
            int var1 = 31 + (int) (var2 ^ var2 >>> 32);
            var2 = Double.doubleToLongBits(this.longitude);
            return var1 * 31 + (int) (var2 ^ var2 >>> 32);
        }

        public final boolean equals(Object var1) {
            if (this == var1) {
                return true;
            } else if (!(var1 instanceof LatLng)) {
                return false;
            } else {
                LatLng var2 = (LatLng) var1;
                return Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(var2.latitude) && Double.doubleToLongBits(this.longitude) == Double.doubleToLongBits(var2.longitude);
            }
        }

        public final String toString() {
            double var1 = this.latitude;
            double var3 = this.longitude;
            return String.format("%s, %s", var1, var3);
        }
    }

}

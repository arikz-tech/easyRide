package arik.easyride.util;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KMeans {

    private static final double EARTH_RADIUS = 6378137;

    private List<Point> points;
    private List<Cluster> clusters;

    public KMeans(List<Point> points, int numberOfClusters) {
        this.points = points;
        clusters = new ArrayList<>();
        initializeCentroid(numberOfClusters);
    }

    private static Point ConvertLatLngToPoint(LatLng latLngPoint) {
        double latRad = Math.toRadians(latLngPoint.latitude);
        double lngRad = Math.toRadians(latLngPoint.longitude);
        double x = EARTH_RADIUS * lngRad;
        double y = EARTH_RADIUS * Math.log(Math.tan((Math.PI / 4) + (latRad / 2)));
        return new Point(x, y);
    }

    private static LatLng convertPointToLatLng(Point point) {
        double x = point.getX();
        double y = point.getY();
        double lng = x / EARTH_RADIUS;
        double lat = (2 * Math.atan(Math.exp((y / EARTH_RADIUS)))) - (Math.PI / 2);
        double latDegree = Math.toDegrees(lat);
        double lngDegree = Math.toDegrees(lng);
        return new LatLng(latDegree, lngDegree);
    }

    private void initializeCentroid(int numberOfClusters) {
        for (Point point : points) {
            if (numberOfClusters == 0)
                break;
            else {
                Cluster newCluster = new Cluster(new Point(point.getX(), point.getY()));
                clusters.add(newCluster);
                numberOfClusters--;
            }
        }
    }

    public void startCluster() {
        for (int i = 0; i < 30; i++) {
            iteration();
        }
    }

    private void iteration() {
        for (Point point : points) {
            double minDistanceValue = Double.MAX_VALUE;
            Cluster minDistanceCluster = clusters.get(0);

            for (Cluster cluster : clusters) {
                double clusterDistance = euclideanDistance(cluster.getCentroidPoint(), point);
                if (clusterDistance < minDistanceValue) {
                    minDistanceValue = clusterDistance;
                    minDistanceCluster = cluster;
                }
            }

            minDistanceCluster.updateCentroidPoint(point);
            for (Cluster cluster : clusters) {
                List<Point> clusterPoints = cluster.getPoints();
                clusterPoints.remove(point);
            }
            minDistanceCluster.getPoints().add(point);
        }
    }

    private static double euclideanDistance(Point point1, Point point2) {
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static List<Point> convertToPoints(List<LatLng> latLngPoints) {
        List<Point> pointsList = new ArrayList<>();

        for (LatLng latLngPoint : latLngPoints) {
            Point newPoint = ConvertLatLngToPoint(latLngPoint);
            pointsList.add(newPoint);
        }

        return pointsList;
    }

    public List<LatLng> latLngCentroidClusters() {
        List<LatLng> latLngList = new ArrayList<>();
        for (Cluster cluster : clusters) {
            Point point = cluster.getCentroidPoint();
            LatLng latLng = convertPointToLatLng(point);
            latLngList.add(latLng);
        }
        return latLngList;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }

    public static class Point {
        private double x;
        private double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    class Cluster {
        private Point centroidPoint;
        private List<Point> points;

        public Cluster(Point centroidPoint) {
            this.centroidPoint = centroidPoint;
            points = new ArrayList<>();
        }

        public void updateCentroidPoint(Point point) {
            double centroidX = centroidPoint.getX();
            double centroidY = centroidPoint.getY();
            double pointX = point.getX();
            double pointY = point.getY();
            centroidPoint.setX((centroidX + pointX) / 2);
            centroidPoint.setY((centroidY + pointY) / 2);
        }

        public Point getCentroidPoint() {
            return centroidPoint;
        }

        public void setCentroidPoint(Point centroidPoint) {
            this.centroidPoint = centroidPoint;
        }

        public List<Point> getPoints() {
            return points;
        }

        public void setPoints(List<Point> points) {
            this.points = points;
        }

        @Override
        public String toString() {
            return "Cluster{" +
                    "centroidPoint=" + centroidPoint +
                    ", points=" + points +
                    '}';
        }
    }

}

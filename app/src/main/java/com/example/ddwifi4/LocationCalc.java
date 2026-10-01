package com.example.ddwifi4;

public class LocationCalc {
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球の半径 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // メートルに変換

        return distance;
    }

    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lonDifference = Math.toRadians(lon2 - lon1);
        double y = Math.sin(lonDifference) * Math.cos(Math.toRadians(lat2));
        double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                - Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(lonDifference);
        double bearing = Math.toDegrees(Math.atan2(y, x));

        // 0～360度の範囲に変換
        return (bearing + 360) % 360;
    }

    public static String getDirectionFromBearing(double bearing) {
        String[] directions = {"北", "北北東", "北東", "東北東", "東", "東南東", "南東", "南南東",
                "南", "南南西", "南西", "西南西", "西", "西北西", "北西", "北北西"};

        // 360度を16等分して、方位に割り当てる
        int index = (int) Math.round(bearing / 22.5) % 16;
        return directions[index];
    }
}

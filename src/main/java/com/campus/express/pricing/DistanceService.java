package com.campus.express.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DistanceService {

    private static final double EARTH_RADIUS_M = 6_371_000D;

    @Value("${app.map.road-factor:1.25}")
    private double roadFactor;

    /** Haversine 直线距离 × 道路折线系数，最小按 100 米计 */
    public int meters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return 800;
        }
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double deltaLat = radLat2 - radLat1;
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        int meters = (int) Math.round(EARTH_RADIUS_M * c * roadFactor);
        return Math.max(100, meters);
    }
}

package iuh.fit.goat.service;

import jakarta.servlet.http.HttpServletRequest;

public interface DeviceInfoService {
    String getDeviceName(HttpServletRequest request);
    String getIp(HttpServletRequest request);
}

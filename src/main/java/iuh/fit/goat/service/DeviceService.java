package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.device.DeviceResponse;
import iuh.fit.goat.entity.Device;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

public interface DeviceService {
    String getDeviceName(HttpServletRequest request);

    String getIp(HttpServletRequest request);

    void handleUpsertDevice(Device device);

    ResultPaginationResponse handleGetAllDevicesByEmail(Pageable pageable);

    DeviceResponse handleConvertToDeviceResponse(Device device);
}

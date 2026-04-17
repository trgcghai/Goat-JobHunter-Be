package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.device.DeviceResponse;
import iuh.fit.goat.entity.Device;
import iuh.fit.goat.repository.DeviceRepository;
import iuh.fit.goat.service.DeviceService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    private final UserAgentAnalyzer analyzer = UserAgentAnalyzer
                    .newBuilder()
                    .hideMatcherLoadStats()
                    .withCache(10000)
                    .build();

    @Override
    public String getDeviceName(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        UserAgent agent = this.analyzer.parse(ua);

        String browser = agent.getValue("AgentName");
        String version = agent.getValue("AgentVersion");
        String os = agent.getValue("OperatingSystemName");
        String device = agent.getValue("DeviceClass");

        return browser + " " + version + " - " + os + " (" + device + ")";
    }

    @Override
    public String getIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");

        if (xf != null) return xf.split(",")[0];

        return request.getRemoteAddr();
    }

    @Override
    public void handleUpsertDevice(Device device) {
        Device sameDevice = this.deviceRepository.findByNameAndAccount_EmailAndDeletedAtIsNull(device.getName(), device.getAccount().getEmail());

        if(sameDevice != null) {
            sameDevice.setUpdatedAt(Instant.now());
            this.deviceRepository.save(sameDevice);
        } else {
            this.deviceRepository.save(device);
        }
    }

    @Override
    public ResultPaginationResponse handleGetAllDevicesByEmail(Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        Specification<Device> spec = (root, query, cb)
                -> cb.and(
                        cb.equal(root.get("account").get("email"), currentEmail),
                        cb.isNull(root.get("deletedAt"))
                );

        Page<Device> page = this.deviceRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<DeviceResponse> devices = page.getContent().stream().map(this :: handleConvertToDeviceResponse).toList();

        return new ResultPaginationResponse(meta, devices);
    }

    @Override
    public DeviceResponse handleConvertToDeviceResponse(Device device) {
        return new DeviceResponse(
                device.getDeviceId(),
                device.getName(),
                device.getUpdatedAt() != null ? device.getUpdatedAt() : device.getCreatedAt(),
                new DeviceResponse.AccountDevice(
                        device.getAccount().getAccountId(),
                        device.getAccount().getEmail()
                )
        );
    }
}

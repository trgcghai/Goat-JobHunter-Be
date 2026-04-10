package iuh.fit.goat.service.impl;

import iuh.fit.goat.service.DeviceInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceInfoServiceImpl implements DeviceInfoService {

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
}

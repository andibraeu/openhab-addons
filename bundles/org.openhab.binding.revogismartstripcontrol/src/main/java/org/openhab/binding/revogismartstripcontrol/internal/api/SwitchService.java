package org.openhab.binding.revogismartstripcontrol.internal.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openhab.binding.revogismartstripcontrol.internal.udp.DatagramSocketWrapper;
import org.openhab.binding.revogismartstripcontrol.internal.udp.UdpSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class SwitchService {

    private static final String UDP_DISCOVERY_QUERY = "V3{\"sn\":\"%s\", \"cmd\": 20, \"port\": %d, \"state\": %d}";
    private static final String VERSION_STRING = "V3";
    private final Logger logger = LoggerFactory.getLogger(SwitchService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UdpSenderService udpSenderService;

    public SwitchService() {
        udpSenderService = new UdpSenderService(new DatagramSocketWrapper());
    }

    public SwitchService(UdpSenderService udpSenderService) {
        this.udpSenderService = udpSenderService;
    }

    public SwitchResponse switchPort(String serialNumber, int port, int state) {
        if (state < 0 || state > 1) {
            logger.warn("state value is not valid: {}", state);
            throw new IllegalArgumentException("state has to be 0 or 1");
        }
        if (port < 0) {
            logger.warn("port doesn't exist on device: {}", port);
            throw new IllegalArgumentException("Given port doesn't exist");
        }

        List<String> responses = udpSenderService.broadcastUpdDatagram(String.format(UDP_DISCOVERY_QUERY, serialNumber, port, state));
        responses.forEach(response -> {
            logger.info("Reveived {}", response);
        });
        return responses.stream()
                .filter(response -> !response.isEmpty())
                .map(this::deserializeString)
                .filter(switchResponse -> switchResponse.getCode() == 200)
                .findFirst()
                .orElse(new SwitchResponse(0, 503));
    }

    private SwitchResponse deserializeString(String response) {
        String extractedJsonResponse = response.substring(response.lastIndexOf(VERSION_STRING) + 2);
        try {
            return objectMapper.readValue(extractedJsonResponse, SwitchResponse.class);
        } catch (IOException e) {
            logger.warn("Could not parse string \"{}\" to SwitchResponse", response);
            return new SwitchResponse(0, 503);
        }
    }
}
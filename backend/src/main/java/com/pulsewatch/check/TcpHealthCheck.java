package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class TcpHealthCheck implements HealthCheck {

    @Override
    public MonitorType type() {
        return MonitorType.TCP;
    }

    @Override
    public CheckOutcome check(Monitor monitor) {
        long start = System.nanoTime();
        String target = monitor.getTarget();
        int separator = target.lastIndexOf(':');
        if (separator <= 0) {
            return CheckOutcome.down(0, "Invalid target, expected host:port");
        }
        String host = target.substring(0, separator);
        int timeoutMillis = monitor.getTimeoutSeconds() * 1000;

        try {
            int port = Integer.parseInt(target.substring(separator + 1));
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            }
            return CheckOutcome.up(elapsedMillis(start));
        } catch (SocketTimeoutException e) {
            return CheckOutcome.down(elapsedMillis(start),
                    "Timed out after " + monitor.getTimeoutSeconds() + " seconds");
        } catch (IOException e) {
            return CheckOutcome.down(elapsedMillis(start), e);
        } catch (IllegalArgumentException e) {
            return CheckOutcome.down(0, "Invalid target, expected host:port");
        }
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
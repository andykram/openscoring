package org.openscoring.standalone.metrics.statsd;

import org.openscoring.standalone.metrics.reporter.client.BaseReporterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class StatsD extends BaseReporterClient {

    private Writer writer;
    private DatagramSocket socket;

    private final ByteArrayOutputStream outputStream;
    private final UDPSocketProvider socketProvider;

    protected static final int OUTPUT_STREAM_BUFFER_SIZE = 32;

    private static final Logger LOG = LoggerFactory.getLogger(StatsD.class);
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public StatsD(String host, int port) {
        outputStream = new ByteArrayOutputStream(OUTPUT_STREAM_BUFFER_SIZE);
        socketProvider = new UDPSocketProviderImpl(host, port);
    }

    @Override
    public void connect() throws IllegalStateException, IOException {
        if (writer != null) {
            throw new IllegalStateException("Already connected");
        }

        this.outputStream.reset();
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8));
        try {
            this.socket = socketProvider.getSocket();
        } catch (Exception e) {
            LOG.error("Error writing to StatsD: {}", e.getMessage());
        }
    }

    protected final static String KV_FORMAT = "%s:%s";

    @Override
    public void send(String name, String value, long timestamp) throws IOException {
        if (writer == null) {
            throw new IOException("No writer available!");
        }
        if (outputStream.size() > 0) {
            writer.append('\n');
        }
        writer.write(String.format(KV_FORMAT, sanitize(name), sanitize(value)));
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            if (writer != null) {
                writer.flush();
                final DatagramPacket packet = this.socketProvider.fillPacketWith(outputStream);
                this.socket.send(packet);
            }
            if (socket != null) {
                socket.close();
            }
        } finally {
            this.socket = null;
            this.writer = null;
        }
    }

    public interface UDPSocketProvider {
        DatagramSocket getSocket() throws Exception;
        DatagramPacket getPacketFor(ByteArrayOutputStream out);
        DatagramPacket fillPacketWith(ByteArrayOutputStream out);
    }

    public static class UDPSocketProviderImpl implements UDPSocketProvider {

        private final String host;
        private final Integer port;

        public UDPSocketProviderImpl(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public DatagramSocket getSocket() throws Exception {
            return new DatagramSocket();
        }

        @Override
        public DatagramPacket getPacketFor(ByteArrayOutputStream out) {
            final byte[] buf = (out != null) ? out.toByteArray() : new byte[OUTPUT_STREAM_BUFFER_SIZE];

            try {
                return new DatagramPacket(buf, buf.length, InetAddress.getByName(host), port);
            } catch (UnknownHostException e) {
                return null;
            }
        }

        @Override
        public DatagramPacket fillPacketWith(ByteArrayOutputStream out) {
            final DatagramPacket packet = getPacketFor(out);
            if (packet != null) {
                packet.setData(out.toByteArray());
            }

            return packet;
        }
    }
}

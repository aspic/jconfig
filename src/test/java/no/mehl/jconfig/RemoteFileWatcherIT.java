package no.mehl.jconfig;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class RemoteFileWatcherIT {

    private static final Logger logger = LoggerFactory.getLogger(RemoteFileWatcherIT.class);

    @Test
    public void run_withServerResponse_shouldRetrieveConfig() throws Exception {
        int port = randomPort();
        Server server = createJettyServer(port, SingleResponseServlet.class);

        ConfigManager manager = new ConfigManager.ConfiguratorBuilder().withRemoteFileWatcher("http://localhost:" + port, 1, TimeUnit.SECONDS).build();
        final AtomicInteger updates = new AtomicInteger();

        manager.addConfigChangedListener(configManager -> {
            assertEquals("baz", configManager.getString("foo", "bar"));
            updates.incrementAndGet();
        });

        waitForManager(manager);

        assertEquals(1, updates.get());

        stopServer(server);
    }

    @Test
    public void multipleRun_withSameServerResponse_shouldOnlyChangeConfigOnce() throws Exception {
        int port = randomPort();
        Server server = createJettyServer(port, SingleResponseServlet.class);

        ConfigManager manager = new ConfigManager.ConfiguratorBuilder().withRemoteFileWatcher("http://localhost:" + port, 2, TimeUnit.SECONDS).build();
        final AtomicInteger updates = new AtomicInteger();

        manager.addConfigChangedListener(configManager -> {
            assertEquals("baz", configManager.getString("foo", "bar"));
            updates.incrementAndGet();
        });

        waitForManager(manager);
        assertEquals(1, updates.get());

        stopServer(server);
    }

    @Test
    public void multipleRun_withIncrementingServerResponse_shouldChangeConfig() throws Exception {
        int port = randomPort();
        Server server = createJettyServer(port, IncrementingResponseServlet.class);

        ConfigManager manager = new ConfigManager.ConfiguratorBuilder().withRemoteFileWatcher("http://localhost:" + port, 500, TimeUnit.MILLISECONDS).build();
        final AtomicInteger updates = new AtomicInteger();

        manager.addConfigChangedListener(configManager -> {
            int runs = configManager.getInt("foo", "incs");
            updates.incrementAndGet();
            if (runs == 1) {
                manager.getPool().shutdown();
            }
        });

        waitForManager(manager);
        assertEquals(2, updates.get());
        stopServer(server);
    }

    private void stopServer(Server server) {
        new Thread() {
            @Override
            public void run() {
                try {
                    server.stop();
                } catch (Exception e) {
                    logger.info("Failed to stop Jetty", e);
                }
            }
        }.start();
    }

    private void waitForManager(ConfigManager manager) {
        try {
            manager.getPool().awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
    }

    private int randomPort() {
        return (int) (1024 + (Math.random() * 10000));
    }

    private Server createJettyServer(int port, Class<? extends HttpServlet> servlet) throws Exception {
        Server server = new Server(port);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(servlet, "/*");
        server.start();

        return server;
    }

    @SuppressWarnings("serial")
    public static class SingleResponseServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("{\"foo\": {\"bar\": \"baz\"}}");
        }
    }

    public static class IncrementingResponseServlet extends HttpServlet {

        int gets = 0;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
            logger.info("enters servlet");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("{\"foo\": {\"incs\": " + gets + "}}");
            gets += 1;
        }
    }

}

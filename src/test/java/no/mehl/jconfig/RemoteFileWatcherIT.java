package no.mehl.jconfig;

import no.mehl.jconfig.listener.ConfigChangeListener;
import no.mehl.jconfig.pojo.Config;
import no.mehl.jconfig.watcher.RemoteFileWatcher;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class RemoteFileWatcherIT {

    @Test
    public void run_withServerResponse_shouldRetrieveConfig() throws Exception {
        Server server = createJettyServer();

        final AtomicInteger updates = new AtomicInteger();
        ConfigChangeListener listener = new ConfigChangeListener() {
            @Override
            public void configChanged(Config newConfig) {
                assertEquals("baz", newConfig.get("foo").get("bar"));
                updates.incrementAndGet();
            }
        };
        RemoteFileWatcher watcher = new RemoteFileWatcher("http://localhost:12345/", listener);
        watcher.run();

        assertEquals(1, updates.get());

        server.stop();
    }

    @Test
    public void multipleRun_withSameServerResponse_shouldOnlyChangeConfigOnce() throws Exception {
        Server server = createJettyServer();

        final AtomicInteger updates = new AtomicInteger();
        ConfigChangeListener listener = newConfig -> {
            assertEquals("baz", newConfig.get("foo").get("bar"));
            updates.incrementAndGet();
        };
        RemoteFileWatcher watcher = new RemoteFileWatcher("http://localhost:12345/", listener);
        watcher.run();
        watcher.run();

        assertEquals(1, updates.get());

        server.stop();
    }



    private Server createJettyServer() throws Exception {
        Server server = new Server(12345);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(HelloServlet.class, "/*");
        server.start();

        return server;
    }

    @SuppressWarnings("serial")
    public static class HelloServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("{\"foo\": {\"bar\": \"baz\"}}");
        }
    }

}

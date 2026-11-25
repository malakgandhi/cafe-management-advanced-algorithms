package application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpServer;

import controller.InventoryController;
import repository.UsageRepository;
import service.InventoryService;

public class CafeApplication {

	public static void main(String[] args) throws IOException {
		Path csvPath = Path.of("data", "sample-usage.csv");

        UsageRepository repository = new UsageRepository(csvPath);
        InventoryService service = new InventoryService(repository);
        InventoryController controller = new InventoryController(service);

		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/", controller);
        server.createContext("/", new FrontendHandler(Path.of("frontend")));
        server.start();

		System.out.println("Cafe inventory API running at http://localhost:8080");
    }
}

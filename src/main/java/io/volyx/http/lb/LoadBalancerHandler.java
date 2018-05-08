package io.volyx.http.lb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

class LoadBalancerHandler implements HttpRequestInterceptor {
	private static final Random random = new Random();
	private final List<InetSocketAddress> servers = new ArrayList<InetSocketAddress>();

	/*
	 * this class does not implement server monitoring or healthiness checks
	 */

	public LoadBalancerHandler(InetSocketAddress... srvs) {
		servers.addAll(Arrays.asList(srvs));
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

		if (request instanceof HttpRequestWrapper) {
			HttpRequestWrapper wrapper = (HttpRequestWrapper) request;
			// determine the business server based on the id's hashcode
//			Integer customerId = request.getRequiredIntParameter("id");
			Integer customerId = wrapper.getParams().getIntParameter("id", 0);
			int idx = customerId.hashCode() % servers.size();
			if (idx < 0) {
				idx *= -1;
			}

			idx = random.nextInt(servers.size());

			// retrieve the business server address and update the Request-URL of the request
			InetSocketAddress server = servers.get(idx);
			URL url = new URL(wrapper.getTarget().toURI());
			final HttpHost target = wrapper.getTarget();
			URL newUrl = new URL(url.getProtocol(), server.getHostName(), server.getPort(), url.getFile());

			try {
				wrapper.setURI(newUrl.toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		System.out.println(request);





	}

	public static void main(String[] args) throws Exception {
		InetSocketAddress[] srvs = new InetSocketAddress[] { new InetSocketAddress("https://ya.ru", 80), new InetSocketAddress("https://google.com", 80)};

		final CloseableHttpClient httpClient = HttpClientBuilder
				.create()
				.addInterceptorFirst(new LoadBalancerHandler(srvs)).build();
		try (httpClient) {
			while (true) {

				Thread.sleep(1000L);

				final String host = "http://123.com";
				final HttpRequest get = DefaultHttpRequestFactory.INSTANCE.newHttpRequest("GET", host);
				final CloseableHttpResponse response = httpClient.execute(HttpHost.create(host), get);
				System.out.println(response.getStatusLine().getStatusCode());
			}
		}

	}

}


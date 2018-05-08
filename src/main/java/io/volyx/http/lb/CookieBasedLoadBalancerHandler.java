//package io.volyx.http.lb;
//
//
//class CookieBasedLoadBalancerHandler implements IHttpRequestHandler, ILifeCycle {
//	private final List<InetSocketAddress> servers = new ArrayList<InetSocketAddress>();
//	private int serverIdx = 0;
//	private HttpClient httpClient;
//
//	/*
//	 * this class does not implement server monitoring or healthiness checks
//	 */
//
//	public CookieBasedLoadBalancerHandler(InetSocketAddress... realServers) {
//		servers.addAll(Arrays.asList(realServers));
//	}
//
//	public void onInit() {
//		httpClient = new HttpClient();
//		httpClient.setAutoHandleCookies(false);
//	}
//
//	public void onDestroy() throws IOException {
//		httpClient.close();
//	}
//
//	public void onRequest(final IHttpExchange exchange) throws IOException {
//		IHttpRequest request = exchange.getRequest();
//
//
//		IHttpResponseHandler respHdl = null;
//		InetSocketAddress serverAddr = null;
//
//		// check if the request contains the LB_SLOT cookie
//		cl : for (String cookieHeader : request.getHeaderList("Cookie")) {
//			for (String cookie : cookieHeader.split(";")) {
//				String[] kvp = cookie.split("=");
//				if (kvp[0].startsWith("LB_SLOT")) {
//					int slot = Integer.parseInt(kvp[1]);
//					serverAddr = servers.get(slot);
//					break cl;
//				}
//			}
//		}
//
//		// request does not contains the LB_SLOT -> select a server
//		if (serverAddr == null) {
//			final int slot = nextServerSlot();
//			serverAddr = servers.get(slot);
//
//			respHdl = new IHttpResponseHandler() {
//
//				@Execution(Execution.NONTHREADED)
//				public void onResponse(IHttpResponse response) throws IOException {
//					// set the LB_SLOT cookie
//					response.setHeader("Set-Cookie", "LB_SLOT=" + slot + ";Path=/");
//					exchange.send(response);
//				}
//
//				@Execution(Execution.NONTHREADED)
//				public void onException(IOException ioe) throws IOException {
//					exchange.sendError(ioe);
//				}
//			};
//
//		} else {
//			respHdl = new IHttpResponseHandler() {
//
//				@Execution(Execution.NONTHREADED)
//				public void onResponse(IHttpResponse response) throws IOException {
//					exchange.send(response);
//				}
//
//				@Execution(Execution.NONTHREADED)
//				public void onException(IOException ioe) throws IOException {
//					exchange.sendError(ioe);
//				}
//			};
//		}
//
//		// update the Request-URL of the request
//		URL url = request.getRequestUrl();
//		URL newUrl = new URL(url.getProtocol(), serverAddr.getHostName(), serverAddr.getPort(), url.getFile());
//		request.setRequestUrl(newUrl);
//
//		// proxy header handling (remove hop-by-hop headers, ...)
//		// ...
//
//		// forward the request
//		httpClient.send(request, respHdl);
//	}
//
//	// get the next slot by using the using round-robin approach
//	private synchronized int nextServerSlot() {
//		serverIdx++;
//		if (serverIdx >= servers.size()) {
//			serverIdx = 0;
//		}
//		return serverIdx;
//	}
//}
//
//
//class LoadBalancer {
//
//	public static void main(String[] args) throws Exception {
//		InetSocketAddress[] srvs = new InetSocketAddress[] { new InetSocketAddress("srv1", 8030), new InetSocketAddress("srv2", 8030)};
//		CookieBasedLoadBalancerHandler hdl = new CookieBasedLoadBalancerHandler(srvs);
//		HttpServer loadBalancer = new HttpServer(8080, hdl);
//		loadBalancer.run();
//	}
//}
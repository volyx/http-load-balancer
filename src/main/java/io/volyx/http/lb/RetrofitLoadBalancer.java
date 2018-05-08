package io.volyx.http.lb;

import okhttp3.*;
import org.apache.http.HttpHost;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import sun.reflect.Reflection;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class RetrofitLoadBalancer implements Interceptor {

	public static void main(String[] args) {
		final OkHttpClient client = new OkHttpClient.Builder()
//				.addInterceptor(new LoggingInterceptor())
				.addInterceptor(new RetrofitLoadBalancer())
				.build();

		Retrofit retrofit = new Retrofit.Builder()
				.client(client)
				.baseUrl("https://api.github.com")
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		GitHubService service = retrofit.create(GitHubService.class);

		try {
			System.out.println(service.listRepos("volyx").execute().message());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();

		try {
			final Class<? extends Request> clazz = request.getClass();
			final Field urlField = clazz.getDeclaredField("url");
			urlField.setAccessible(true);
			HttpUrl original = (HttpUrl) urlField.get(request);

			final URL url = original.uri().toURL();

			URL newUrl = new URL(url.getProtocol(), "127.0.0.1", 81, url.getFile());
			HttpUrl target = HttpUrl.get(newUrl.toURI());
			System.out.println(original);

			urlField.set(request, target);
			System.out.println(target);
		} catch (NoSuchFieldException | IllegalAccessException | URISyntaxException e) {
			e.printStackTrace();
		}

		return chain.proceed(request);
	}

	public interface GitHubService {
		@GET("users/{user}/repos")
		Call<List<Repo>> listRepos(@Path("user") String user);
	}

	public class Repo {
		public String name;
	}

	static class LoggingInterceptor implements Interceptor {
		@Override public Response intercept(Interceptor.Chain chain) throws IOException {
			Request request = chain.request();

			long t1 = System.nanoTime();
			System.out.println(String.format("Sending request %s on %s%n%s",
					request.url(), chain.connection(), request.headers()));

			Response response = chain.proceed(request);

			long t2 = System.nanoTime();
			System.out.println(String.format("Received response for %s in %.1fms%n%s",
					response.request().url(), (t2 - t1) / 1e6d, response.headers()));

			return response;
		}
	}
}

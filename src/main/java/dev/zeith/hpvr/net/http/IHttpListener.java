package dev.zeith.hpvr.net.http;

public interface IHttpListener
{
	void handle(HttpRequest request, HttpResponse response)
			throws Exception;
}
package org.bosik.filebrowser.core.nodes.ftp;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nikita Bosik
 * @since 2017-09-17
 */
public class TestServerURL
{
	private static final int DEFAULT_FTP_PORT = ServerURL.DEFAULT_FTP_PORT;

	private static void assertEquals(String expectedHost, int expectedPort, String expectedPath, ServerURL actual)
	{
		Assert.assertEquals(expectedHost, actual.getHost());
		Assert.assertEquals(expectedPort, actual.getPort());
		Assert.assertEquals(expectedPath, actual.getPath());
	}

	private static void assertEquals(ServerURL expected, ServerURL actual)
	{
		Assert.assertEquals(expected.getHost(), actual.getHost());
		Assert.assertEquals(expected.getPort(), actual.getPort());
		Assert.assertEquals(expected.getPath(), actual.getPath());
	}

	@Test
	public void test_constructor()
	{
		assertEquals("ftpsrv.com", 9000, "foo/bar", new ServerURL("ftpsrv.com", 9000, "foo/bar"));
	}

	@Test
	public void test_parse_slash()
	{
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com/"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo", ServerURL.parse("ftp://ftpsrv.com/foo"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo", ServerURL.parse("ftp://ftpsrv.com/foo/"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo/bar", ServerURL.parse("ftp://ftpsrv.com/foo/bar"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo/bar", ServerURL.parse("ftp://ftpsrv.com/foo/bar/"));

		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000"));
		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000/"));
		assertEquals("ftpsrv.com", 9000, "foo", ServerURL.parse("ftp://ftpsrv.com:9000/foo"));
		assertEquals("ftpsrv.com", 9000, "foo", ServerURL.parse("ftp://ftpsrv.com:9000/foo/"));
		assertEquals("ftpsrv.com", 9000, "foo/bar", ServerURL.parse("ftp://ftpsrv.com:9000/foo/bar"));
		assertEquals("ftpsrv.com", 9000, "foo/bar", ServerURL.parse("ftp://ftpsrv.com:9000/foo/bar/"));
	}

	@Test
	public void test_parse_backslash()
	{
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com\\"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo", ServerURL.parse("ftp://ftpsrv.com\\foo"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo", ServerURL.parse("ftp://ftpsrv.com\\foo\\"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo/bar", ServerURL.parse("ftp://ftpsrv.com\\foo\\bar"));
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo/bar", ServerURL.parse("ftp://ftpsrv.com\\foo\\bar\\"));

		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000"));
		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000\\"));
		assertEquals("ftpsrv.com", 9000, "foo", ServerURL.parse("ftp://ftpsrv.com:9000\\foo"));
		assertEquals("ftpsrv.com", 9000, "foo", ServerURL.parse("ftp://ftpsrv.com:9000\\foo\\"));
		assertEquals("ftpsrv.com", 9000, "foo/bar", ServerURL.parse("ftp://ftpsrv.com:9000\\foo\\bar"));
		assertEquals("ftpsrv.com", 9000, "foo/bar", ServerURL.parse("ftp://ftpsrv.com:9000\\foo\\bar\\"));
	}

	@Test
	public void test_getParent()
	{
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com/").getParent());
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com/foo/").getParent());
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo", ServerURL.parse("ftp://ftpsrv.com/foo/bar/").getParent());
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "foo/bar", ServerURL.parse("ftp://ftpsrv.com/foo/bar/next/").getParent());

		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000/").getParent());
		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000/foo/").getParent());
		assertEquals("ftpsrv.com", 9000, "foo", ServerURL.parse("ftp://ftpsrv.com:9000/foo/bar/").getParent());
		assertEquals("ftpsrv.com", 9000, "foo/bar", ServerURL.parse("ftp://ftpsrv.com:9000/foo/bar/next/").getParent());
	}

	@Test
	public void test_getRoot()
	{
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com/").getRoot());
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com/foo/").getRoot());
		assertEquals("ftpsrv.com", DEFAULT_FTP_PORT, "", ServerURL.parse("ftp://ftpsrv.com/foo/bar/").getRoot());

		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000/").getRoot());
		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000/foo/").getRoot());
		assertEquals("ftpsrv.com", 9000, "", ServerURL.parse("ftp://ftpsrv.com:9000/foo/bar/").getRoot());
	}

	@Test
	public void test_toString()
	{
		Assert.assertEquals("ftp://ftpsrv.com/", ServerURL.parse("ftp://ftpsrv.com/").toString());
		Assert.assertEquals("ftp://ftpsrv.com/foo", ServerURL.parse("ftp://ftpsrv.com/foo/").toString());
		Assert.assertEquals("ftp://ftpsrv.com/foo/bar", ServerURL.parse("ftp://ftpsrv.com/foo/bar/").toString());

		Assert.assertEquals("ftp://ftpsrv.com:9000/", ServerURL.parse("ftp://ftpsrv.com:9000/").toString());
		Assert.assertEquals("ftp://ftpsrv.com:9000/foo", ServerURL.parse("ftp://ftpsrv.com:9000/foo/").toString());
		Assert.assertEquals("ftp://ftpsrv.com:9000/foo/bar", ServerURL.parse("ftp://ftpsrv.com:9000/foo/bar/").toString());
	}
}
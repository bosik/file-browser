package org.bosik.filebrowser.core;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nikita Bosik
 * @since 2017-10-04
 */
public class TestUtil
{
    @Test
    public void test_stripeSlashes()
    {
        Assert.assertEquals(null, Util.stripeSlashes(null));
        Assert.assertEquals("", Util.stripeSlashes(""));
        Assert.assertEquals("foo", Util.stripeSlashes("foo"));

        Assert.assertEquals("foo", Util.stripeSlashes("/foo"));
        Assert.assertEquals("foo", Util.stripeSlashes("\\foo"));

        Assert.assertEquals("/foo", Util.stripeSlashes("//foo"));
        Assert.assertEquals("\\foo", Util.stripeSlashes("\\\\foo"));
        Assert.assertEquals("\\foo", Util.stripeSlashes("/\\foo"));
        Assert.assertEquals("/foo", Util.stripeSlashes("\\/foo"));

        Assert.assertEquals("foo/", Util.stripeSlashes("foo//"));
        Assert.assertEquals("foo\\", Util.stripeSlashes("foo\\\\"));
        Assert.assertEquals("foo/", Util.stripeSlashes("foo/\\"));
        Assert.assertEquals("foo\\", Util.stripeSlashes("foo\\/"));

        Assert.assertEquals("foo", Util.stripeSlashes("/foo/"));
        Assert.assertEquals("/foo/", Util.stripeSlashes("//foo//"));
        Assert.assertEquals("foo", Util.stripeSlashes("\\foo\\"));
        Assert.assertEquals("\\foo\\", Util.stripeSlashes("\\\\foo\\\\"));

        Assert.assertEquals("", Util.stripeSlashes("//"));
    }
}

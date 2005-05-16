/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.test.mule.interceptors;

import java.util.ArrayList;
import java.util.List;

import org.mule.interceptors.InterceptorStack;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.Invocation;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOMessage;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.mockobjects.dynamic.Mock;

/**
 * TODO: document this class 
 *
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class InterceptorStackTestCase extends AbstractMuleTestCase {

	public static class DummyInvocation extends Invocation {
		public DummyInvocation(UMOMessage msg) {
			super(null, msg, null);
		}
	    public UMOMessage execute() throws UMOException {
			return getMessage();
	    }
	}
	
	public void testStack() throws Exception {
		final SynchronizedInt c = new SynchronizedInt(0);
		final UMOMessage m1 = (UMOMessage) new Mock(UMOMessage.class).proxy();
		final UMOMessage m2 = (UMOMessage) new Mock(UMOMessage.class).proxy();
		final UMOMessage m3 = (UMOMessage) new Mock(UMOMessage.class).proxy();
		final UMOMessage m4 = (UMOMessage) new Mock(UMOMessage.class).proxy();
		final UMOMessage m5 = (UMOMessage) new Mock(UMOMessage.class).proxy();
		
		InterceptorStack s = new InterceptorStack();
		List interceptors = new ArrayList();
		interceptors.add(new UMOInterceptor() {
			public UMOMessage intercept(Invocation invocation) throws UMOException {
				assertEquals(0, c.get());
				c.increment();
				assertTrue(m1 == invocation.getMessage());
				invocation.setMessage(m2);
				UMOMessage msg = invocation.execute();
				assertEquals(3, c.get());
				c.increment();
				assertTrue(m4 == msg);
				return m5;
			} 
		});
		interceptors.add(new UMOInterceptor() {
			public UMOMessage intercept(Invocation invocation) throws UMOException {
				assertEquals(1, c.get());
				c.increment();
				assertTrue(m2 == invocation.getMessage());
				invocation.setMessage(m3);
				UMOMessage msg = invocation.execute();
				assertEquals(2, c.get());
				c.increment();
				assertTrue(m3 == msg);
				return m4;
			} 
		});
		s.setInterceptors(interceptors);
		
		UMOMessage r = s.intercept(new DummyInvocation(m1));
		assertTrue(r == m5);
		assertEquals(4, c.get());
	}
	
}

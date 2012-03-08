/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.joran.action;

import ch.qos.logback.core.status.OnConsoleStatusListener;
import org.xml.sax.Attributes;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.ReconfigureOnChangeFilter;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.ContextUtil;
import ch.qos.logback.core.util.Duration;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;

public class ConfigurationAction extends Action {
  static final String INTERNAL_DEBUG_ATTR = "debug";
  static final String SCAN_ATTR = "scan";
  static final String SCAN_PERIOD_ATTR = "scanPeriod";

  long threshold = 0;

  public void begin(InterpretationContext ec, String name, Attributes attributes) {
    String debugAttrib = ec.subst(attributes.getValue(INTERNAL_DEBUG_ATTR));
    threshold = System.currentTimeMillis();
    if (OptionHelper.isEmpty(debugAttrib)
        || debugAttrib.equalsIgnoreCase("false")
        || debugAttrib.equalsIgnoreCase("null")) {
      addInfo(INTERNAL_DEBUG_ATTR + " attribute not set");
    } else {
      OnConsoleStatusListener.addNewInstanceToContext(context);
    }

    processScanAttrib(attributes);

  	/* http://developer.android.com/reference/java/net/InetAddress.html#getLocalHost()
  	 * 
  	 * addHostNameasProperty() leads to InetAddress.getLocalHost(), which performs
  	 * a DNS query and thus causes an android.os.NetworkOnMainThreadException if 
  	 * this logger configuration originated from the main thread (Issue #3). It
  	 * turns out the hostname is atypical in Android devices, and the DNS query
  	 * is normally fruitless. The end result is a hostname of "localhost" (the
  	 * default) at the cost of a DNS query. 
  	 * 
  	 * The reason the hostname is added to the context's properties is for the 
  	 * variable-lookup of ${HOSTNAME} in configuration XML, but the context is only
  	 * one of the sources. If the context doesn't have the variable, the OS environment
  	 * is searched next. We can safely omit this next call.
  	 */
    //new ContextUtil(context).addHostNameAsProperty();

    // the context is turbo filter attachable, so it is pushed on top of the
    // stack
    ec.pushObject(getContext());
  }

  void processScanAttrib(Attributes attributes) {
    String scanAttrib = attributes.getValue(SCAN_ATTR);
    if (!OptionHelper.isEmpty(scanAttrib)
        && !"false".equalsIgnoreCase(scanAttrib)) {
      ReconfigureOnChangeFilter rocf = new ReconfigureOnChangeFilter();
      rocf.setContext(context);
      String scanPeriodAttrib = attributes.getValue(SCAN_PERIOD_ATTR);
      if (!OptionHelper.isEmpty(scanPeriodAttrib)) {
        try {
          Duration duration = Duration.valueOf(scanPeriodAttrib);
          rocf.setRefreshPeriod(duration.getMilliseconds());
          addInfo("Setting ReconfigureOnChangeFilter scanning period to "
              + duration);
        } catch (NumberFormatException nfe) {
          addError("Error while converting [" + scanAttrib + "] to long", nfe);
        }
      }
      rocf.start();
      LoggerContext lc = (LoggerContext) context;
      addInfo("Adding ReconfigureOnChangeFilter as a turbo filter");
      lc.addTurboFilter(rocf);
    }
  }

  public void end(InterpretationContext ec, String name) {
    addInfo("End of configuration.");
    ec.popObject();
  }
}

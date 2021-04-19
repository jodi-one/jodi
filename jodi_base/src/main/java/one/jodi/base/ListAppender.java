package one.jodi.base;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.SerializedLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This appender is primarily used for testing. Use in a real environment is discouraged as the
 * List could eventually grow to cause an OutOfMemoryError.
 * <p>
 * This appender will use {@link Layout#toByteArray(LogEvent)}.
 * <p>
 * see org.apache.logging.log4j.junit.LoggerContextRule#getListAppender(String) ILC.getListAppender
 */
@Plugin(name = "List", category = "Core", elementType = "appender", printObject = true)
public class ListAppender extends AbstractAppender {

   // Use CopyOnWriteArrayList?

   private static final String WINDOWS_LINE_SEP = "\r\n";
   final List<LogEvent> events = new ArrayList<>();
   final List<byte[]> data = new ArrayList<>();
   private final List<String> messages = new ArrayList<>();
   private final boolean newLine;
   private final boolean raw;


   public ListAppender(final String name) {
      super(name, null, null);
      newLine = false;
      raw = false;
      this.setState(LifeCycle.State.STARTED);
   }

   public ListAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
                       final boolean newline, final boolean raw) {
      super(name, filter, layout);
      this.newLine = newline;
      this.raw = raw;
      if (layout != null && !(layout instanceof SerializedLayout)) {
         final byte[] bytes = layout.getHeader();
         if (bytes != null) {
            write(bytes);
         }
      }
   }

   @SuppressWarnings("unused")
   public static ListAppender createAppender(final String name, final boolean newLine, final boolean raw,
                                             final Layout<? extends Serializable> layout, final Filter filter) {
      return new ListAppender(name, filter, layout, newLine, raw);
   }

   @PluginBuilderFactory
   @SuppressWarnings("unused")
   public static Builder newBuilder() {
      return new Builder();
   }

   /**
    * Gets the named ListAppender if it has been registered.
    * see org.apache.logging.log4j.junit.LoggerContextRule#getListAppender(String)
    *
    * @param name the name of the ListAppender
    * @return the named ListAppender or {@code null} if it does not exist
    */
   @SuppressWarnings("unused")
   public static ListAppender getListAppender(final String name) {
      return (LoggerContext.getContext(false)).getConfiguration()
                                              .getAppender(name);
   }

   @Override
   public synchronized void append(final LogEvent event) {
      final Layout<? extends Serializable> layout = getLayout();
      events.add(event);
      if (layout == null) {
         if (event instanceof MutableLogEvent) {
            // must take snapshot or subsequent calls to logger.log() will modify this event
            events.add(((MutableLogEvent) event).createMemento());
         } else {
            events.add(event);
         }
      } else if (layout instanceof SerializedLayout) {
         final byte[] header = layout.getHeader();
         final byte[] content = layout.toByteArray(event);
         final byte[] record = new byte[header.length + content.length];
         System.arraycopy(header, 0, record, 0, header.length);
         System.arraycopy(content, 0, record, header.length, content.length);
         data.add(record);
      } else {
         write(layout.toByteArray(event));
      }

   }

   void write(final byte[] bytes) {
      if (raw) {
         data.add(bytes);
         return;
      }
      final String str = new String(bytes);
      if (newLine) {
         int index = 0;
         while (index < str.length()) {
            int end;
            final int wend = str.indexOf(WINDOWS_LINE_SEP, index);
            final int lend = str.indexOf('\n', index);
            int length;
            if (wend >= 0 && wend < lend) {
               end = wend;
               length = 2;
            } else {
               end = lend;
               length = 1;
            }
            if (index == end) {
               if (!messages.get(messages.size() - length)
                            .isEmpty()) {
                  messages.add("");
               }
            } else if (end >= 0) {
               messages.add(str.substring(index, end));
            } else {
               messages.add(str.substring(index));
               break;
            }
            index = end + length;
         }
      } else {
         messages.add(str);
      }
   }

   @Override
   public void stop() {
      super.stop();
      final Layout<? extends Serializable> layout = getLayout();
      if (layout != null) {
         final byte[] bytes = layout.getFooter();
         if (bytes != null) {
            write(bytes);
         }
      }
   }

   public synchronized ListAppender clear() {
      events.clear();
      messages.clear();
      data.clear();
      return this;
   }

   public synchronized List<LogEvent> getEvents() {
      return Collections.unmodifiableList(events);
   }

   public synchronized List<String> getMessages() {
      return Collections.unmodifiableList(messages);
   }

   public synchronized List<byte[]> getData() {
      return Collections.unmodifiableList(data);
   }

   public boolean contains(Level level, String message) {
      if (events.size() == 0) {
         throw new RuntimeException("Listappender contains implies some events, no events recorded.");
      }
      for (LogEvent logEvent : events) {
         if (level.equals(logEvent.getLevel()) && logEvent.getMessage()
                                                          .getFormattedMessage()
                                                          .contains(message)) {
            return true;
         }
      }
      return false;
   }

   public boolean contains(Level level, boolean specific) {
      if (events.size() == 0) {
         throw new RuntimeException("Listappender contains implies some events, no events recorded.");
      }
      for (LogEvent logEvent : events) {
         if (specific && level.equals(logEvent.getLevel())) {
            return true;
         } else if (!specific && logEvent.getLevel()
                                         .isMoreSpecificThan(level)) {
            return true;
         }
      }
      return false;

   }

   public static class Builder implements org.apache.logging.log4j.core.util.Builder<ListAppender> {

      @PluginBuilderAttribute
      @Required
      private String name;

      @PluginBuilderAttribute
      private boolean entryPerNewLine;

      @PluginBuilderAttribute
      private boolean raw;

      @PluginElement("Layout")
      private Layout<? extends Serializable> layout;

      @PluginElement("Filter")
      private Filter filter;

      public Builder setName(final String name) {
         this.name = name;
         return this;
      }

      @SuppressWarnings("unused")
      public Builder setEntryPerNewLine(final boolean entryPerNewLine) {
         this.entryPerNewLine = entryPerNewLine;
         return this;
      }

      public Builder setRaw(final boolean raw) {
         this.raw = raw;
         return this;
      }

      public Builder setLayout(final Layout<? extends Serializable> layout) {
         this.layout = layout;
         return this;
      }

      public Builder setFilter(final Filter filter) {
         this.filter = filter;
         return this;
      }

      @Override
      public ListAppender build() {
         return new ListAppender(name, filter, layout, entryPerNewLine, raw);
      }
   }
}

package one.jodi.odi.common;

import one.jodi.base.exception.UnRecoverableException;
import one.jodi.etl.common.EtlSubSystemVersion;
import oracle.odi.core.OdiInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class OdiVersion implements EtlSubSystemVersion {
   public static final Logger logger = LogManager.getLogger(OdiVersion.class);

   private static final String ODI_INTERFACE_CLASS = "/oracle/odi/domain/project/OdiInterface.class";

   private static String readVersionInfoInManifest() {
      String versionNumber = "-1";
      URL jarURL = OdiVersion.class.getResource(ODI_INTERFACE_CLASS);
      if (jarURL == null) {
         String msg = "Unable to find ODI class " + ODI_INTERFACE_CLASS +
                 " on the classpath. Please review classpath settings.";
         logger.fatal(msg);
         throw new UnRecoverableException(msg);
      }

      JarURLConnection jurlConn = null;
      try {
         jurlConn = (JarURLConnection) jarURL.openConnection();
         Manifest mf = jurlConn.getManifest();
         if (mf != null) {
            Attributes attr = mf.getMainAttributes();
            versionNumber = (attr.getValue("Version"));
         }
      } catch (IOException | IllegalArgumentException e) {
         logger.error("Error loading ODI libraries auto-detection of version.", e);
      }
      return versionNumber;
   }

   @Override
   public boolean isVersion11() {
      try {
         String VERSION = readVersionInfoInManifest();
         return VERSION.startsWith("11");
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public boolean isVersion122() {
      try {
         String VERSION = readVersionInfoInManifest();
         return VERSION.startsWith("12.2");
      } catch (Exception e) {
         return true;
      }
   }

   @Override
   public boolean isVersion1213() {
      try {
         String VERSION = readVersionInfoInManifest();
         return VERSION.startsWith("12.1.3");
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public boolean isVersion1212() {
      try {
         String VERSION = readVersionInfoInManifest();
         return VERSION.startsWith("12.1.2");
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public String getVersion() {
      return readVersionInfoInManifest();
   }

   private int getGlobalVersion(final OdiInstance odiInstance) {
      int version = -1;
      if (odiInstance.getWorkRepository()
                     .getLegacyConnectionDef()
                     .getProductVersion()
                     .isBefore11G()) {
         version = 10;
      } else if (odiInstance.getWorkRepository()
                            .getLegacyConnectionDef()
                            .getProductVersion()
                            .toString()
                            .startsWith("11.")) {
         version = 11;
      } else if (odiInstance.getWorkRepository()
                            .getLegacyConnectionDef()
                            .getProductVersion()
                            .toString()
                            .startsWith("12.")) {
         version = 12;
      } else {
         version = -1;
      }
      // logger.debug("Version:"+odiInstance.getWorkRepository().getLegacyConnectionDef().getProductVersion().toString());
      return version;
   }
}